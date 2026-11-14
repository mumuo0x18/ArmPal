package cn.armpal.rehab.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrajectoryMatcherTest {

    @Test
    fun `完全一致 距离为0`() {
        val t = listOf(Vec3(0f, 0f, 0f), Vec3(0.1f, 0.2f, 0f), Vec3(0.2f, 0.4f, 0f))
        assertEquals(0f, TrajectoryMatcher.dtw(t, t), 1e-4f)
    }

    @Test
    fun `时间轴错位但形状相同 DTW 距离很小`() {
        val template = listOf(
            Vec3(0f, 0f, 0f), Vec3(0f, 0.3f, 0f), Vec3(0f, 0.6f, 0f),
        )
        val actual = listOf(
            Vec3(0f, 0f, 0f), Vec3(0f, 0.1f, 0f), Vec3(0f, 0.2f, 0f),
            Vec3(0f, 0.3f, 0f), Vec3(0f, 0.45f, 0f), Vec3(0f, 0.6f, 0f),
        )
        val d = TrajectoryMatcher.dtw(template, actual)
        assertTrue("dtw=$d", d < 0.05f)
    }

    @Test
    fun `完全跑偏 距离显著大`() {
        val template = listOf(
            Vec3(0f, 0f, 0f), Vec3(0f, 0.3f, 0f), Vec3(0f, 0.6f, 0f),
        )
        val actual = listOf(
            Vec3(1f, 0f, 1f), Vec3(1f, 0.3f, 1f), Vec3(1f, 0.6f, 1f),
        )
        val d = TrajectoryMatcher.dtw(template, actual)
        assertTrue("dtw=$d", d > 0.5f)
        assertEquals(0f, TrajectoryMatcher.qualityScore(d), 1e-4f)
    }

    @Test
    fun `空序列 返回最大值且不崩溃`() {
        val t = listOf(Vec3(0f, 0f, 0f))
        assertEquals(Float.MAX_VALUE, TrajectoryMatcher.dtw(t, emptyList()), 0f)
    }

    @Test
    fun `质量分单调递减`() {
        assertTrue(TrajectoryMatcher.qualityScore(0f) > TrajectoryMatcher.qualityScore(0.2f))
        assertTrue(TrajectoryMatcher.qualityScore(0.2f) > TrajectoryMatcher.qualityScore(0.4f))
    }

    @Test
    fun `非有限距离 质量为0`() {
        assertEquals(0f, TrajectoryMatcher.qualityScore(Float.POSITIVE_INFINITY), 0f)
        assertEquals(0f, TrajectoryMatcher.qualityScore(Float.NaN), 0f)
    }

    @Test
    fun `归一化到肩部原点与臂长尺度`() {
        val shoulder = Vec3(0.18f, 1.4f, 0f)
        val points = listOf(
            shoulder + Vec3(0f, -0.6f, 0f),
            shoulder + Vec3(0.6f, 0f, 0f),
        )
        val normalized = TrajectoryMatcher.normalize(points, shoulder, scale = 0.6f)
        assertEquals(0f, normalized[0].x, 1e-4f)
        assertEquals(-1f, normalized[0].y, 1e-4f)
        assertEquals(1f, normalized[1].x, 1e-4f)
        assertEquals(0f, normalized[1].y, 1e-4f)
    }
}
