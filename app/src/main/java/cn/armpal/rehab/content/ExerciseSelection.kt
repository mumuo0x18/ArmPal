package cn.armpal.rehab.content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.armpal.rehab.analysis.ExerciseTemplate

/**
 * 动作目录/选择的跨组合共享状态(窗口与 Stage 属不同组合树)。
 * HomeStage 装载目录并监听 currentId 变化执行 rebind;训练主窗渲染选择 chips。
 */
object ExerciseSelection {

    /** 全部可用动作(exercises.json)。 */
    var all: List<ExerciseTemplate> by mutableStateOf(emptyList())

    /** 当前选中动作 id(null = 未选择,回退到第一个)。 */
    var currentId: String? by mutableStateOf(null)

    /** 当前动作模板(目录为空时 null)。 */
    val current: ExerciseTemplate?
        get() = all.firstOrNull { it.id == currentId } ?: all.firstOrNull()
}
