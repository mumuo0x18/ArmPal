package cn.armpal.rehab.windows

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Text

/**
 * 免责声明确认状态(跨组合共享:HomeStage 用它决定开窗策略,训练窗渲染 gate)。
 * null = 加载中;true = 已确认;false = 待确认。
 */
object DisclaimerStore {
    var accepted: Boolean? by mutableStateOf(null)
}

/**
 * 首次启动免责声明 gate(PRD §5 合规红线):
 * "本产品不替代专业医疗建议,急性期或疼痛请遵医嘱"。
 * 确认前遮住 Stage 内容;确认一次后不再显示(DataStore)。
 */
@Composable
fun DisclaimerGate(onAccept: () -> Unit) {
    RehabPanel(
        title = "使用须知",
        modifier = Modifier,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "本应用提供居家上肢/颈部活动度训练陪伴与动作引导。",
                    fontSize = 22.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "不替代专业医疗建议,不做诊断或疗效宣称。",
                    fontSize = 22.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "急性期、疼痛加重或不适时,请立即停止并遵医嘱。",
                    fontSize = 22.sp,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onAccept) {
                    Text(text = "我知道了,开始使用", fontSize = 24.sp)
                }
            }
        },
    )
}
