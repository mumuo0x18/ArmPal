package cn.armpal.rehab.tracking

import android.util.Log
import cn.armpal.rehab.analysis.Vec3
import com.pico.spatial.tracking.DataProvider
import com.pico.spatial.tracking.hand.HandJoint
import com.pico.spatial.tracking.hand.HandPose
import com.pico.spatial.tracking.hand.HandTrackingProvider
import com.pico.spatial.tracking.hmd.HMDTrackingProvider

/**
 * 真实手部 + 头显追踪数据源。
 *
 * 关键修复(2026-11-14 评审 P0-3):
 * - Provider 的 start/stop 由生命周期管理,但**授权成功后允许再次 ensureStarted()**,
 *   否则首次运行时用户点"允许"之后手部追踪永远不启动
 * - HMD Provider 提供头显位姿,用于肩部估计与 yaw 旋转(原来只用手,没有头显数据)
 * - DataListener 回调不在主线程,本类只做 `latestData` 轮询,由会话层在主循环消费
 */
class PicoTrackingSource(
    private val hand: HandTrackingProvider = HandTrackingProvider(),
    private val hmd: HMDTrackingProvider = HMDTrackingProvider(),
) : TrackingSource {

    override fun start() {
        hmd.start()
        hand.start()
        Log.i(TAG, "start hand=${hand.state}/${hand.supportState} hmd=${hmd.state}/${hmd.supportState}")
    }

    override fun stop() {
        hand.stop()
        hmd.stop()
        Log.i(TAG, "stop")
    }

    /** 授权完成后由外部巡检调用:只要还支持就重试启动。 */
    fun ensureStarted() {
        if (hand.supportState == DataProvider.SupportState.SUPPORTED &&
            hand.state != DataProvider.State.STARTED
        ) {
            hand.start()
        }
        if (hmd.state != DataProvider.State.STARTED) {
            hmd.start()
        }
    }

    val handSupportState: DataProvider.SupportState get() = hand.supportState
    val handProviderState: DataProvider.State get() = hand.state

    override fun currentFrame(timestampMs: Long): TrackingFrame {
        val hmdData = runCatching { hmd.latestData }.getOrNull()
        val handData = runCatching { hand.latestData }.getOrNull()

        val pose = hmdData?.hmdPose
        val head = pose?.position?.toVec3()?.takeIf { it.isSane() }
        val euler = runCatching { pose?.rotation?.toEulerAngles() }.getOrNull()
        val yaw = euler?.yaw ?: 0f
        val pitch = euler?.pitch ?: 0f

        return TrackingFrame(
            timestampMs = timestampMs,
            status = mapStatus(),
            headWorld = head,
            headYawRad = yaw,
            headPitchRad = pitch,
            wristRWorld = handData?.right?.wristWorld(),
            wristLWorld = handData?.left?.wristWorld(),
        )
    }

    private fun mapStatus(): TrackingStatus = when (hand.supportState) {
        DataProvider.SupportState.WITHOUT_PERMISSION -> TrackingStatus.PERMISSION_REQUIRED
        DataProvider.SupportState.NOT_IN_FULL_SPACE -> TrackingStatus.NOT_IN_FULL_SPACE
        DataProvider.SupportState.DEVICE_NOT_SUPPORTED,
        DataProvider.SupportState.NONE,
        -> TrackingStatus.UNSUPPORTED

        DataProvider.SupportState.UNKNOWN -> TrackingStatus.WAITING
        DataProvider.SupportState.SUPPORTED ->
            if (hand.state == DataProvider.State.STARTED) TrackingStatus.ACTIVE
            else TrackingStatus.WAITING
    }

    private fun HandPose.wristWorld(): Vec3? = runCatching {
        this[HandJoint.Index.WRIST].position.toVec3()
    }.getOrNull()?.takeIf { it.isSane() }

    private fun com.pico.spatial.core.math.Vector3.toVec3() = Vec3(x, y, z)

    /** 过滤模拟器/丢失时的 (0,0,0) 与 NaN,避免"假装在追踪"。 */
    private fun Vec3.isSane(): Boolean =
        x.isFinite() && y.isFinite() && z.isFinite() &&
            !(x == 0f && y == 0f && z == 0f) &&
            x * x + y * y + z * z < 25f

    companion object {
        private const val TAG = "PicoTrackingSource"
    }
}
