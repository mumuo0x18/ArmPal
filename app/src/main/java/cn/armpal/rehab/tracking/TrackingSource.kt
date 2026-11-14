package cn.armpal.rehab.tracking

import cn.armpal.rehab.analysis.Vec3

/**
 * 追踪健康状态(面向 UI 的粗粒度状态,不暴露 SDK 枚举)。
 */
enum class TrackingStatus {
    /** Provider 已启动,但还没有可用帧。 */
    WAITING,

    /** 收到有效的手部 + 头显数据。 */
    ACTIVE,

    /** 曾经有数据,但已超过丢失阈值没有更新(遮挡/手放下)。 */
    LOST,

    /** 支持手部追踪,但用户尚未授权。 */
    PERMISSION_REQUIRED,

    /** 当前不在 Full Space(Stage)。 */
    NOT_IN_FULL_SPACE,

    /** 设备/系统不支持手部追踪。 */
    UNSUPPORTED,

    /** 调试注入的模拟轨迹(无真机演示模式)。 */
    DEMO,
}

/**
 * 一帧追踪快照(已转换为 analysis 包使用的 Vec3 / 米制世界坐标)。
 *
 * 这是 tracking 层与 analysis/session 层之间的数据契约:
 * session 层只消费本结构,不直接依赖 PICO SDK,便于 JVM 单测与 Debug 注入。
 */
data class TrackingFrame(
    val timestampMs: Long,
    val status: TrackingStatus,
    /** 头显在世界系中的位置(米)。 */
    val headWorld: Vec3? = null,
    /** 头显朝向 yaw(弧度),用于旋转肩部偏移。 */
    val headYawRad: Float = 0f,
    /** 右手腕世界坐标(米),null = 当前帧无有效数据。 */
    val wristRWorld: Vec3? = null,
    /** 左手腕世界坐标(米)。 */
    val wristLWorld: Vec3? = null,
) {
    /** 是否至少有一只手的数据。 */
    val hasHands: Boolean get() = wristRWorld != null || wristLWorld != null
}

/**
 * 追踪数据源抽象。
 *
 * 实现:
 * - [PicoTrackingSource]:真实 PICO 手部 + HMD Provider
 * - [DemoTrackingSource]:按脚本注入的模拟轨迹(模拟器/无真机演示、算法联调)
 */
interface TrackingSource {
    fun start()
    fun stop()

    /** 取当前帧;由会话循环每 ~33ms 调用一次,实现应为非阻塞。 */
    fun currentFrame(timestampMs: Long): TrackingFrame

    /** true = 调试注入数据源(UI 会显式标注"演示模式")。 */
    val isDemo: Boolean get() = false
}
