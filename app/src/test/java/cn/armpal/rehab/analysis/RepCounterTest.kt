package cn.armpal.rehab.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepCounterTest {

    private fun counter(target: Float = 80f, holdMs: Long = 500L) = RepCounter(target, holdMs)

    private fun feed(c: RepCounter, vararg frames: Pair<Float, Long>) {
        frames.forEach { (deg, t) -> c.onFrame(deg, t) }
    }

    @Test
    fun `达标序列 计数一次`() {
        val c = counter()
        assertNull(c.onFrame(0f, 0))
        assertNull(c.onFrame(50f, 100))
        assertNull(c.onFrame(80f, 200)) // 达到目标角 → HOLD
        assertNull(c.onFrame(85f, 400))
        assertNull(c.onFrame(82f, 650))
        val ev = c.onFrame(80f, 750)    // 保持 550ms ≥ 500ms → 完成
        assertNotNull(ev)
        assertEquals(1, c.count)
        assertEquals(1, (ev as RepEvent.RepCompleted).count)
        assertTrue(ev.peakDeg >= 80f)
    }

    @Test
    fun `未到目标角 不计次`() {
        val c = counter()
        c.onFrame(0f, 0)
        c.onFrame(40f, 100)
        c.onFrame(60f, 200)
        c.onFrame(30f, 400) // 半路放下
        c.onFrame(5f, 600)
        assertEquals(0, c.count)
        assertEquals(RepPhase.IDLE, c.phase)
    }

    @Test
    fun `顶点未保持住 回到 RAISING 后可再次冲顶`() {
        val c = counter()
        c.onFrame(0f, 0)
        c.onFrame(50f, 100)
        c.onFrame(80f, 200) // HOLD
        c.onFrame(45f, 300) // 跌破 0.6×80=48 → RAISING
        assertEquals(RepPhase.RAISING, c.phase)
        assertNull(c.onFrame(80f, 500))
        val ev = c.onFrame(82f, 1100)
        assertNotNull(ev)
        assertEquals(1, c.count)
    }

    @Test
    fun `连续两次达标 计数两次`() {
        val c = counter()
        c.onFrame(0f, 0)
        c.onFrame(45f, 100)
        c.onFrame(80f, 200)
        c.onFrame(80f, 800)
        // 第二次(必须放下到释放阈值以下再抬起)
        c.onFrame(10f, 1000)
        c.onFrame(45f, 1200)
        c.onFrame(80f, 1400)
        val ev = c.onFrame(81f, 2000)
        assertNotNull(ev)
        assertEquals(2, c.count)
    }

    /**
     * P0-1 回归:顶点持续保持 3 秒只能算一次。
     * 修复前(2026-11-14 评审)实测为 4 次。
     */
    @Test
    fun `顶点持续保持三秒 只计一次`() {
        val c = counter()
        var t = 0L
        c.onFrame(0f, t)
        for (e in listOf(30f, 50f, 70f, 80f, 85f)) {
            t += 100
            c.onFrame(e, t)
        }
        repeat(30) {
            t += 100
            c.onFrame(85f, t) // 手臂一直在顶点,没有放下
        }
        assertEquals(1, c.count)
    }

    /** 计次后只放到 40°(高于释放阈值 20°)不算成功释放,不能再次计数。 */
    @Test
    fun `未回到释放阈值 不重复计数`() {
        val c = counter()
        var t = 0L
        c.onFrame(0f, t)
        for (e in listOf(45f, 80f, 85f)) {
            t += 100
            c.onFrame(e, t)
        }
        t += 900 // 完成第一次
        c.onFrame(85f, t)
        assertEquals(1, c.count)
        assertEquals(RepPhase.LOWERING, c.phase)

        // 只放到 45°,再抬回顶点保持 3 秒
        t += 100
        c.onFrame(45f, t)
        for (e in listOf(60f, 80f, 85f)) {
            t += 100
            c.onFrame(e, t)
        }
        repeat(30) {
            t += 100
            c.onFrame(85f, t)
        }
        assertEquals(1, c.count)
    }

    @Test
    fun `抖动小角度 不触发`() {
        val c = counter()
        assertNull(c.onFrame(10f, 0))
        assertNull(c.onFrame(15f, 100))
        assertNull(c.onFrame(12f, 200))
        assertEquals(0, c.count)
        assertEquals(RepPhase.IDLE, c.phase)
    }

    @Test
    fun `NaN 输入 不崩溃不计数`() {
        val c = counter()
        feed(c, Float.NaN to 0L, 85f to 100L)
        assertEquals(0, c.count)
    }

    @Test
    fun `reset 清空状态`() {
        val c = counter()
        c.onFrame(0f, 0)
        c.onFrame(45f, 100)
        c.onFrame(80f, 200)
        c.onFrame(80f, 800)
        assertEquals(1, c.count)
        c.reset()
        assertEquals(0, c.count)
        assertEquals(RepPhase.IDLE, c.phase)
    }
}
