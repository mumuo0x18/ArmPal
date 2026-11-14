package cn.armpal.rehab.anchor

import android.content.Context
import android.util.Log
import cn.armpal.rehab.data.SettingsStore
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.sense.world.WorldTrackingManager
import com.pico.spatial.sense.world.WorldTrackingResult
import com.pico.spatial.sense.world.WorldAnchor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * 训练位空间锚点(PRD §4.2 行为设计):
 * 首次在当前位置创建"训练位"并持久化;之后启动时找回 —— 找到即"到点到位",
 * 找不到提示"回到训练位或手动开始"。全程降级不阻断(开发文档 §5)。
 *
 * 约束(官方文档):仅 Full Space 可用;找回半径 ≤5 米;每应用锚点上限 1024。
 * 无自定义权限字符串(6.2.8 实测),错误经 WorldTrackingResult.Error 上报。
 */
object AnchorManager {

    private const val TAG = "AnchorManager"
    private const val ANCHOR_NAME = "armpal_training_spot"

    sealed interface AnchorState {
        /** 尚未尝试。 */
        data object Idle : AnchorState

        /** 正在创建/找回。 */
        data object Working : AnchorState

        /** 训练位已就绪(创建成功或找回成功)。 */
        data class Ready(val anchorId: String) : AnchorState

        /** 有过训练位,但本次没找回(不在 5 米内)→ 提示"回到训练位或手动开始"。 */
        data object NotFound : AnchorState

        /** 锚点能力错误(不支持/系统错误)。 */
        data class Failed(val message: String) : AnchorState
    }

    private val _state = MutableStateFlow<AnchorState>(AnchorState.Idle)
    val state: StateFlow<AnchorState> = _state.asStateFlow()

    /** 首次设置:在头显当前位置创建训练位(Stage 坐标,米)。 */
    suspend fun createTrainingSpot(context: Context, headWorld: Vector3) {
        _state.value = AnchorState.Working
        runCatching {
            WorldTrackingManager.createAnchor(
                position = Vector3(headWorld.x, headWorld.y, headWorld.z),
                rotation = EulerAngles(0f, 0f, 0f),
                name = ANCHOR_NAME,
            )
        }.fold(
            onSuccess = { result ->
                when (result) {
                    is WorldTrackingResult.Success -> {
                        val uuid = result.data?.anchorUUID
                        if (uuid == null) {
                            _state.value = AnchorState.Failed("创建返回空锚点")
                        } else {
                            SettingsStore.setAnchorId(context, uuid.toString())
                            _state.value = AnchorState.Ready(uuid.toString())
                            Log.i(TAG, "anchor created $uuid")
                        }
                    }

                    is WorldTrackingResult.Error -> {
                        _state.value = AnchorState.Failed(result.errorMessage ?: "创建失败")
                        Log.w(TAG, "create error: ${result.errorCode} ${result.errorMessage}")
                    }
                }
            },
            onFailure = {
                _state.value = AnchorState.Failed(it.message ?: "锚点调用异常")
                Log.e(TAG, "createAnchor failed", it)
            },
        )
    }

    /** 启动时找回训练位;无历史锚点直接 Ready(首次用户不阻断)。 */
    suspend fun restore(context: Context) {
        val uuidStr = runCatching { SettingsStore.anchorIdOnce(context) }.getOrNull()
        if (uuidStr.isNullOrBlank()) {
            _state.value = AnchorState.Idle
            return
        }
        _state.value = AnchorState.Working
        runCatching {
            WorldTrackingManager.loadAnchor(arrayOf(UUID.fromString(uuidStr)))
        }.fold(
            onSuccess = { result ->
                when (result) {
                    is WorldTrackingResult.Success -> {
                        val found: Array<WorldAnchor> = result.data ?: emptyArray()
                        if (found.isNotEmpty()) {
                            _state.value = AnchorState.Ready(uuidStr)
                            Log.i(TAG, "anchor restored ${found.size}")
                        } else {
                            _state.value = AnchorState.NotFound
                            Log.i(TAG, "anchor not found (out of range)")
                        }
                    }

                    is WorldTrackingResult.Error -> {
                        _state.value = AnchorState.Failed(result.errorMessage ?: "找回失败")
                        Log.w(TAG, "load error: ${result.errorCode} ${result.errorMessage}")
                    }
                }
            },
            onFailure = {
                _state.value = AnchorState.Failed(it.message ?: "锚点调用异常")
                Log.e(TAG, "loadAnchor failed", it)
            },
        )
    }

    /** 清除训练位(用户要求重新设置)。 */
    suspend fun clear(context: Context) {
        val uuidStr = runCatching { SettingsStore.anchorIdOnce(context) }.getOrNull() ?: return
        runCatching { WorldTrackingManager.removeAnchor(UUID.fromString(uuidStr)) }
        SettingsStore.setAnchorId(context, "")
        _state.value = AnchorState.Idle
    }
}
