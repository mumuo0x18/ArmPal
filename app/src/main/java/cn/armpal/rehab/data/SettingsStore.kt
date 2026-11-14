package cn.armpal.rehab.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "armpal_settings")

/**
 * 轻量设置/状态存储(DataStore Preferences):免责声明、动作选择、streak、校准、语音开关、锚点 id。
 * 结构化训练记录走 Room(TrainingRepository),二者分工按开发文档 §6。
 */
object SettingsStore {

    private object Keys {
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val SELECTED_EXERCISE_ID = stringPreferencesKey("selected_exercise_id")
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val LAST_TRAIN_DATE = stringPreferencesKey("last_train_date")
        val CALIBRATION_JSON = stringPreferencesKey("calibration_json")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val ANCHOR_ID = stringPreferencesKey("anchor_id")
    }

    fun disclaimerAccepted(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DISCLAIMER_ACCEPTED] ?: false }

    suspend fun setDisclaimerAccepted(context: Context, accepted: Boolean) {
        context.dataStore.edit { it[Keys.DISCLAIMER_ACCEPTED] = accepted }
    }

    fun selectedExerciseId(context: Context): Flow<String?> =
        context.dataStore.data.map { it[Keys.SELECTED_EXERCISE_ID] }

    suspend fun setSelectedExerciseId(context: Context, id: String) {
        context.dataStore.edit { it[Keys.SELECTED_EXERCISE_ID] = id }
    }

    fun voiceEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[Keys.VOICE_ENABLED] ?: true }

    suspend fun setVoiceEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_ENABLED] = enabled }
    }

    fun calibrationJson(context: Context): Flow<String?> =
        context.dataStore.data.map { it[Keys.CALIBRATION_JSON] }

    suspend fun setCalibrationJson(context: Context, json: String) {
        context.dataStore.edit { it[Keys.CALIBRATION_JSON] = json }
    }

    fun anchorId(context: Context): Flow<String?> =
        context.dataStore.data.map { it[Keys.ANCHOR_ID] }

    suspend fun setAnchorId(context: Context, id: String) {
        context.dataStore.edit { it[Keys.ANCHOR_ID] = id }
    }

    /** 读取当前 streak(单次)。 */
    suspend fun streak(context: Context): Int =
        context.dataStore.data.first()[Keys.STREAK_COUNT] ?: 0

    /**
     * 打卡:与上次训练日比较推进 streak(同日重复不涨,隔日 +1,断档清 1)。
     * 由调用方(锚点/手动开始)在每日首次训练完成时调用;date = yyyy-MM-dd。
     */
    suspend fun bumpStreak(context: Context, today: String, yesterday: String): Int {
        var result = 1
        context.dataStore.edit { p ->
            val last = p[Keys.LAST_TRAIN_DATE]
            val current = p[Keys.STREAK_COUNT] ?: 0
            result = when (last) {
                today -> current
                yesterday -> current + 1
                else -> 1
            }
            p[Keys.STREAK_COUNT] = result
            p[Keys.LAST_TRAIN_DATE] = today
        }
        return result
    }

    /** 读取 streak 用的最后训练日(供展示"上次训练")。 */
    suspend fun lastTrainDate(context: Context): String? =
        context.dataStore.data.first()[Keys.LAST_TRAIN_DATE]
}
