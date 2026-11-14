package cn.armpal.rehab

import androidx.compose.ui.unit.dp
import cn.armpal.rehab.content.HomeStage
import cn.armpal.rehab.windows.CorrectWindow
import cn.armpal.rehab.windows.ReportWindow
import cn.armpal.rehab.windows.TrainingWindow
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultStage
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.Placement
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize

/**
 * 臂伴三窗布局(对齐官方 WelcomeSpace 多窗范式):
 * - Stage(Full Space):承载追踪运行时与 3D 场景(追踪能力仅 Stage 可用)
 * - 训练主窗(中央)/ 纠正侧窗(左)/ 报告窗(右)
 * - 摆放:主窗默认位置(视线正前方),侧窗/报告窗通过 placement 相对主窗左右摆放,
 *   打开顺序在 HomeStage 中保证(先主窗,后侧窗),避免三窗重叠
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
            defaultSize = WindowContainerSize(width = 1080.dp, height = 760.dp, depth = 40.dp),
            placement = { Placement.none() },
        ) {
            PicoTheme {
                TrainingWindow()
            }
        }

        WindowContainer(
            id = "correct",
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 560.dp, height = 720.dp, depth = 40.dp),
            placement = {
                containers.firstOrNull { it.id == "training" }
                    ?.let { Placement.left(it, 32.dp) }
                    ?: Placement.none()
            },
        ) {
            PicoTheme {
                CorrectWindow()
            }
        }

        WindowContainer(
            id = "report",
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 520.dp, height = 720.dp, depth = 40.dp),
            placement = {
                containers.firstOrNull { it.id == "training" }
                    ?.let { Placement.right(it, 32.dp) }
                    ?: Placement.none()
            },
        ) {
            PicoTheme {
                ReportWindow()
            }
        }
    }
