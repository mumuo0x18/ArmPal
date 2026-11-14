package cn.armpal.rehab.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 训练记录仓库:Room 落库 + 报告窗消费的状态流。
 *
 * 单例 + init(context)(Application.onCreate 调用);窗口/控制器不直接碰 DAO。
 * 所有写操作 fire-and-forget 在 IO 线程,失败只记日志不阻断训练(准入红线优先)。
 */
object TrainingRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var dao: SessionDao? = null

    /** 近 50 条记录(新→旧),报告窗订阅。 */
    private val _recent = MutableStateFlow<List<TrainingSessionEntity>>(emptyList())
    val recent: StateFlow<List<TrainingSessionEntity>> = _recent.asStateFlow()

    fun init(context: Context) {
        if (dao != null) return
        val db = Room.databaseBuilder(
            context.applicationContext,
            ArmPalDatabase::class.java,
            ArmPalDatabase.NAME,
        ).fallbackToDestructiveMigration(dropAllTables = false).build()
        dao = db.sessionDao()
        scope.launch { refresh() }
    }

    /** 落一条会话记录并刷新状态流。 */
    fun record(session: TrainingSessionEntity) {
        val d = dao ?: return
        scope.launch {
            runCatching { d.insert(session) }
                .onFailure { android.util.Log.e(TAG, "insert failed", it) }
            refresh()
        }
    }

    suspend fun sessionsSince(sinceDate: String): List<TrainingSessionEntity> =
        dao?.sessionsSince(sinceDate) ?: emptyList()

    suspend fun countOnDate(date: String): Int = dao?.countOnDate(date) ?: 0

    private suspend fun refresh() {
        val d = dao ?: return
        runCatching { _recent.value = d.queryRecentOnce() }
            .onFailure { android.util.Log.e(TAG, "refresh failed", it) }
    }

    private const val TAG = "TrainingRepository"
}
