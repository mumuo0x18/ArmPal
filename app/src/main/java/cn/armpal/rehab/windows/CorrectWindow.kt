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
import kotlin.math.abs

/**
 * 动作纠正侧窗:抬举角仪表、左右对称性、颜色反馈、追踪降级提示。
 */
@Composable
fun CorrectWindow() {
    val state by TrainingSessionController.state.collectAsState()
    val isNeck = state.exercise?.targetJoint
        ?.let { cn.armpal.rehab.analysis.HeadMetrics.isNeckExercise(it) } == true
    RehabPanel(title = "动作纠正") {
        val eR = state.elevationR
        val eL = state.elevationL
        // 颈部动作:显示头部活动幅度;臂部动作:显示右臂抬举角
        val primaryDeg: Float? = if (isNeck) state.headSwingDeg else state.elevationR
        val primaryLabel = if (isNeck) "活动幅度(目标 ${state.targetDeg.toInt()}°)"
        else "右臂抬举(目标 ${state.targetDeg.toInt()}°)"

        Text(
            text = if (primaryDeg != null) "${primaryDeg.toInt()}°" else "--°",
            fontSize = 64.sp,
            color = feedbackColor(state.feedback),
        )
        Text(text = primaryLabel, fontSize = 20.sp, color = TextSecondary)
        Spacer(Modifier.height(14.dp))
        AngleGauge(elevationDeg = primaryDeg, targetDeg = state.targetDeg)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "目标线 ${state.targetDeg.toInt()}° · 保持中角标",
            fontSize = 16.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(22.dp))

        Text(
            text = feedbackText(state.feedback),
            fontSize = 30.sp,
            color = feedbackColor(state.feedback),
        )
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isNeck) {
                Text(
                    text = "缓慢转到位,停一拍再回正",
                    fontSize = 22.sp,
                    color = AccentBlue,
                )
            } else {
                Text(
                    text = "左臂 ${eL?.toInt()?.toString() ?: "--"}°",
                    fontSize = 22.sp,
                    color = AccentBlue,
                )
                Spacer(Modifier.width(18.dp))
                val symmetry = if (eR != null && eL != null) abs(eR - eL) else null
                Text(
                    text = if (symmetry != null) "左右差 ${symmetry.toInt()}°" else "左右差 --",
                    fontSize = 22.sp,
                    color = if (symmetry != null && symmetry > 25f) AccentAmber else TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(22.dp))

        TrackingHint(state = state)
    }
}

@Composable
internal fun TrackingHint(state: cn.armpal.rehab.session.SessionState) {
    val (text, color) = when {
        state.demoMode -> Pair("演示模式:模拟轨迹,用于无真机演示", AccentAmber)
        state.trackingLost -> Pair("追踪丢失超过 0.5 秒,已切换为跟随模式", AccentRed)
        state.tracking == cn.armpal.rehab.tracking.TrackingStatus.PERMISSION_REQUIRED ->
            Pair("请允许手部追踪权限", AccentAmber)

        state.tracking == cn.armpal.rehab.tracking.TrackingStatus.WAITING ->
            Pair("手部追踪:正在准备…", TextSecondary)

        state.tracking == cn.armpal.rehab.tracking.TrackingStatus.ACTIVE ->
            Pair("手部追踪:正常", AccentGreen)

        else -> Pair(trackingText(state.tracking, state.demoMode), TextSecondary)
    }
    Row(
        modifier = Modifier
            .background(TrackBackground, RoundedCornerShape(10.dp))
            .height(40.dp)
            .fillMaxWidth(0.92f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.018f)
                .background(color, RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = text, fontSize = 17.sp, color = color)
    }
}
