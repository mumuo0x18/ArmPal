package cn.armpal.rehab.windows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.armpal.rehab.session.TrainingSessionController
import com.pico.spatial.ui.design.Text

/**
 * 训练主窗:动作名、目标、次数进度、实时反馈、追踪状态。
 * 数据全部来自单一状态中枢 [TrainingSessionController](三窗共享)。
 */
@Composable
fun TrainingWindow() {
    val state by TrainingSessionController.state.collectAsState()
    RehabPanel(title = "臂伴 · 今日训练") {
        Text(
            text = state.exercise?.name ?: "坐姿抬臂",
            fontSize = 34.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "目标:抬到 ${state.targetDeg.toInt()}°,保持 0.5 秒",
            fontSize = 20.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${state.repsDone}",
                fontSize = 76.sp,
                color = if (state.repsDone > 0) AccentGreen else TextPrimary,
            )
            Text(
                text = " / ${state.targetReps} 次",
                fontSize = 24.sp,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.height(10.dp))
        ProgressBar(progress = state.progress)
        Spacer(Modifier.height(22.dp))

        Text(
            text = feedbackText(state.feedback),
            fontSize = 32.sp,
            color = feedbackColor(state.feedback),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = state.lastMessage,
            fontSize = 18.sp,
            color = AccentBlue,
        )
        Spacer(Modifier.height(24.dp))

        if (state.targetReached) {
            Text(text = "今日目标已完成 🎉", fontSize = 24.sp, color = AccentGreen)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = trackingText(state.tracking, state.demoMode),
            fontSize = 18.sp,
            color = if (state.demoMode) AccentAmber else TextSecondary,
        )
    }
}

@Composable
internal fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .width(520.dp)
            .height(14.dp)
            .background(TrackBackground, RoundedCornerShape(7.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(AccentGreen, RoundedCornerShape(7.dp)),
        )
    }
}

/** 抬举角仪表:0°~180° 刻度条 + 目标标记。 */
@Composable
internal fun AngleGauge(elevationDeg: Float?, targetDeg: Float) {
    val value = (elevationDeg ?: 0f).coerceIn(0f, 180f)
    Box(
        modifier = Modifier
            .width(480.dp)
            .height(20.dp)
            .background(TrackBackground, RoundedCornerShape(10.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(value / 180f)
                .background(
                    if (value >= targetDeg) AccentGreen else AccentAmber,
                    RoundedCornerShape(10.dp),
                ),
        )
        // 目标刻度
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth((targetDeg / 180f).coerceIn(0f, 1f)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Color.White),
            )
        }
    }
}
