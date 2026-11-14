package cn.armpal.rehab.windows

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.armpal.rehab.tracking.HandTrackingStore
import com.pico.spatial.ui.design.Text

/**
 * 动作纠正侧窗:轨迹对比、角度仪表、颜色+语音反馈。
 * W1:显示追踪状态与腕坐标原始读数(spike 可视化);W2 接入角度仪表。
 */
@Composable
fun CorrectWindow() {
    val snap = HandTrackingStore.snapshot
    RehabPanel(title = "动作纠正") {
        val r = snap.wristRWorld
        val l = snap.wristLWorld
        Text(
            text = if (r != null) "右手腕 x=%.2f y=%.2f z=%.2f".format(r[0], r[1], r[2])
            else "右手腕:无数据",
            fontSize = 20.sp,
            color = Color(0xFF4ADE80),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (l != null) "左手腕 x=%.2f y=%.2f z=%.2f".format(l[0], l[1], l[2])
            else "左手腕:无数据",
            fontSize = 20.sp,
            color = Color(0xFF60A5FA),
        )
        Spacer(Modifier.height(12.dp))
        Text(text = "跟随示范,慢慢抬臂", fontSize = 20.sp)
    }
}
