package cn.armpal.rehab.windows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.armpal.rehab.analysis.HeadMetrics
import cn.armpal.rehab.anchor.AnchorManager
import cn.armpal.rehab.content.DemoAvatarInline
import cn.armpal.rehab.content.ExerciseSelection
import cn.armpal.rehab.data.SettingsStore
import cn.armpal.rehab.session.TrainingSessionController
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.Text
import kotlinx.coroutines.launch

/**
 * 训练主窗:动作名、目标、次数进度、实时反馈、追踪状态。
 * 首次启动先渲染免责声明 gate(确认后才显示训练内容,HomeStage 据此决定开其余两窗)。
 * 数据全部来自单一状态中枢 [TrainingSessionController](三窗共享)。
 */
@Composable
fun TrainingWindow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 免责声明 gate:确认前占满训练主窗(合规红线,PRD §5)
    if (DisclaimerStore.accepted == false) {
        DisclaimerGate(
            onAccept = {
                DisclaimerStore.accepted = true
                scope.launch { SettingsStore.setDisclaimerAccepted(context, true) }
            },
        )
        return
    }

    val state by TrainingSessionController.state.collectAsState()
    RehabPanel(title = "臂伴 · 今日训练") {
        // 动作选择 chips(两列一排;选中项高亮)
        if (ExerciseSelection.all.size > 1) {
            ExerciseSelection.all.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { ex ->
                        val selected = state.exercise?.id == ex.id
                        Button(
                            onClick = { ExerciseSelection.currentId = ex.id },
                            colors = ButtonDefaults.buttonColors().copy(
                                containerColor = if (selected) AccentBlue else TrackBackground,
                                contentColor = if (selected) TextPrimary else TextSecondary,
                            ),
                        ) {
                            Text(
                                text = if (selected) "● " + ex.name else ex.name,
                                fontSize = 17.sp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = state.exercise?.name ?: "坐姿抬臂",
            fontSize = 34.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        val neck = state.exercise?.targetJoint?.let { HeadMetrics.isNeckExercise(it) } == true
        Text(
            text = if (neck) "目标:转到 ${state.targetDeg.toInt()}°,停 0.4 秒"
            else "目标:抬到 ${state.targetDeg.toInt()}°,保持 0.5 秒",
            fontSize = 20.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(18.dp))

        if (state.restingSecLeft > 0) {
            // 组间休息倒计时
            val restProgress = if (state.restingTotalSec > 0) {
                state.restingSecLeft.toFloat() / state.restingTotalSec
            } else 0f
            Text(
                text = "第 ${state.setIndex + 1} 组完成 🎉",
                fontSize = 28.sp,
                color = AccentGreen,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "休息 ${state.restingSecLeft} 秒,下一组马上开始",
                fontSize = 40.sp,
                color = AccentAmber,
            )
            Spacer(Modifier.height(10.dp))
            ProgressBar(progress = restProgress)
            Spacer(Modifier.height(18.dp))
        }

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
        Spacer(Modifier.height(16.dp))

        // 3D 示范者(窗口内嵌 SpatialView;臂部动作时跟随实时抬举角摆臂)
        if (!neck) {
            DemoAvatarInline()
            Spacer(Modifier.height(12.dp))
        }

        if (state.targetReached) {
            Text(text = "今日目标已完成 🎉", fontSize = 24.sp, color = AccentGreen)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = trackingText(state.tracking, state.demoMode),
            fontSize = 18.sp,
            color = if (state.demoMode) AccentAmber else TextSecondary,
        )

        // —— 训练位锚点(行为设计:到点到位;状态 + 设置/找回,降级不阻断)——
        Spacer(Modifier.height(14.dp))
        val anchorState by AnchorManager.state.collectAsState()
        val anchorText = when (val a = anchorState) {
            is AnchorManager.AnchorState.Ready -> "训练位已就绪 ✅(连续打卡见报告窗)"
            AnchorManager.AnchorState.NotFound -> "不在训练位?走过去 5 米内,或直接训练"
            is AnchorManager.AnchorState.Failed -> "训练位不可用(${a.message.take(20)}),不影响训练"
            AnchorManager.AnchorState.Working -> "训练位处理中…"
            AnchorManager.AnchorState.Idle -> "还没设置训练位"
        }
        Text(text = anchorText, fontSize = 16.sp, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val head = state.headWorld ?: cn.armpal.rehab.analysis.Vec3(0f, 1.2f, -0.3f)
                    scope.launch {
                        AnchorManager.createTrainingSpot(
                            context,
                            com.pico.spatial.core.math.Vector3(head.x, head.y, head.z),
                        )
                    }
                },
            ) {
                Text(text = "这里设为训练位", fontSize = 16.sp)
            }
        }
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
