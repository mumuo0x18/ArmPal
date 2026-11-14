package cn.armpal.rehab.analysis

import kotlin.math.acos
import kotlin.math.abs

/**
 * 角度指标:抬举角与左右对称性。
 *
 * 参考系约定(与 BodyFrameEstimator 一致):
 * - 躯干竖直轴取世界 +Y(坐姿近似,不随头显俯仰旋转 —— 见 README 技术验证记录)
 * - 抬举角定义:肩→腕向量与躯干竖直轴的夹角。0° = 手臂自然下垂,90° = 侧平举,180° = 过头
 *
 * 诚实标注:肩部位置是估计值(头显位置 + 校准偏移),角度输出仅作活动度参考,非医疗级测量。
 */
object AngleMetrics {

    /** 抬举角(度)。shoulder 与 wrist 在同一世界坐标系(Stage 或头显原点均可,只要一致)。 */
    fun elevationDeg(wrist: Vec3, shoulder: Vec3): Float {
        val upperArm = (wrist - shoulder).normalized()
        val cos = upperArm.dot(Vec3.UP).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cos.toDouble())).toFloat()
    }

    /** 左右抬举角差值(度),用于"患侧 vs 健侧"对称性参考。 */
    fun symmetryDeg(
        rightWrist: Vec3, leftWrist: Vec3,
        frame: BodyFrame,
    ): Float {
        val r = elevationDeg(rightWrist, frame.shoulderR)
        val l = elevationDeg(leftWrist, frame.shoulderL)
        return abs(r - l)
    }
}
