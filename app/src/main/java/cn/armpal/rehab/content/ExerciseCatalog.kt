package cn.armpal.rehab.content

import android.content.Context
import cn.armpal.rehab.analysis.ExerciseTemplate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * exercises.json 的外层结构。
 * 原来只有单动作的 [ExerciseTemplate],与 JSON 顶层 { version, exercises } 不匹配,
 * 直接 decode 会失败;这里补上目录包装(2026-11-14 评审 P1)。
 */
@Serializable
data class ExerciseCatalog(
    val version: Int = 1,
    val exercises: List<ExerciseTemplate> = emptyList(),
)

/**
 * 动作模板库加载器(assets/exercises.json)。
 *
 * 读取失败时返回空列表而不是崩溃:降级链路要求"动作库坏了也能进示范模式"。
 */
object ExerciseRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** 测试/内部使用的纯文本解码入口。 */
    fun decode(raw: String): List<ExerciseTemplate> =
        json.decodeFromString<ExerciseCatalog>(raw).exercises

    /** 从 assets 加载全部动作模板。 */
    fun load(context: Context): List<ExerciseTemplate> = runCatching {
        context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
            decode(reader.readText())
        }
    }.getOrDefault(emptyList())

    private const val ASSET_NAME = "exercises.json"
}
