package cn.armpal.rehab.tracking

import cn.armpal.rehab.analysis.BodyFrameEstimator
import cn.armpal.rehab.analysis.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 演示脚本:没有任何真机/手势时,用合成轨迹驱动完整闭环(评分/路演必需)。
 *
 * - GOOD:双臂对称抬到 85° → 达标、计数、绿色反馈
 * - INSUFFICIENT:只抬到 55° → 黄色"再抬高一点",不计数
 * - ASYMMETRIC:右臂 85°、左臂明显偏低 → 对称性提示
 */
enum class DemoScript { GOOD, INSUFFICIENT, ASYMMETRIC }

/**
 * 脚本化追踪数据源。
 *
 * 姿态由"头显固定 + 肩部估计 + 臂长"反算腕点,和真实数据的角度语义完全一致,
 * 因此走的是同一条 analysis/session 链路,而不是给 UI 造假数字。
 */
class DemoTrackingSource(
    private val headWorld: Vec3 = Vec3(0f, 1.6f, 0f),
    private val armLength: Float = BodyFrameEstimator.DEFAULT_ARM_LENGTH,
) : TrackingSource {

    override val isDemo: Boolean = true

    private var startedAtMs = 0L
    private val estimator = BodyFrameEstimator()

    override fun start() {
        startedAtMs = 0L
    }

    override fun stop() {
        startedAtMs = 0L
    }

    override fun currentFrame(timestampMs: Long): TrackingFrame {
        if (startedAtMs == 0L) startedAtMs = timestampMs
        val t = ((timestampMs - startedAtMs).coerceAtLeast(0L)) / 1000f
        val script = scriptAt(t)
        val peak = when (script) {
            DemoScript.GOOD -> 85f
            DemoScript.INSUFFICIENT -> 55f
            DemoScript.ASYMMETRIC -> 85f
        }
        val elevation = elevationAt(t % CYCLE_SEC, peak)
        val leftElevation = when (script) {
            DemoScript.GOOD, DemoScript.INSUFFICIENT -> elevation
            DemoScript.ASYMMETRIC -> elevation * 0.5f
        }
        val body = estimator.estimate(headWorld, 0f)
        return TrackingFrame(
            timestampMs = timestampMs,
            status = TrackingStatus.DEMO,
            headWorld = headWorld,
            headYawRad = 0f,
            wristRWorld = wristFor(body.shoulderR, elevation, rightSide = true),
            wristLWorld = wristFor(body.shoulderL, leftElevation, rightSide = false),
        )
    }

    /** 每 15 秒切换一个脚本(GOOD → INSUFFICIENT → ASYMMETRIC)。 */
    private fun scriptAt(seconds: Float): DemoScript =
        when (((seconds / 15f).toInt()) % 3) {
            0 -> DemoScript.GOOD
            1 -> DemoScript.INSUFFICIENT
            else -> DemoScript.ASYMMETRIC
        }

    /**
     * 单次重复的 5 秒节奏:抬 1.5s → 保持 1.0s → 放 1.5s → 休息 1.0s。
     * 始终回到 3°(低于释放阈值 20°),保证状态机能正常计数。
     */
    private fun elevationAt(localSec: Float, peak: Float): Float = when {
        localSec < RAISE_SEC -> lerp(REST_DEG, peak, localSec / RAISE_SEC)
        localSec < RAISE_SEC + HOLD_SEC -> peak
        localSec < RAISE_SEC + HOLD_SEC + LOWER_SEC ->
            lerp(peak, REST_DEG, (localSec - RAISE_SEC - HOLD_SEC) / LOWER_SEC)

        else -> REST_DEG
    }

    private fun wristFor(shoulder: Vec3, elevationDeg: Float, rightSide: Boolean): Vec3 {
        val rad = elevationDeg * PI.toFloat() / 180f
        val side = if (rightSide) 1f else -1f
        // 前平面内的抬举:0°=垂下,90°=侧平举,180°=过头
        val direction = Vec3(side * sin(rad), -cos(rad), 0f)
        return shoulder + direction * armLength
    }

    private fun lerp(from: Float, to: Float, t: Float): Float =
        from + (to - from) * t.coerceIn(0f, 1f)

    companion object {
        private const val CYCLE_SEC = 5f
        private const val RAISE_SEC = 1.5f
        private const val HOLD_SEC = 1.0f
        private const val LOWER_SEC = 1.5f
        private const val REST_DEG = 3f
    }
}
