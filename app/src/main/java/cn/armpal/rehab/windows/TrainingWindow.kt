package cn.armpal.rehab.windows

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.armpal.rehab.tracking.HandTrackingStore
import com.pico.spatial.ui.design.Text

/**
 * 训练主窗(中央大窗):示范者、节奏引导、组数进度。
 * W1:占位说明 + 手部追踪状态;W2:接入示范者与 TrainingSessionViewModel。
 */
@Composable
fun TrainingWindow() {
    val snap = HandTrackingStore.snapshot
    RehabPanel(title = "臂伴 · 今日训练") {
        Text(text = "坐姿抬臂", fontSize = 28.sp)
        Spacer(Modifier.height(12.dp))
        Text(text = spikeStatusText(snap), fontSize = 22.sp)
    }
}

/** Spike B 状态可读化(适老文案:少术语)。 */
fun spikeStatusText(snap: cn.armpal.rehab.tracking.HandTrackingSnapshot): String =
    when {
        snap.isTracking -> "手部追踪:正常"
        snap.started -> "手部追踪:等待数据…"
        snap.supportState == com.pico.spatial.tracking.DataProvider.SupportState.WITHOUT_PERMISSION ->
            "请在系统弹窗中允许手部追踪"
        snap.supportState == com.pico.spatial.tracking.DataProvider.SupportState.NOT_IN_FULL_SPACE ->
            "请进入 Full Space(Stage)后使用"
        else -> "手部追踪:未就绪"
    }
