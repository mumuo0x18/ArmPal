package cn.armpal.rehab.session

import android.os.SystemClock
import android.util.Log
import cn.armpal.rehab.analysis.AngleMetrics
import cn.armpal.rehab.analysis.BodyFrameEstimator
import cn.armpal.rehab.analysis.ExerciseTemplate
import cn.armpal.rehab.analysis.HeadMetrics
import cn.armpal.rehab.analysis.RepCounter
import cn.armpal.rehab.analysis.RepEvent
import cn.armpal.rehab.analysis.RepQualityScorer
import cn.armpal.rehab.analysis.Vec3
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
 * W2.5:组间休息、DTW 动作质量采样、会话落库钩子([sessionSink],由 data 层注入实现)。
 */
object TrainingSessionController {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val current: SessionState get() = _state.value

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    /** 会话结束落库钩子(HomeStage 注入 data 层实现;null = 不持久化)。 */
    @Volatile
    var sessionSink: ((SessionRecord) -> Unit)? = null

    @Volatile
    private var source: TrackingSource? = null
    private var estimator = BodyFrameEstimator()
    private var counter: RepCounter? = null
    private var exercise: ExerciseTemplate? = null
    private var lastHandMs = 0L

    // —— 会话/组推进状态 ——
    private var setIndex = 0
    private var restingSecLeft = 0
    private var restingTotalSec = 0
    private var lastRestTickMs = 0L
    private var totalReps = 0
    private var sessionRecorded = false
    private var maxElevation = 0f
    private var qualitySamples = mutableListOf<Float>()
    private var symmetrySamples = mutableListOf<Float>()
    private var repTrajectory = mutableListOf<Vec3>()
    private var templatePoints: List<Vec3> = emptyList()

    /** 绑定数据源与动作模板(幂等:同一 source + 同一动作不重复启动)。 */
    fun bind(newSource: TrackingSource, newExercise: ExerciseTemplate) {
        if (source === newSource && exercise?.id == newExercise.id) return
        loop?.cancel()
        source = newSource
        exercise = newExercise
        estimator = BodyFrameEstimator()
        counter = RepCounter(newExercise.targetElevationDeg, newExercise.holdMs)
        lastHandMs = 0L
        resetSessionCounters()
        templatePoints = newExercise.templateTrajectory.mapNotNull { p ->
            if (p.size >= 3) Vec3(p[0], p[1], p[2]) else null
        }
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

    /** 解绑(Stage 退出时调用,避免后台空转;有未落库的有效数据则先落库)。 */
    fun unbind() {
        flushSessionIfPending(reason = "unbind")
        loop?.cancel()
        loop = null
        source = null
        counter = null
    }

    /** 重新开始(未落库的有效数据先落库,再清零)。 */
    fun reset() {
        flushSessionIfPending(reason = "reset")
        counter?.reset()
        resetSessionCounters()
        _state.update {
            it.copy(
                repsDone = 0,
                peakDeg = 0f,
                phase = cn.armpal.rehab.analysis.RepPhase.IDLE,
                feedback = FeedbackLevel.WAITING,
                lastMessage = "重新开始,慢慢抬臂",
                sessionStartedAtMs = SystemClock.elapsedRealtime(),
                restingSecLeft = 0,
                restingTotalSec = 0,
                totalReps = 0,
                sessionAvgQuality = -1f,
                sessionCompleted = false,
            )
        }
    }

    private fun resetSessionCounters() {
        setIndex = 0
        restingSecLeft = 0
        restingTotalSec = 0
        lastRestTickMs = 0L
        totalReps = 0
        sessionRecorded = false
        maxElevation = 0f
        qualitySamples.clear()
        symmetrySamples.clear()
        repTrajectory.clear()
    }

    private fun tick() {
        val src = source ?: return
        val ex = exercise ?: return
        val counter = counter ?: return

        val now = SystemClock.elapsedRealtime()
        val frame = src.currentFrame(now)
        val isNeck = HeadMetrics.isNeckExercise(ex.targetJoint)
        // 有效信号:臂部动作看双手,颈部动作看头显姿态
        val hasSignal = frame.hasHands || (isNeck && frame.hasHead)
        if (hasSignal) lastHandMs = now

        val effectiveStatus = when {
            frame.status != TrackingStatus.ACTIVE && frame.status != TrackingStatus.DEMO -> frame.status
            hasSignal -> frame.status
            lastHandMs == 0L -> TrackingStatus.WAITING
            now - lastHandMs > LOST_GRACE_MS -> TrackingStatus.LOST
            else -> frame.status
        }

        var elevationR: Float? = null
        var elevationL: Float? = null
        val head = frame.headWorld
        if (!isNeck && head != null && frame.hasHands) {
            val body = estimator.estimate(head, frame.headYawRad)
            elevationR = frame.wristRWorld?.let { AngleMetrics.elevationDeg(it, body.shoulderR) }
            elevationL = frame.wristLWorld?.let { AngleMetrics.elevationDeg(it, body.shoulderL) }
        }

        val targetSide: Float? = when {
            ex.targetJoint.equals("HEAD_YAW", ignoreCase = true) ->
                HeadMetrics.yawSwingDeg(frame.headYawRad).takeIf { frame.hasHead }

            ex.targetJoint.equals("HEAD_PITCH", ignoreCase = true) ->
                HeadMetrics.pitchSwingDeg(frame.headPitchRad).takeIf { frame.hasHead }

            ex.targetJoint.endsWith("_L", ignoreCase = true) -> elevationL

            else -> elevationR
        }

        // 对称性采样(双臂都有数据时)
        if (elevationR != null && elevationL != null) {
            symmetrySamples.add(kotlin.math.abs(elevationR - elevationL).coerceAtMost(180f))
        }

        // 组间休息倒计时(休息中不计数)
        if (restingSecLeft > 0) {
            if (now - lastRestTickMs >= REST_TICK_MS) {
                restingSecLeft = (restingSecLeft - 1).coerceAtLeast(0)
                lastRestTickMs = now
            }
            if (restingSecLeft == 0) {
                counter.reset()
                setIndex++
                _state.update {
                    it.copy(
                        restingSecLeft = 0,
                        restingTotalSec = 0,
                        setIndex = setIndex,
                        repsDone = 0,
                        phase = cn.armpal.rehab.analysis.RepPhase.IDLE,
                        feedback = FeedbackLevel.WAITING,
                        lastMessage = "开始第 ${setIndex + 1} 组",
                    )
                }
                return
            }
            _state.update {
                it.copy(
                    restingSecLeft = restingSecLeft,
                    restingTotalSec = restingTotalSec,
                    tracking = effectiveStatus,
                    demoMode = src.isDemo,
                    lastMessage = "第 ${setIndex + 1} 组完成,休息一下",
                )
            }
            return
        }

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
        if (sessionRecorded) {
            // 全部组数已完成并落库:停止计数,只保留实时反馈
            _state.update { prev ->
                prev.copy(
                    tracking = effectiveStatus,
                    trackingLost = effectiveStatus == TrackingStatus.LOST,
                    demoMode = src.isDemo,
                    elevationR = elevationR,
                    elevationL = elevationL,
                    feedback = feedback,
                    phase = counter.phase,
                )
            }
            return
        }
        if (targetSide != null && effectiveStatus != TrackingStatus.LOST) {
            if (!isNeck && counter.phase != cn.armpal.rehab.analysis.RepPhase.IDLE) {
                // 采集本次重复的腕点轨迹(供 DTW 质量分;颈部动作无腕轨迹,不评分)
                val wrist = frame.wristRWorld ?: frame.wristLWorld
                if (wrist != null) {
                    repTrajectory.add(wrist)
                    if (repTrajectory.size > TRAJECTORY_CAP) repTrajectory.removeAt(0)
                }
            }

            val event = counter.onFrame(targetSide, now)
            if (event is RepEvent.RepCompleted) {
                totalReps++
                message = "第 $totalReps 次完成,最高 ${event.peakDeg.toInt()}°"
                scoreRepQuality(ex, frame)

                val setDone = counter.count >= ex.reps
                val hasMoreSets = setIndex + 1 < ex.sets
                if (setDone && hasMoreSets) {
                    restingSecLeft = ex.restSec.coerceAtLeast(1)
                    restingTotalSec = restingSecLeft
                    lastRestTickMs = now
                    message = "第 ${setIndex + 1} 组完成!"
                } else if (setDone) {
                    finishSession(ex, src)
                    message = "全部 ${ex.sets} 组完成,真棒!"
                }
            }
        }

        if (targetSide != null && targetSide > maxElevation) maxElevation = targetSide

        _state.update { prev ->
            prev.copy(
                tracking = effectiveStatus,
                trackingLost = effectiveStatus == TrackingStatus.LOST,
                demoMode = src.isDemo,
                elevationR = elevationR,
                elevationL = elevationL,
                targetDeg = ex.targetElevationDeg,
                repsDone = counter.count,
                targetReps = ex.reps,
                setIndex = setIndex,
                totalSets = ex.sets,
                peakDeg = counter.peakDeg,
                feedback = feedback,
                phase = counter.phase,
                lastMessage = if (message.isNotEmpty()) message else prev.lastMessage,
                restingSecLeft = restingSecLeft,
                restingTotalSec = restingTotalSec,
                totalReps = totalReps,
                sessionAvgQuality = sessionAvgQuality(),
                sessionCompleted = sessionRecorded,
            )
        }
    }

    /** 对刚完成的一次重复做 DTW 质量评分,并清空轨迹缓冲。 */
    private fun scoreRepQuality(ex: ExerciseTemplate, frame: cn.armpal.rehab.tracking.TrackingFrame) {
        val shoulder = frame.headWorld?.let { estimator.estimate(it, frame.headYawRad) }?.let { body ->
            if (ex.targetJoint.endsWith("_L", ignoreCase = true)) body.shoulderL else body.shoulderR
        }
        val wrist = frame.wristRWorld ?: frame.wristLWorld
        val q = if (shoulder == null || wrist == null) -1f else RepQualityScorer.score(
            template = templatePoints,
            actualWorld = repTrajectory.toList(),
            shoulder = shoulder,
            armLength = (wrist - shoulder).length.coerceAtLeast(0.2f),
        )
        if (q >= 0f) qualitySamples.add(q)
        repTrajectory.clear()
    }

    private fun sessionAvgQuality(): Float {
        if (qualitySamples.isEmpty()) return -1f
        return qualitySamples.sum() / qualitySamples.size
    }

    private fun avgSymmetry(): Float {
        if (symmetrySamples.isEmpty()) return -1f
        return symmetrySamples.sum() / symmetrySamples.size
    }

    /** 全部组数完成:构建会话记录并落库。 */
    private fun finishSession(ex: ExerciseTemplate, src: TrackingSource) {
        if (sessionRecorded) return
        sessionRecorded = true
        val startedAt = _state.value.sessionStartedAtMs
        val durationSec = ((SystemClock.elapsedRealtime() - startedAt) / 1000L).toInt().coerceAtLeast(0)
        val record = SessionRecord(
            exerciseId = ex.id,
            exerciseName = ex.name,
            reps = totalReps,
            targetReps = ex.reps * ex.sets,
            maxElevationDeg = maxElevation,
            avgQuality = sessionAvgQuality(),
            symmetryDeg = avgSymmetry(),
            durationSec = durationSec,
            setsDone = setIndex + 1,
            demoMode = src.isDemo,
            createdAtMs = System.currentTimeMillis(),
        )
        _state.update { it.copy(sessionCompleted = true) }
        sessionSink?.invoke(record)
        Log.i(TAG, "session completed: reps=${record.reps} quality=${record.avgQuality}")
    }

    /** 退出/重置时,若本场有有效次数且未落库,按部分完成落库。 */
    private fun flushSessionIfPending(reason: String) {
        val ex = exercise ?: return
        val src = source ?: return
        if (sessionRecorded || totalReps <= 0) return
        sessionRecorded = true
        val startedAt = _state.value.sessionStartedAtMs
        val durationSec = ((SystemClock.elapsedRealtime() - startedAt) / 1000L).toInt().coerceAtLeast(0)
        val record = SessionRecord(
            exerciseId = ex.id,
            exerciseName = ex.name,
            reps = totalReps,
            targetReps = ex.reps * ex.sets,
            maxElevationDeg = maxElevation,
            avgQuality = sessionAvgQuality(),
            symmetryDeg = avgSymmetry(),
            durationSec = durationSec,
            setsDone = setIndex + if (counter?.count ?: 0 > 0) 1 else 0,
            demoMode = src.isDemo,
            createdAtMs = System.currentTimeMillis(),
        )
        sessionSink?.invoke(record)
        Log.i(TAG, "session flushed ($reason): reps=${record.reps}")
    }

    private const val TAG = "TrainingSession"
    private const val FRAME_INTERVAL_MS = 33L
    private const val LOST_GRACE_MS = 500L
    private const val REST_TICK_MS = 1000L
    private const val TRAJECTORY_CAP = 150

    /** 接近目标的下限(0.6×目标角)用黄色提示"再抬一点"。 */
    private const val PARTIAL_RATIO = 0.6f
}
