package cn.armpal.rehab.analysis

import kotlin.math.abs

/**
 * 人体参考系:以头显位置为原点,给出估计的左右肩位置。
 */
data class BodyFrame(
    val head: Vec3,
    val shoulderR: Vec3,
    val shoulderL: Vec3,
)

/**
 * 肩部位置估计(头显 + 双手,无外设)。
 *
 * 真实约束:只有「头显位姿 + 双手腕」数据,肩部只能估计:
 * 1. 默认值:肩在头显下方约 20cm、左右各偏 18cm(成人坐姿经验值)
 * 2. 首次校准(可选):双臀侧平举时,用两侧腕高估计"肩高/臂长",肩宽保持解剖学先验
 *
 * 注意(2026-11-14 评审 P0-6):不能把两侧腕距的一半当作肩宽 —— 侧平举时腕在肩外侧,
 * 腕距 ≈ 两倍(肩半宽 + 上臂+前臂长),直接当肩宽会把肩估到手腕位置,角度量程被压缩一半。
 * 因此校准只更新肩高与臂长,肩宽使用 [DEFAULT_HALF_WIDTH](可被腕距做温和缩放)。
 *
 * 本类不再是全局单例:每个训练会话持有一个实例,避免跨会话/测试互相污染。
 */
class BodyFrameEstimator {

    /** 头显→肩的偏移(米,头显局部坐标系:x=右,y=上,z=前)。 */
    var shoulderOffsetR = Vec3(+DEFAULT_HALF_WIDTH, -0.20f, 0f)
        private set
    var shoulderOffsetL = Vec3(-DEFAULT_HALF_WIDTH, -0.20f, 0f)
        private set

    /** 估计的肩→腕臂长(米),DTW 归一化与仪表展示可复用。 */
    var armLength: Float = DEFAULT_ARM_LENGTH
        private set

    /** 是否已完成 T-pose 校准。 */
    var isCalibrated: Boolean = false
        private set

    /**
     * 用头显位置与朝向估计当前 BodyFrame。
     * 肩部偏移会绕世界 +Y 轴按头显 yaw 旋转,用户转身后左右肩位置仍正确。
     *
     * @param headPos   头显在世界系(Stage/global)位置
     * @param headYawRad 头显朝向的 yaw(弧度),来自 HMDPose.rotation.toEulerAngles().yaw
     */
    fun estimate(headPos: Vec3, headYawRad: Float = 0f): BodyFrame = BodyFrame(
        head = headPos,
        shoulderR = headPos + shoulderOffsetR.rotateY(headYawRad),
        shoulderL = headPos + shoulderOffsetL.rotateY(headYawRad),
    )

    /**
     * 首次校准:双臂侧平举时记录"肩高/臂长"。
     * 侧平举时腕高 ≈ 肩高;腕距用于温和缩放肩宽 (腕距 × 0.15,夹在 0.14~0.24m)。
     * 臂长 = 腕到肩的水平距离。
     *
     * @return true = 校准成功;false = 姿势/数据不合理(不修改已有状态)
     */
    fun calibrateFromTPose(
        headPos: Vec3,
        wristR: Vec3,
        wristL: Vec3,
        headYawRad: Float = 0f,
    ): Boolean {
        // 转到头显局部坐标系(消除用户朝向影响)
        val r = (wristR - headPos).rotateY(-headYawRad)
        val l = (wristL - headPos).rotateY(-headYawRad)
        if (!r.x.isFinite() || !l.x.isFinite()) return false
        val span = abs(r.x - l.x)
        if (span !in MIN_WRIST_SPAN..MAX_WRIST_SPAN) return false
        val midY = (r.y + l.y) / 2f
        val midZ = (r.z + l.z) / 2f
        if (midY !in -0.8f..0.4f) return false

        val halfWidth = (span * HALF_WIDTH_RATIO).coerceIn(MIN_HALF_WIDTH, MAX_HALF_WIDTH)
        armLength = (span / 2f - halfWidth).coerceIn(MIN_ARM_LENGTH, MAX_ARM_LENGTH)
        shoulderOffsetR = Vec3(+halfWidth, midY, midZ)
        shoulderOffsetL = Vec3(-halfWidth, midY, midZ)
        isCalibrated = true
        return true
    }

    /** 恢复默认(校准失败/用户要求重置)。 */
    fun resetToDefaults() {
        shoulderOffsetR = Vec3(+DEFAULT_HALF_WIDTH, -0.20f, 0f)
        shoulderOffsetL = Vec3(-DEFAULT_HALF_WIDTH, -0.20f, 0f)
        armLength = DEFAULT_ARM_LENGTH
        isCalibrated = false
    }

    companion object {
        /** 成人肩半宽先验(米)。 */
        const val DEFAULT_HALF_WIDTH = 0.18f
        const val DEFAULT_ARM_LENGTH = 0.60f

        private const val HALF_WIDTH_RATIO = 0.15f
        private const val MIN_HALF_WIDTH = 0.14f
        private const val MAX_HALF_WIDTH = 0.24f
        private const val MIN_ARM_LENGTH = 0.35f
        private const val MAX_ARM_LENGTH = 0.75f
        private const val MIN_WRIST_SPAN = 0.45f
        private const val MAX_WRIST_SPAN = 1.80f
    }
}
