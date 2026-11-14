package cn.armpal.rehab.windows

import androidx.compose.ui.graphics.Color
import cn.armpal.rehab.session.FeedbackLevel
import cn.armpal.rehab.tracking.TrackingStatus

/** 适老高对比色板(深底 + 亮字,对比度按 WCAG 目标设计)。 */
internal val PanelBackground = Color(0xF214171D)
internal val TextPrimary = Color(0xFFF7F8FA)
internal val TextSecondary = Color(0xFFB8C0CC)
internal val TrackBackground = Color(0xFF2B313B)
internal val AccentGreen = Color(0xFF34D399)
internal val AccentAmber = Color(0xFFFBBF24)
internal val AccentRed = Color(0xFFF87171)
internal val AccentBlue = Color(0xFF60A5FA)

/** 实时反馈文案(适老:少术语、动词开头)。 */
internal fun feedbackText(level: FeedbackLevel): String = when (level) {
    FeedbackLevel.WAITING -> "准备开始,慢慢抬臂"
    FeedbackLevel.TOO_LOW -> "再抬高一点"
    FeedbackLevel.PARTIAL -> "快到了,再高一点"
    FeedbackLevel.GOOD -> "很好,保持住"
    FeedbackLevel.HOLDING -> "保持中…"
}

internal fun feedbackColor(level: FeedbackLevel): Color = when (level) {
    FeedbackLevel.TOO_LOW, FeedbackLevel.PARTIAL -> AccentAmber
    FeedbackLevel.GOOD, FeedbackLevel.HOLDING -> AccentGreen
    FeedbackLevel.WAITING -> TextSecondary
}

/** 追踪状态文案。 */
internal fun trackingText(status: TrackingStatus, demo: Boolean): String = when {
    demo && status != TrackingStatus.UNSUPPORTED -> "演示模式:模拟手部轨迹"
    status == TrackingStatus.ACTIVE -> "手部追踪:正常"
    status == TrackingStatus.LOST -> "手部暂时离开视野,请把手抬回镜头前"
    status == TrackingStatus.WAITING -> "手部追踪:正在准备…"
    status == TrackingStatus.PERMISSION_REQUIRED -> "请在系统弹窗中允许「手部追踪」"
    status == TrackingStatus.NOT_IN_FULL_SPACE -> "请进入 Full Space 后使用"
    status == TrackingStatus.UNSUPPORTED -> "当前设备不支持手部追踪,可用演示模式"
    status == TrackingStatus.DEMO -> "演示模式:模拟手部轨迹"
    else -> "手部追踪:未就绪"
}
