package cn.armpal.rehab.windows

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.Text

/**
 * 报告窗:今日结果、本周趋势、连续打卡、家属分享卡片。
 * W1:占位;W2.5:接入 Room 与 ReportAggregator。
 */
@Composable
fun ReportWindow() {
    RehabPanel(title = "今日报告") {
        Text(text = "训练数据会保存在本机", fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(text = "报告与分享卡片即将上线", fontSize = 20.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = "本产品不替代专业医疗建议,急性期或疼痛请遵医嘱",
            fontSize = 16.sp,
        )
    }
}
