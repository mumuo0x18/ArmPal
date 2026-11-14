package cn.armpal.rehab.session

import cn.armpal.rehab.analysis.ExerciseTemplate
import cn.armpal.rehab.analysis.RepPhase
import cn.armpal.rehab.tracking.TrackingStatus

/** 实时反馈等级(UI 颜色/文案的单一来源)。 */
enum class FeedbackLevel {
    /** 还没有可用数据 / 未开始。 */
    WAITING,

    /** 抬得不够高(黄色)。 */
    TOO_LOW,

    /** 接近目标(黄色)。 */
    PARTIAL,

    /** 达标(绿色)。 */
    GOOD,

    /** 正在顶点保持(绿色高亮)。 */
    HOLDING,
}

/**
 * 三窗共享的训练状态(单一状态中枢)。
 *
 * W1 的全局 HandTrackingStore 已被本结构取代:窗口不再各自读追踪原始数据,
 * 而是订阅同一份"已分析"状态(开发文档 §1 的单一状态中枢约束)。
 */
data class SessionState(
    val exercise: ExerciseTemplate? = null,
    /** 追踪健康状态。 */
    val tracking: TrackingStatus = TrackingStatus.WAITING,
    /** 追踪是否已丢失(曾有过数据,超过 500ms 未更新)。 */
    val trackingLost: Boolean = false,
    /** 当前是否演示模式(合成轨迹)。 */
    val demoMode: Boolean = false,
    /** 右手抬举角(度,null = 当前无有效数据)。 */
    val elevationR: Float? = null,
    /** 左手抬举角(度)。 */
    val elevationL: Float? = null,
    /** 目标抬举角(度)。 */
    val targetDeg: Float = 80f,
    /** 已完成次数。 */
    val repsDone: Int = 0,
    /** 目标次数。 */
    val targetReps: Int = 12,
    /** 当前组(0 基)。 */
    val setIndex: Int = 0,
    /** 总组数。 */
    val totalSets: Int = 1,
    /** 本次训练最高抬举角。 */
    val peakDeg: Float = 0f,
    /** 实时反馈等级。 */
    val feedback: FeedbackLevel = FeedbackLevel.WAITING,
    /** 计数状态机阶段(便于侧窗显示"保持中")。 */
    val phase: RepPhase = RepPhase.IDLE,
    /** 会话开始时间(单调时钟,毫秒)。 */
    val sessionStartedAtMs: Long = 0L,
    /** 最近一次完成提示。 */
    val lastMessage: String = "",
) {
    /** 进度 0..1(按目标次数)。 */
    val progress: Float
        get() = if (targetReps <= 0) 0f else (repsDone.toFloat() / targetReps).coerceIn(0f, 1f)

    /** 目标是否已达成。 */
    val targetReached: Boolean get() = targetReps > 0 && repsDone >= targetReps
}
