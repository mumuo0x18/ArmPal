package cn.armpal.rehab.analysis

import kotlin.math.abs

/**
 * 人体参考系:以头显位置为原点消除身体平移后的肩/颈估计位置。
 */
data class BodyFrame(
    val head: Vec3,
    val shoulderR: Vec3,
    val shoulderL: Vec3,
)

/**
 * 肩/颈位置估计。
 *
 * 真实约束:只有「头显 + 双手」三点数据(不依赖 Motion Tracker 外设),肩部只能估计:
 * 1. 默认值:经验偏移(肩在头显下方约 20cm,左右各偏 18cm)
 * 2. 首次校准(完整版):用户双臂侧平举,由双手腕位置反推肩宽/臂长,覆盖默认偏移
 *
 * 该估计决定角度的可信度上限,输出定位为"活动度参考",不做医疗精度宣称(PRD §5)。
 */
object BodyFrameEstimator {

    /** 头显→肩的默认偏移(米),坐姿、头显水平佩戴假设。 */
    var shoulderOffsetR = Vec3(+0.18f, -0.20f, 0f)
        private set
    var shoulderOffsetL = Vec3(-0.18f, -0.20f, 0f)
        private set

    /** 校准结果(米),null = 未校准。 */
    var shoulderWidth: Float? = null
        private set

    /**
     * 用头显位置估计当前 BodyFrame。
     * headPos 应为头显在世界系(如 Stage)中的位置。
     */
    fun estimate(headPos: Vec3): BodyFrame = BodyFrame(
        head = headPos,
        shoulderR = headPos + shoulderOffsetR,
        shoulderL = headPos + shoulderOffsetL,
    )

    /**
     * 首次校准:双臂侧平举姿势下,由左右手腕世界位置估计肩位与肩宽。
     * 假设此时腕 ≈ 肩的侧向延长线,肩心取两侧腕的中点,Y 取头显高度下方固定比例。
     */
    fun calibrateFromTPose(headPos: Vec3, wristR: Vec3, wristL: Vec3) {
        val mid = Vec3(
            (wristR.x + wristL.x) / 2f,
            (wristR.y + wristL.y) / 2f,
            (wristR.z + wristL.z) / 2f,
        )
        val width = abs(wristR.x - wristL.x).coerceAtLeast(0.24f)
        shoulderWidth = width
        val half = width / 2f
        shoulderOffsetR = Vec3(+half, mid.y - headPos.y, mid.z - headPos.z)
        shoulderOffsetL = Vec3(-half, mid.y - headPos.y, mid.z - headPos.z)
    }

    /** 恢复默认(校准失败/用户要求重置)。 */
    fun resetToDefaults() {
        shoulderOffsetR = Vec3(+0.18f, -0.20f, 0f)
        shoulderOffsetL = Vec3(-0.18f, -0.20f, 0f)
        shoulderWidth = null
    }
}
