package cn.armpal.rehab.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 周报聚合(纯 Kotlin,可 JVM 单测):streak / 周合计 / 活跃天数。
 *
 * 口径:
 * - 一次"活跃日" = 当天至少 1 条非演示记录
 * - streak = 从今天(或昨天——今天还没练不算断)往前连续活跃的天数
 */
object WeekAggregator {

    data class WeeklyReport(
        val weekReps: Int,
        val activeDays: Int,
        val streakDays: Int,
        val avgQuality: Float,   // -1 = 无有效样本
        val maxElevationDeg: Float,
        val totalMinutes: Int,
    )

    /**
     * @param sessions 近 7 天记录(任意顺序)
     * @param today    今天(注入以便单测)
     */
    fun aggregate(
        sessions: List<TrainingSessionEntity>,
        today: LocalDate = LocalDate.now(),
    ): WeeklyReport {
        val real = sessions.filter { !it.demoMode }
        if (real.isEmpty()) {
            return WeeklyReport(0, 0, 0, -1f, 0f, 0)
        }

        val byDate = real.groupBy { it.date }
        val activeDays = byDate.size

        // streak:从今天或昨天开始往前数连续活跃日
        var streak = 0
        var cursor = if (byDate.containsKey(today.format(ISO))) today else today.minusDays(1)
        while (byDate.containsKey(cursor.format(ISO))) {
            streak++
            cursor = cursor.minusDays(1)
        }

        val qualities = real.map { it.avgQuality }.filter { it in 0f..1f }
        val avgQuality = if (qualities.isEmpty()) -1f
        else qualities.sum() / qualities.size

        return WeeklyReport(
            weekReps = real.sumOf { it.reps },
            activeDays = activeDays,
            streakDays = streak,
            avgQuality = avgQuality,
            maxElevationDeg = real.maxOf { it.maxElevationDeg },
            totalMinutes = real.sumOf { it.durationSec } / 60,
        )
    }

    /** 近 7 天的起始日期(含)。 */
    fun weekStartDate(today: LocalDate = LocalDate.now()): LocalDate = today.minusDays(6)

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
}
