package cn.armpal.rehab.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cn.armpal.rehab.tracking.rememberHandTracking
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator

/**
 * Stage(Full Space)3D 场景内容。
 * - 手部追踪 Provider 在 Stage 内容中启动(追踪能力仅 Full Space 可用),
 *   快照写入 HandTrackingStore 供三个窗口订阅
 * - 三个 WindowContainer 在 mainApp 中声明,这里显式 open
 *   (6.2 行为:声明 ≠ 打开,参照官方 WelcomeSpace)
 * W2 在此挂示范者实体。
 */
@Composable
fun HomeStage() {
    val navigator = LocalSpatialNavigator.current

    // Provider 生命周期绑定 Stage 组合(DisposableEffect start/stop 配对)
    rememberHandTracking()

    LaunchedEffect(Unit) {
        navigator.openWindowContainer("training")
        navigator.openWindowContainer("correct")
        navigator.openWindowContainer("report")
    }

    // W2:示范者实体(SpatialView / ECS Entity)在此添加
}
