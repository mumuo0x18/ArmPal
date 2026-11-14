package cn.armpal.rehab.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 肩部估计:默认偏移、头显 yaw 旋转、T-pose 校准(2026-11-14 评审 P0-6)。
 *
 * 关键约束:校准不能把"腕距的一半"当肩宽 —— 那会把肩拉到手腕位置、压缩角度量程。
 */
class BodyFrameEstimatorTest {

    @Test
    fun `默认估计 肩在头显下方与两侧`() {
        val estimator = BodyFrameEstimator()
        val f = estimator.estimate(Vec3(0f, 1.6f, 0f))
        assertEquals(0.18f, f.shoulderR.x, 1e-4f)
        assertEquals(1.4f, f.shoulderR.y, 1e-4f)
        assertEquals(-0.18f, f.shoulderL.x, 1e-4f)
        assertFalse(estimator.isCalibrated)
    }

    @Test
    fun `头显转向后 肩部偏移跟随 yaw 旋转`() {
        val estimator = BodyFrameEstimator()
        // 右转 90°(绕 +Y 正向),右肩应从 +X 转到 -Z 方向
        val f = estimator.estimate(Vec3(0f, 1.6f, 0f), headYawRad = (Math.PI / 2).toFloat())
        assertEquals(0f, f.shoulderR.x, 1e-3f)
        assertEquals(-0.18f, f.shoulderR.z, 1e-3f)
    }

    @Test
    fun `TPose 校准 肩宽保持解剖学先验并估计臂长`() {
        val estimator = BodyFrameEstimator()
        val ok = estimator.calibrateFromTPose(
            headPos = Vec3(0f, 1.6f, 0f),
            wristR = Vec3(0.80f, 1.40f, 0f),
            wristL = Vec3(-0.80f, 1.40f, 0f),
        )
        assertTrue(ok)
        assertTrue(estimator.isCalibrated)
        val f = estimator.estimate(Vec3(0f, 1.6f, 0f))
        // 肩宽被夹在 0.14~0.24m 的半宽范围,而不是腕距一半(0.80m)
        assertEquals(0.24f, f.shoulderR.x, 1e-3f)
        assertEquals(1.40f, f.shoulderR.y, 1e-3f)
        assertEquals(0.56f, estimator.armLength, 1e-3f)

        // 校准后"手臂自然下垂"仍应读到 0°,不能被压缩成中间角度
        val wristDown = f.shoulderR + Vec3(0f, -0.56f, 0f)
        assertEquals(0f, AngleMetrics.elevationDeg(wristDown, f.shoulderR), 0.5f)
        // 侧平举仍应读到 90°
        val wristLateral = f.shoulderR + Vec3(0.56f, 0f, 0f)
        assertEquals(90f, AngleMetrics.elevationDeg(wristLateral, f.shoulderR), 0.5f)
    }

    @Test
    fun `不合理姿势 校准失败且不污染状态`() {
        val estimator = BodyFrameEstimator()
        // 两手几乎重合,腕距过小 → 拒绝
        val ok = estimator.calibrateFromTPose(
            headPos = Vec3(0f, 1.6f, 0f),
            wristR = Vec3(0.1f, 1.4f, 0f),
            wristL = Vec3(-0.1f, 1.4f, 0f),
        )
        assertFalse(ok)
        assertFalse(estimator.isCalibrated)
        assertEquals(0.18f, estimator.shoulderOffsetR.x, 1e-4f)
    }

    @Test
    fun `校准后 reset 恢复默认`() {
        val estimator = BodyFrameEstimator()
        estimator.calibrateFromTPose(
            headPos = Vec3(0f, 1.6f, 0f),
            wristR = Vec3(0.75f, 1.4f, 0f),
            wristL = Vec3(-0.75f, 1.4f, 0f),
        )
        estimator.resetToDefaults()
        assertFalse(estimator.isCalibrated)
        assertEquals(BodyFrameEstimator.DEFAULT_ARM_LENGTH, estimator.armLength, 1e-4f)
        assertEquals(0.18f, estimator.estimate(Vec3(0f, 1.6f, 0f)).shoulderR.x, 1e-4f)
    }

    @Test
    fun `头显平移 肩位跟随 不产生角度漂移`() {
        val estimator = BodyFrameEstimator()
        val f1 = estimator.estimate(Vec3(0f, 1.6f, 0f))
        val f2 = estimator.estimate(Vec3(1f, 1.6f, 0f))
        val wrist1 = f1.shoulderR + Vec3(0.6f, 0f, 0f)
        val wrist2 = f2.shoulderR + Vec3(0.6f, 0f, 0f)
        assertEquals(
            AngleMetrics.elevationDeg(wrist1, f1.shoulderR),
            AngleMetrics.elevationDeg(wrist2, f2.shoulderR),
            1e-3f,
        )
    }
}
