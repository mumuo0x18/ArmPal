package cn.armpal.rehab.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BodyFrameEstimatorTest {

    @Test
    fun `默认估计 肩在头显下方与两侧`() {
        val f = BodyFrameEstimator.estimate(Vec3(0f, 1.6f, 0f))
        assertEquals(0.18f, f.shoulderR.x, 1e-4f)
        assertEquals(1.4f, f.shoulderR.y, 1e-4f)
        assertEquals(-0.18f, f.shoulderL.x, 1e-4f)
    }

    @Test
    fun `TPose 校准覆盖偏移并记录肩宽`() {
        assertNull(BodyFrameEstimator.shoulderWidth)
        BodyFrameEstimator.calibrateFromTPose(
            headPos = Vec3(0f, 1.6f, 0f),
            wristR = Vec3(0.60f, 1.35f, 0f),
            wristL = Vec3(-0.60f, 1.35f, 0f),
        )
        assertEquals(1.20f, BodyFrameEstimator.shoulderWidth!!, 1e-4f)
        val f = BodyFrameEstimator.estimate(Vec3(0f, 1.6f, 0f))
        assertEquals(0.60f, f.shoulderR.x, 1e-3f)
        assertEquals(1.35f, f.shoulderR.y, 1e-3f)
        BodyFrameEstimator.resetToDefaults()
        assertNull(BodyFrameEstimator.shoulderWidth)
    }

    @Test
    fun `头显平移 肩位跟随 不产生角度漂移`() {
        BodyFrameEstimator.resetToDefaults()
        val f1 = BodyFrameEstimator.estimate(Vec3(0f, 1.6f, 0f))
        val f2 = BodyFrameEstimator.estimate(Vec3(1f, 1.6f, 0f))
        // 腕相对身体同样侧平举 → 平移不应改变抬举角
        val wrist1 = f1.shoulderR + Vec3(0.6f, 0f, 0f)
        val wrist2 = f2.shoulderR + Vec3(0.6f, 0f, 0f)
        assertEquals(
            AngleMetrics.elevationDeg(wrist1, f1.shoulderR),
            AngleMetrics.elevationDeg(wrist2, f2.shoulderR),
            1e-3f,
        )
    }
}
