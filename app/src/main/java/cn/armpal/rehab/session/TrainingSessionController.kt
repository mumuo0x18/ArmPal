package cn.armpal.rehab.session

import android.os.SystemClock
import android.util.Log
import cn.armpal.rehab.analysis.AngleMetrics
import cn.armpal.rehab.analysis.BodyFrameEstimator
import cn.armpal.rehab.analysis.ExerciseTemplate
import cn.armpal.rehab.analysis.RepCounter
import cn.armpal.rehab.analysis.RepEvent
import cn.armpal.rehab.tracking.TrackingSource
import cn.armpal.rehab.tracking.TrackingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 训练会话状态中枢:追踪帧 → BodyFrame → 抬举角 → 计数状态机 → [SessionState]。
 *
 * 三窗只订阅 [state],窗口之间不直接通信;算法全部在 analysis 包,可 JVM 单测。
 * W2 阶段先用进程内单例(与 PICO Spatial 的 Application 级 Compose 启动方式配合),
 * 后续接入 Room/DataStore 时把存储副作用挂到本类即可。
 */
object TrainingSessionController {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val current: SessionState get() = _state.value

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    @Volatile
    private var source: TrackingSource? = null
    private var estimator = BodyFrameEstimator()
    private var counter: RepCounter? = null
    private var exercise: ExerciseTemplate? = null
    private var lastHandMs = 0L

    /** 绑定数据源与动作模板(幂等:同一 source + 同一动作不重复启动)。 */
    fun bind(newSource: TrackingSource, newExercise: ExerciseTemplate) {
        if (source === newSource && exercise?.id == newExercise.id) return
        loop?.cancel()
        source = newSource
        exercise = newExercise
        estimator = BodyFrameEstimator()
        counter = RepCounter(newExercise.targetElevationDeg, newExercise.holdMs)
        lastHandMs = 0L
        _state.value = SessionState(
            exercise = newExercise,
            targetDeg = newExercise.targetElevationDeg,
            targetReps = newExercise.reps,
            totalSets = newExercise.sets,
            demoMode = newSource.isDemo,
            tracking = TrackingStatus.WAITING,
            sessionStartedAtMs = SystemClock.elapsedRealtime(),
            lastMessage = "跟着示范慢慢抬臂",
        )
        loop = scope.launch {
            while (isActive) {
                runCatching { tick() }.onFailure { Log.e(TAG, "tick failed", it) }
                delay(FRAME_INTERVAL_MS)
            }
        }
        Log.i(TAG, "bind source=${newSource::class.simpleName} exercise=${newExercise.id}")
    }

    /** 解绑(Stage 退出时调用,避免后台空转)。 */
    fun unbind() {
        loop?.cancel()
        loop = null
        source = null
        counter = null
    }

    /** 重新开始(清空计数,保留本动作)。 */
    fun reset() {
        counter?.reset()
        lastHandMs = 0L
        _state.update {
            it.copy(
                repsDone = 0,
                peakDeg = 0f,
                phase = cn.armpal.rehab.analysis.RepPhase.IDLE,
                feedback = FeedbackLevel.WAITING,
                lastMessage = "重新开始,慢慢抬臂",
                sessionStartedAtMs = SystemClock.elapsedRealtime(),
            )
        }
    }

    private fun tick() {
        val src = source ?: return
        val ex = exercise ?: return
        val counter = counter ?: return

        val now = SystemClock.elapsedRealtime()
        val frame = src.currentFrame(now)
        if (frame.hasHands) lastHandMs = now

        val effectiveStatus = when {
            frame.status != TrackingStatus.ACTIVE && frame.status != TrackingStatus.DEMO -> frame.status
            frame.hasHands -> frame.status
            lastHandMs == 0L -> TrackingStatus.WAITING
            now - lastHandMs > LOST_GRACE_MS -> TrackingStatus.LOST
            else -> frame.status
        }

        var elevationR: Float? = null
        var elevationL: Float? = null
        val head = frame.headWorld
        if (head != null && frame.hasHands) {
            val body = estimator.estimate(head, frame.headYawRad)
            elevationR = frame.wristRWorld?.let { AngleMetrics.elevationDeg(it, body.shoulderR) }
            elevationL = frame.wristLWorld?.let { AngleMetrics.elevationDeg(it, body.shoulderL) }
        }

        val targetSide = if (ex.targetJoint.endsWith("_L", ignoreCase = true)) elevationL else elevationR

        // 反馈等级(不写入状态机,只用于颜色/文案)
        val feedback = when {
            effectiveStatus == TrackingStatus.PERMISSION_REQUIRED && targetSide == null ->
                FeedbackLevel.WAITING

            targetSide == null -> FeedbackLevel.WAITING
            effectiveStatus == TrackingStatus.LOST -> FeedbackLevel.WAITING
            counter.phase == cn.armpal.rehab.analysis.RepPhase.HOLD -> FeedbackLevel.HOLDING
            targetSide >= ex.targetElevationDeg -> FeedbackLevel.GOOD
            targetSide >= ex.targetElevationDeg * PARTIAL_RATIO -> FeedbackLevel.PARTIAL
            else -> FeedbackLevel.TOO_LOW
        }

        var message = ""
        if (targetSide != null && effectiveStatus != TrackingStatus.LOST) {
            val event = counter.onFrame(targetSide, now)
            if (event is RepEvent.RepCompleted) {
                message = "第 ${event.count} 次完成,最高 ${event.peakDeg.toInt()}°"
            }
        }

        val repsDone = counter.count
        _state.update { prev ->
            prev.copy(
                tracking = effectiveStatus,
                trackingLost = effectiveStatus == TrackingStatus.LOST,
                demoMode = src.isDemo,
                elevationR = elevationR,
                elevationL = elevationL,
                targetDeg = ex.targetElevationDeg,
                repsDone = repsDone,
                targetReps = ex.reps,
                setIndex = if (ex.reps > 0) repsDone / ex.reps else 0,
                totalSets = ex.sets,
                peakDeg = counter.peakDeg,
                feedback = feedback,
                phase = counter.phase,
                lastMessage = if (message.isNotEmpty()) message else prev.lastMessage,
            )
        }
    }

    private const val TAG = "TrainingSession"
    private const val FRAME_INTERVAL_MS = 33L
    private const val LOST_GRACE_MS = 500L

    /** 接近目标的下限(0.6×目标角)用黄色提示"再抬一点"。 */
    private const val PARTIAL_RATIO = 0.6f
}
