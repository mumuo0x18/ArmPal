package cn.armpal.rehab.analysis

/**
 * 单次重复的动作质量评分(纯 Kotlin,可 JVM 单测)。
 *
 * 口径:模板点(绝对米制幅度)与实测腕点轨迹(世界坐标)都归一到"单位最大幅度"空间,
 * 比较形状而非绝对尺度;DTW 距离映射为 0..1(1 = 形状完全一致)。
 * 只用于报告页展示,不用于实时打断(开发文档 §4.4)。
 */
object RepQualityScorer {

    /** 一次重复的质量分;无模板或空轨迹返回 -1(未评分)。 */
    fun score(
        template: List<Vec3>,
        actualWorld: List<Vec3>,
        shoulder: Vec3,
        armLength: Float,
    ): Float {
        if (template.isEmpty() || actualWorld.isEmpty()) return -1f
        val actual = TrajectoryMatcher.normalize(actualWorld, shoulder, armLength)
        val templateScale = template.maxOf { it.length }.coerceAtLeast(1e-3f)
        val templateUnit = template.map { it * (1f / templateScale) }
        val dist = TrajectoryMatcher.dtw(templateUnit, actual)
        val q = TrajectoryMatcher.qualityScore(dist)
        return if (q.isFinite()) q else -1f
    }

    /** 多次重复的平均分;-1 = 全部未评分。 */
    fun average(scores: List<Float>): Float {
        val valid = scores.filter { it in 0f..1f }
        return if (valid.isEmpty()) -1f else valid.sum() / valid.size
    }
}
