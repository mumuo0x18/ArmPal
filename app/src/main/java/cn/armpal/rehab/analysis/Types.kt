package cn.armpal.rehab.analysis

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 纯 Kotlin 三维向量,供 analysis 包使用(无 SDK 依赖,保证算法可 JVM 单测)。
 * 与 Spatial SDK 的 com.pico.spatial.core.math.Vector3 之间的转换由 tracking 层负责。
 */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)

    val length: Float get() = sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val len = length
        return if (len < 1e-6f) Vec3(0f, 0f, 0f) else Vec3(x / len, y / len, z / len)
    }

    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z

    fun distance(o: Vec3): Float = (this - o).length

    /**
     * 绕世界 +Y 轴旋转(右手系,正角 = 向左转头)。
     * 用于让"肩部偏移"跟随头显朝向旋转,而不是永远固定在 +X 方向。
     */
    fun rotateY(radians: Float): Vec3 {
        val c = cos(radians)
        val s = sin(radians)
        return Vec3(x * c + z * s, y, -x * s + z * c)
    }

    companion object {
        val UP = Vec3(0f, 1f, 0f)

        /** 躯干"向下"轴:抬举角 0° 的参考方向。 */
        val DOWN = Vec3(0f, -1f, 0f)
    }
}
