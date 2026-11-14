package cn.armpal.rehab.tracking

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pico.spatial.tracking.DataProvider
import com.pico.spatial.tracking.hand.HandJoint
import com.pico.spatial.tracking.hand.HandTrackingData
import com.pico.spatial.tracking.hand.HandTrackingProvider

/** PICO 空间权限:手部追踪(仅 Full Space 可用)。 */
const val PERMISSION_HAND_TRACKING = "com.picovr.permission.HAND_TRACKING"

/**
 * 手部追踪快照:供 UI 层消费的最小数据面。
 * left/right 可为 null(追踪丢失),UI 必须做空安全处理(开发文档 §3.3)。
 */
data class HandTrackingSnapshot(
    val supportState: DataProvider.SupportState = DataProvider.SupportState.UNKNOWN,
    val started: Boolean = false,
    val timestampMs: Long = 0L,
    val wristRWorld: FloatArray? = null, // x,y,z(米),右手腕
    val wristLWorld: FloatArray? = null, // x,y,z(米),左手腕
) {
    val isTracking: Boolean get() = wristRWorld != null || wristLWorld != null

    override fun equals(other: Any?): Boolean = other is HandTrackingSnapshot &&
        supportState == other.supportState && started == other.started &&
        timestampMs == other.timestampMs && wristRWorld.contentEquals(other.wristRWorld) &&
        wristLWorld.contentEquals(other.wristLWorld)

    override fun hashCode(): Int = listOf(supportState, started, timestampMs).hashCode()
}

/**
 * 手部追踪接入(Spike B):
 * - Provider 生命周期严格 start/stop 配对(30 分钟稳定红线)
 * - dataFlow 收集在 DisposableEffect 生命周期内,退出即取消
 * - supportState 直接暴露 SDK 判定(WITHOUT_PERMISSION / NOT_IN_FULL_SPACE / DEVICE_NOT_SUPPORTED)
 *
 * 6.2.8 API:HandTrackingProvider implements DataProvider<HandTrackingData>。
 */
@Composable
fun rememberHandTracking(): State<HandTrackingSnapshot> {
    val context = LocalContext.current
    val provider = remember { HandTrackingProvider() }
    val snapshot = remember { mutableStateOf(HandTrackingSnapshot()) }

    DisposableEffect(Unit) {
        var lastLogMs = 0L
        val listener = object : DataProvider.DataListener<HandTrackingData> {
            override fun onProvideData(data: HandTrackingData) {
                val now = System.currentTimeMillis()
                val right = data.right
                val left = data.left
                val wristR = right?.get(HandJoint.Index.WRIST)
                val wristL = left?.get(HandJoint.Index.WRIST)
                val snap = HandTrackingSnapshot(
                    supportState = provider.supportState,
                    started = provider.state == DataProvider.State.STARTED,
                    timestampMs = System.currentTimeMillis(),
                    wristRWorld = wristR?.position?.let { floatArrayOf(it.x, it.y, it.z) },
                    wristLWorld = wristL?.position?.let { floatArrayOf(it.x, it.y, it.z) },
                )
                snapshot.value = snap
                HandTrackingStore.snapshot = snap
                // Spike B 证据:限频打印腕关节世界坐标
                if (now - lastLogMs > 1000) {
                    lastLogMs = now
                    Log.i(
                        "HandTracker",
                        "support=${provider.supportState} state=${provider.state} " +
                            "wristR=${wristR?.position} wristL=${wristL?.position}",
                    )
                }
            }
        }

        val startResult = provider.start()
        provider.addListener(listener)
        Log.i("HandTracker", "start=$startResult support=${provider.supportState}")

        // 追踪支持但缺权限 → 走运行时权限请求
        if (provider.supportState == DataProvider.SupportState.WITHOUT_PERMISSION) {
            (context.findActivity())?.let { activity ->
                if (ContextCompat.checkSelfPermission(activity, PERMISSION_HAND_TRACKING)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    androidx.core.app.ActivityCompat.requestPermissions(
                        activity, arrayOf(PERMISSION_HAND_TRACKING), REQ_HAND,
                    )
                }
            }
        }

        onDispose {
            provider.removeListener(listener)
            provider.stop()
            Log.i("HandTracker", "stopped (dispose)")
        }
    }

    return snapshot
}

private const val REQ_HAND = 4001

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * W1 过渡用的全局状态总线:三窗共享同一份手部追踪快照。
 * W2 将由 TrainingSessionViewModel(StateFlow)取代,窗口不得直接互访(开发文档 §1)。
 */
object HandTrackingStore {
    var snapshot: HandTrackingSnapshot by androidx.compose.runtime.mutableStateOf(
        HandTrackingSnapshot(),
    )
}
