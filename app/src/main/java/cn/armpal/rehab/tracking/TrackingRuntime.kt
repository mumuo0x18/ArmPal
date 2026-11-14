package cn.armpal.rehab.tracking

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pico.spatial.tracking.DataProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** PICO 空间权限:手部追踪(仅 Full Space 可用)。 */
const val PERMISSION_HAND_TRACKING = "com.picovr.permission.HAND_TRACKING"

private const val REQUEST_HAND_TRACKING = 4001

/**
 * 追踪运行时:Provider 生命周期 + 权限请求/授权后重启 + 无手部数据时自动降级到演示模式。
 *
 * 修复(2026-11-14 评审 P0-3):
 * - 权限弹窗后**巡检授权结果**,一旦授权就 ensureStarted(),不再需要重启应用
 * - start() 返回 PENDING(例如尚未进入 Full Space)时会持续重试
 *
 * 演示模式兜底:真实模式启动 ~6s 仍没有任何有效手腕数据(模拟器/未开手势追踪/无真机)时自动切换,
 * 保证三窗闭环始终可演示;UI 会明确标注"演示模式",不冒充真实追踪。
 */
@Composable
fun rememberTrackingSource(): TrackingSource {
    val context = LocalContext.current
    var demoMode by remember { mutableStateOf(FORCE_DEMO_MODE) }
    val source = remember(demoMode) {
        if (demoMode) DemoTrackingSource() else PicoTrackingSource()
    }

    DisposableEffect(source) {
        source.start()
        onDispose { source.stop() }
    }

    // 权限巡检 + 授权后重启 Provider(真实数据源)
    LaunchedEffect(source) {
        val real = source as? PicoTrackingSource ?: return@LaunchedEffect
        val activity = context.findActivity()
        var requested = false
        while (isActive) {
            val granted = ContextCompat.checkSelfPermission(
                context, PERMISSION_HAND_TRACKING,
            ) == PackageManager.PERMISSION_GRANTED

            when {
                real.handSupportState == DataProvider.SupportState.WITHOUT_PERMISSION &&
                    !granted && !requested && activity != null -> {
                    requested = true
                    Log.i(TAG, "request HAND_TRACKING permission")
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(PERMISSION_HAND_TRACKING), REQUEST_HAND_TRACKING,
                    )
                }

                real.handSupportState == DataProvider.SupportState.SUPPORTED -> {
                    real.ensureStarted()
                }
            }
            delay(500)
        }
    }

    // 无真机兜底:真实模式 6 秒没有有效手腕数据 → 演示模式
    LaunchedEffect(source) {
        val real = source as? PicoTrackingSource ?: return@LaunchedEffect
        var sawHands = false
        repeat(60) {
            delay(100)
            if (real.currentFrame(SystemClock.elapsedRealtime()).hasHands) sawHands = true
        }
        if (!sawHands) {
            Log.i(TAG, "no hand data in 6s, switching to demo mode")
            demoMode = true
        }
    }

    return source
}

private const val TAG = "TrackingRuntime"

/** 开发调试:强制从演示模式启动。 */
private const val FORCE_DEMO_MODE = false

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
