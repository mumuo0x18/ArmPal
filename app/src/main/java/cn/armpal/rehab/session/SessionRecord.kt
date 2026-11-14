package cn.armpal.rehab.session

/**
 * 一次训练会话的持久化记录(纯数据,可 JVM 单测)。
 * 由 TrainingSessionController 在会话完整结束(或退出时有有效数据)时产出,
 * 由 data 层转换为 Room 实体落库。
 */
data class SessionRecord(
    val exerciseId: String,
    val exerciseName: String,
    /** 全场累计完成次数(跨组)。 */
    val reps: Int,
    val targetReps: Int,
    val maxElevationDeg: Float,
    /** 平均动作质量 0..1(-1 = 无模板未评分)。 */
    val avgQuality: Float,
    /** 平均左右对称差(度,-1 = 单臂动作无数据)。 */
    val symmetryDeg: Float,
    val durationSec: Int,
    val setsDone: Int,
    val demoMode: Boolean,
    val createdAtMs: Long,
)
