package cn.armpal.rehab.analysis

import kotlin.math.abs

/**
 * 颈部活动度(纯 Kotlin,可 JVM 单测):由 HMD 欧拉角推算转头/点头幅度。
 *
 * 口径(幅度制,符号无关):
 * - yaw 幅度 = 相对"正前方"的转角绝对值(0° = 正前,90° = 完全侧向)
 * - pitch 幅度 = 相对水平的俯仰绝对值(0° = 平视,正值仰头/负值低头,均取幅度)
 *
 * 输出作为训练计数器的"活动角",与抬举角共用 RepCounter 状态机。
 * 诚实标注:头部活动由头显姿态推算,仅作活动度参考,非医疗级测量。
 */
object HeadMetrics {

    /** 左右转头幅度(度)。 */
    fun yawSwingDeg(headYawRad: Float): Float = swingDeg(headYawRad)

    /** 点头/仰头幅度(度)。 */
    fun pitchSwingDeg(headPitchRad: Float): Float = swingDeg(headPitchRad)

    /** 是否颈部动作(targetJoint 约定:HEAD_ 前缀)。 */
    fun isNeckExercise(targetJoint: String): Boolean =
        targetJoint.uppercase().startsWith("HEAD")

    private fun swingDeg(rad: Float): Float {
        if (!rad.isFinite()) return 0f
        val deg = Math.toDegrees(rad.toDouble()).toFloat()
        return abs(deg).coerceIn(0f, 180f)
    }
}
