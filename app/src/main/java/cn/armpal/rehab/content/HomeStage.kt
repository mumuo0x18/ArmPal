package cn.armpal.rehab.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cn.armpal.rehab.analysis.ExerciseTemplate
import cn.armpal.rehab.session.TrainingSessionController
import cn.armpal.rehab.tracking.rememberTrackingSource
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import kotlinx.coroutines.delay

/**
 * Stage(Full Space)内容:
 * - 追踪运行时(手部 + HMD)在这里启动;追踪能力仅 Full Space 可用
 * - 加载动作模板 → 绑定训练会话 → 三窗订阅同一份 SessionState
 * - 三个 WindowContainer 在 mainApp 中声明,这里按顺序显式 open
 *   (6.2 行为:声明 ≠ 打开,参照官方 WelcomeSpace;顺序打开才能用 placement 相对摆放)
 *
 * W2 说明:3D 示范者实体(SpatialView/ECS)尚未接入,当前示范由主窗文案 + 侧窗角度仪表承担;
 * 这是下一步(动作库/真人模型)的插入点。
 */
@Composable
fun HomeStage() {
    val navigator = LocalSpatialNavigator.current
    val context = LocalContext.current

    var exercise by remember { mutableStateOf<ExerciseTemplate?>(null) }
    LaunchedEffect(Unit) {
        exercise = cn.armpal.rehab.content.ExerciseRepository.load(context).firstOrNull()
    }

    // 追踪数据源(含权限巡检/授权后重启/无真机自动演示兜底)
    val source = rememberTrackingSource()

    LaunchedEffect(source, exercise) {
        val template = exercise ?: return@LaunchedEffect
        TrainingSessionController.bind(source, template)
    }

    DisposableEffect(Unit) {
        onDispose { TrainingSessionController.unbind() }
    }

    // 三窗顺序打开:先训练主窗,侧窗/报告窗再相对它摆放,避免三个窗口叠在默认位置
    LaunchedEffect(Unit) {
        navigator.openWindowContainer("training")
        delay(600)
        navigator.openWindowContainer("correct")
        delay(600)
        navigator.openWindowContainer("report")
    }
}
