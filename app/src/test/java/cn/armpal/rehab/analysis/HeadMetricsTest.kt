package cn.armpal.rehab.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadMetricsTest {

    @Test
    fun `正前方 yaw 为 0`() {
        assertEquals(0f, HeadMetrics.yawSwingDeg(0f), 1e-4f)
    }

    @Test
    fun `左右转都取幅度 绝对值`() {
        val right = Math.toRadians(40.0).toFloat()
        val left = Math.toRadians(-40.0).toFloat()
        assertEquals(40f, HeadMetrics.yawSwingDeg(right), 1e-3f)
        assertEquals(40f, HeadMetrics.yawSwingDeg(left), 1e-3f)
    }

    @Test
    fun `pitch 低头仰头都取幅度`() {
        val down = Math.toRadians(-25.0).toFloat()
        val up = Math.toRadians(25.0).toFloat()
        assertEquals(25f, HeadMetrics.pitchSwingDeg(down), 1e-3f)
        assertEquals(25f, HeadMetrics.pitchSwingDeg(up), 1e-3f)
    }

    @Test
    fun `非有限输入 回退 0 不崩溃`() {
        assertEquals(0f, HeadMetrics.yawSwingDeg(Float.NaN), 1e-4f)
        assertEquals(0f, HeadMetrics.pitchSwingDeg(Float.POSITIVE_INFINITY), 1e-4f)
    }

    @Test
    fun `幅度钳制在 0 到 180`() {
        assertEquals(180f, HeadMetrics.yawSwingDeg(Math.toRadians(190.0).toFloat()), 1e-3f)
    }

    @Test
    fun `颈部动作判定`() {
        assertTrue(HeadMetrics.isNeckExercise("HEAD_YAW"))
        assertTrue(HeadMetrics.isNeckExercise("head_pitch"))
        assertFalse(HeadMetrics.isNeckExercise("WRIST_R"))
    }
}
