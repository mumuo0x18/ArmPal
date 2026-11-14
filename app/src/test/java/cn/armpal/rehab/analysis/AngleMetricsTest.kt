package cn.armpal.rehab.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AngleMetricsTest {

    private val frame = BodyFrame(
        head = Vec3(0f, 1.6f, 0f),
        shoulderR = Vec3(0.18f, 1.4f, 0f),
        shoulderL = Vec3(-0.18f, 1.4f, 0f),
    )

    @Test
    fun `手臂自然下垂 抬举角约180度`() {
        val wrist = Vec3(0.18f, 0.4f, 0f) // 正下方
        val deg = AngleMetrics.elevationDeg(wrist, frame.shoulderR)
        assertEquals(180f, deg, 1.5f)
    }

    @Test
    fun `侧平举 抬举角约90度`() {
        val wrist = Vec3(0.78f, 1.4f, 0f) // 正右侧
        val deg = AngleMetrics.elevationDeg(wrist, frame.shoulderR)
        assertEquals(90f, deg, 1.5f)
    }

    @Test
    fun `过头 抬举角约0度`() {
        val wrist = Vec3(0.18f, 2.0f, 0f) // 正上方
        val deg = AngleMetrics.elevationDeg(wrist, frame.shoulderR)
        assertEquals(0f, deg, 1.5f)
    }

    @Test
    fun `对称性 两手同高为0`() {
        val rWrist = Vec3(0.78f, 1.4f, 0f)
        val lWrist = Vec3(-0.78f, 1.4f, 0f)
        assertEquals(0f, AngleMetrics.symmetryDeg(rWrist, lWrist, frame), 2f)
    }

    @Test
    fun `对称性 一侧高一侧低 有差值`() {
        val rWrist = Vec3(0.78f, 1.4f, 0f)  // 90°
        val lWrist = Vec3(-0.38f, 0.9f, 0f) // 更低
        val diff = AngleMetrics.symmetryDeg(rWrist, lWrist, frame)
        assertTrue(diff > 20f)
    }
}
