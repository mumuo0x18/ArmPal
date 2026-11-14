package cn.armpal.rehab.windows

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.armpal.rehab.session.TrainingSessionController
import com.pico.spatial.ui.design.Text

/**
 * 报告窗:今日结果(内存态,W2.5 接 Room 后持久化)、达标情况、合规免责声明。
 */
@Composable
fun ReportWindow() {
    val state by TrainingSessionController.state.collectAsState()
    RehabPanel(title = "今日报告") {
        Text(text = "完成次数", fontSize = 22.sp, color = TextSecondary)
        Text(
            text = "${state.repsDone} / ${state.targetReps} 次",
            fontSize = 54.sp,
            color = if (state.targetReached) AccentGreen else TextPrimary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "最高抬举 ${state.peakDeg.toInt()}°  ·  目标 ${state.targetDeg.toInt()}°",
            fontSize = 22.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "组数 ${(state.setIndex + 1).coerceAtMost(state.totalSets)} / ${state.totalSets}",
            fontSize = 20.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (state.targetReached) "今日目标已达成 🎉" else "已完成 ${(state.progress * 100).toInt()}%",
            fontSize = 24.sp,
            color = if (state.targetReached) AccentGreen else AccentAmber,
        )
        Spacer(Modifier.height(26.dp))
        Text(
            text = "训练数据在本机处理,不上传云端",
            fontSize = 17.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "本产品不替代专业医疗建议,急性期或疼痛请遵医嘱",
            fontSize = 16.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "周报与家属分享卡片将在后续版本上线",
            fontSize = 15.sp,
            color = TextSecondary,
        )
    }
}
