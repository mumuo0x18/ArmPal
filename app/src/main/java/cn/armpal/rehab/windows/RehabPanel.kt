package cn.armpal.rehab.windows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.LocalContentColor
import com.pico.spatial.ui.design.Text

/**
 * 适老窗口公共骨架:深底高对比、大字号、居中排版。
 *
 * P0-5 修复:面板背景是深色,但内容默认文字色是黑色(LocalContentColor 默认值),
 * 实测对比度约 1.1:1。这里用 ProvideContentColor 显式提供亮色内容色,
 * 并给标题单独显式颜色,保证"大字 + 高对比"同时成立。
 */
@Composable
fun RehabPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides TextPrimary) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(PanelBackground, RoundedCornerShape(24.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = title,
                fontSize = 32.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}
