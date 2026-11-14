package cn.armpal.rehab

import cn.armpal.rehab.content.HomeStage
import cn.armpal.rehab.tracking.rememberHandTracking
import cn.armpal.rehab.windows.CorrectWindow
import cn.armpal.rehab.windows.ReportWindow
import cn.armpal.rehab.windows.TrainingWindow
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultStage
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize
import com.pico.spatial.ui.platform.LengthUnit

/**
 * 臂伴三窗布局(Spike A/B):
 * - 训练主窗(中央):持有手部追踪 Provider(全局仅此一份)
 * - 纠正侧窗 / 报告窗:订阅 HandTrackingStore
 * 窗口尺寸单位为米;W4 打磨时按视线高度与摆放重新调整。
 */
fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultStage {
            PicoTheme {
                HomeStage()
            }
        }

        WindowContainer(
            "training",
            Form.Planar,
            WindowContainerSize(1.10f, 0.85f, 0.04f, LengthUnit.Meters),
        ) {
            PicoTheme {
                // Provider 生命周期绑定在主窗组合内(DisposableEffect start/stop 配对)
                rememberHandTracking()
                TrainingWindow()
            }
        }

        WindowContainer(
            "correct",
            Form.Planar,
            WindowContainerSize(0.62f, 0.72f, 0.04f, LengthUnit.Meters),
        ) {
            PicoTheme {
                CorrectWindow()
            }
        }

        WindowContainer(
            "report",
            Form.Planar,
            WindowContainerSize(0.55f, 0.72f, 0.04f, LengthUnit.Meters),
        ) {
            PicoTheme {
                ReportWindow()
            }
        }
    }
