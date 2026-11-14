package cn.armpal.rehab.analysis

import kotlinx.serialization.Serializable

/**
 * 动作模板(exercises.json 反序列化目标),开源可扩展的数据契约。
 *
 * 新增动作 = 在 assets/exercises.json 追加一条;格式见 README「如何新增一个动作模板」。
 */
@Serializable
data class ExerciseTemplate(
    val id: String,
    val name: String,
    /** 作用侧与关节,如 "WRIST_R"(右手)/ "WRIST_L"(左手)/ "HEAD"(颈部) */
    val targetJoint: String,
    /** 达标抬举角(度;0°=垂下,90°=侧平举) */
    val targetElevationDeg: Float,
    /** 目标次数 */
    val reps: Int,
    /** 顶点保持时长(毫秒) */
    val holdMs: Long,
    /** 组数 */
    val sets: Int = 1,
    /** 组间休息(秒) */
    val restSec: Int = 0,
    /** 标准轨迹点(归一化到单位空间,只作形状参考) */
    val templateTrajectory: List<List<Float>> = emptyList(),
    /** 适老提示文案(可选) */
    val description: String = "",
)
