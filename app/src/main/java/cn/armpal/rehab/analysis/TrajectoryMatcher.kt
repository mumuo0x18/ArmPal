package cn.armpal.rehab.analysis

/**
 * 轨迹相似度(DTW):标准动作轨迹模板与用户轨迹对比,得出"动作质量"分数。
 *
 * 设计约束(开发文档 §4.4):DTW 分数只用于报告页的动作质量展示,
 * 不用于实时打断 —— 实时纠正由角度阈值反馈承担。
 */
object TrajectoryMatcher {

    /**
     * 归一化 DTW 距离:越小越相似。
     * template/actual 为等维向量序列(轨迹点)。
     */
    fun dtw(template: List<Vec3>, actual: List<Vec3>): Float {
        if (template.isEmpty() || actual.isEmpty()) return Float.MAX_VALUE
        val n = template.size
        val m = actual.size
        // 滚动数组,避免 O(n*m) 内存
        var prev = FloatArray(m + 1) { Float.MAX_VALUE }
        var curr = FloatArray(m + 1) { Float.MAX_VALUE }
        prev[0] = 0f
        for (i in 1..n) {
            curr[0] = Float.MAX_VALUE
            for (j in 1..m) {
                val cost = template[i - 1].distance(actual[j - 1])
                curr[j] = cost + minOf(prev[j], curr[j - 1], prev[j - 1])
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[m] / (n + m)
    }

    /** 把 DTW 距离映射到 0..1 的质量分:1 = 完全一致。maxDist 为"完全跑偏"的距离。 */
    fun qualityScore(normalizedDist: Float, maxDist: Float = 0.5f): Float {
        if (normalizedDist >= maxDist) return 0f
        return (1f - normalizedDist / maxDist).coerceIn(0f, 1f)
    }
}
