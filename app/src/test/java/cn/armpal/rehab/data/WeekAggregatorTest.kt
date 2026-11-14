package cn.armpal.rehab.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekAggregatorTest {

    private fun session(
        date: String,
        reps: Int,
        demo: Boolean = false,
        quality: Float = -1f,
        durationSec: Int = 300,
        maxElevation: Float = 85f,
    ) = TrainingSessionEntity(
        date = date, exerciseId = "e", exerciseName = "测试",
        reps = reps, targetReps = 12, maxElevationDeg = maxElevation,
        avgQuality = quality, symmetryDeg = 5f, durationSec = durationSec,
        setsDone = 1, demoMode = demo, createdAtMs = 0L,
    )

    private val today: LocalDate = LocalDate.of(2026, 11, 14) // 周六

    @Test
    fun `空记录 全零且streak为0`() {
        val r = WeekAggregator.aggregate(emptyList(), today)
        assertEquals(0, r.weekReps)
        assertEquals(0, r.activeDays)
        assertEquals(0, r.streakDays)
        assertEquals(-1f, r.avgQuality, 1e-4f)
    }

    @Test
    fun `演示数据不计入统计`() {
        val r = WeekAggregator.aggregate(
            listOf(session(today.toString(), reps = 10, demo = true)),
            today,
        )
        assertEquals(0, r.weekReps)
        assertEquals(0, r.streakDays)
    }

    @Test
    fun `今天练了 streak 从今天往前数`() {
        val r = WeekAggregator.aggregate(
            listOf(
                session(today.toString(), 12),
                session(today.minusDays(1).toString(), 12),
                session(today.minusDays(2).toString(), 12),
                // 3 天前断档
                session(today.minusDays(4).toString(), 12),
            ),
            today,
        )
        assertEquals(3, r.streakDays)
        assertEquals(4, r.activeDays)
        assertEquals(48, r.weekReps)
    }

    @Test
    fun `今天还没练 昨天连续不算断`() {
        val r = WeekAggregator.aggregate(
            listOf(
                session(today.minusDays(1).toString(), 12),
                session(today.minusDays(2).toString(), 12),
            ),
            today,
        )
        assertEquals(2, r.streakDays)
    }

    @Test
    fun `断档后 streak 归零`() {
        val r = WeekAggregator.aggregate(
            listOf(session(today.minusDays(3).toString(), 12)),
            today,
        )
        assertEquals(0, r.streakDays)
    }

    @Test
    fun `质量分平均 只算有效样本`() {
        val r = WeekAggregator.aggregate(
            listOf(
                session(today.toString(), 12, quality = 0.8f),
                session(today.minusDays(1).toString(), 12, quality = 0.6f),
                session(today.minusDays(1).toString(), 8, quality = -1f),
            ),
            today,
        )
        assertEquals(0.7f, r.avgQuality, 1e-4f)
    }

    @Test
    fun `时长与最高角度聚合`() {
        val r = WeekAggregator.aggregate(
            listOf(
                session(today.toString(), 12, durationSec = 600, maxElevation = 90f),
                session(today.minusDays(1).toString(), 10, durationSec = 300, maxElevation = 84f),
            ),
            today,
        )
        assertEquals(15, r.totalMinutes)
        assertEquals(90f, r.maxElevationDeg, 1e-4f)
    }
}
