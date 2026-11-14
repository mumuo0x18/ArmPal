package cn.armpal.rehab.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * 一次训练会话(一组或多组完整记录)。仅本地存储,不上传(PRD §5 隐私边界)。
 */
@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 本地日期 yyyy-MM-dd(记录时区 = 设备时区)。 */
    val date: String,
    val exerciseId: String,
    val exerciseName: String,
    /** 完成次数。 */
    val reps: Int,
    val targetReps: Int,
    /** 最高抬举角(度)。 */
    val maxElevationDeg: Float,
    /** 平均动作质量 0..1(-1 = 无模板未评分)。 */
    val avgQuality: Float,
    /** 平均左右对称差(度)。 */
    val symmetryDeg: Float,
    val durationSec: Int,
    val setsDone: Int,
    /** 是否演示模式产生的数据(报告统计默认剔除)。 */
    val demoMode: Boolean,
    val createdAtMs: Long,
)

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: TrainingSessionEntity): Long

    @Query("SELECT * FROM training_sessions ORDER BY createdAtMs DESC LIMIT 50")
    fun observeRecent(): Flow<List<TrainingSessionEntity>>

    /** 一次性查询(仓库刷新用,不常驻收集)。 */
    @Query("SELECT * FROM training_sessions ORDER BY createdAtMs DESC LIMIT 50")
    suspend fun queryRecentOnce(): List<TrainingSessionEntity>

    @Query("SELECT * FROM training_sessions WHERE date = :date ORDER BY createdAtMs DESC")
    fun observeByDate(date: String): Flow<List<TrainingSessionEntity>>

    /** 近 N 天记录(含当天),供周聚合;date 为 yyyy-MM-dd 字典序比较。 */
    @Query("SELECT * FROM training_sessions WHERE date >= :sinceDate ORDER BY createdAtMs ASC")
    suspend fun sessionsSince(sinceDate: String): List<TrainingSessionEntity>

    @Query("SELECT COUNT(*) FROM training_sessions WHERE date = :date")
    suspend fun countOnDate(date: String): Int
}

@Database(
    entities = [TrainingSessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ArmPalDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        const val NAME = "armpal.db"
    }
}
