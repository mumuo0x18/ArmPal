package cn.armpal.rehab.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepQualityScorerTest {

    /** 前平面抬举轨迹(0°→peak°),离散采样;shoulderRelative=false 时返回世界坐标。 */
    private fun raiseTrajectory(shoulder: Vec3, peakDeg: Float, armLen: Float, steps: Int = 20, shoulderRelative: Boolean = false): List<Vec3> {
        return (0..steps).map { i ->
            val e = (peakDeg * i / steps) * Math.PI.toFloat() / 180f
            val rel = Vec3(kotlin.math.sin(e) * armLen, -kotlin.math.cos(e) * armLen, 0f)
            if (shoulderRelative) rel else shoulder + rel
        }
    }

    private val shoulder = Vec3(0.18f, 1.3f, 0f)
    private val armLen = 0.55f
    /** 模板语义:肩相对偏移(与 exercises.json 的 templateTrajectory 一致)。 */
    private val template = raiseTrajectory(shoulder, 85f, armLen, shoulderRelative = true)

    @Test
    fun `形状完全一致的轨迹得高分`() {
        val actual = raiseTrajectory(shoulder, 85f, armLen, steps = 40) // 时间轴更密
        val q = RepQualityScorer.score(template, actual, shoulder, armLen)
        assertTrue("q=$q", q > 0.8f)
    }

    @Test
    fun `明显跑偏的轨迹得低分`() {
        // 抬到一半就横着飘走
        val off = (0..20).map { i ->
            val t = i / 20f
            Vec3(shoulder.x + t * 0.5f, shoulder.y + t * 0.1f, shoulder.z + 0.3f * t)
        }
        val q = RepQualityScorer.score(template, off, shoulder, armLen)
        assertTrue("q=$q", q < 0.4f)
    }

    @Test
    fun `时间轴错位但形状相同 得高分`() {
        val actual = raiseTrajectory(shoulder, 85f, armLen, steps = 8)
        val q = RepQualityScorer.score(template, actual, shoulder, armLen)
        assertTrue("q=$q", q > 0.6f)
    }

    @Test
    fun `空模板或空轨迹 返回未评分`() {
        assertEquals(-1f, RepQualityScorer.score(emptyList(), template, shoulder, armLen), 1e-4f)
        assertEquals(-1f, RepQualityScorer.score(template, emptyList(), shoulder, armLen), 1e-4f)
    }

    @Test
    fun `平均分过滤未评分样本`() {
        val avg = RepQualityScorer.average(listOf(0.8f, -1f, 0.6f, -1f))
        assertEquals(0.7f, avg, 1e-4f)
        assertEquals(-1f, RepQualityScorer.average(listOf(-1f, -1f)), 1e-4f)
    }
}
