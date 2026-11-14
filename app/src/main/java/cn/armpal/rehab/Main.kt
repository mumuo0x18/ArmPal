package cn.armpal.rehab

import androidx.compose.ui.unit.dp
import cn.armpal.rehab.content.HomeStage
import cn.armpal.rehab.windows.CorrectWindow
import cn.armpal.rehab.windows.ReportWindow
import cn.armpal.rehab.windows.TrainingWindow
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultStage
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize

/**
 * 臂伴三窗布局(对齐官方 WelcomeSpace 多窗范式):
 * - Stage(Full Space):承载手部追踪 Provider 与 3D 场景(追踪能力仅 Stage 可用)
 * - 训练主窗 / 纠正侧窗 / 报告窗:三个 WindowContainer,经 HandTrackingStore 订阅状态
 * 尺寸单位为 dp(6.2 DSL 约定);W4 打磨时按视线与摆放再调。
 */
fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultStage {
            PicoTheme {
                HomeStage()
            }
        }

        WindowContainer(
            id = "training",
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 1100.dp, height = 800.dp, depth = 40.dp),
        ) {
            PicoTheme {
                TrainingWindow()
            }
        }

        WindowContainer(
            id = "correct",
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 620.dp, height = 720.dp, depth = 40.dp),
        ) {
            PicoTheme {
                CorrectWindow()
            }
        }

        WindowContainer(
            id = "report",
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 550.dp, height = 720.dp, depth = 40.dp),
        ) {
            PicoTheme {
                ReportWindow()
            }
        }
    }
