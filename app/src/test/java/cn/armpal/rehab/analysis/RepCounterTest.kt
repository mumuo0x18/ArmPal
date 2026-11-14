package cn.armpal.rehab.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepCounterTest {

    private fun counter(target: Float = 80f, holdMs: Long = 500L) = RepCounter(target, holdMs)

    @Test
    fun `达标序列 计数一次`() {
        val c = counter()
        // 0 → 50(触发 RAISING)→ 85(进入 HOLD,t=400)→ 保持 500ms → 计数
        assertNull(c.onFrame(0f, 0))
        assertNull(c.onFrame(50f, 100))
        assertNull(c.onFrame(80f, 200)) // 达到目标角 → 进入 HOLD,holdStart=200
        assertNull(c.onFrame(85f, 400))
        assertNull(c.onFrame(82f, 650)) // 保持 450ms,未够
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
        c.onFrame(80f, 200) // 进入 HOLD
        c.onFrame(45f, 300) // 跌破 0.6×80=48 → 回 RAISING
        assertEquals(RepPhase.RAISING, c.phase)
        assertNull(c.onFrame(80f, 500)) // 再次进入 HOLD
        val ev = c.onFrame(82f, 1100)
        assertNotNull(ev)
        assertEquals(1, c.count)
    }

    @Test
    fun `连续两次达标 计数两次`() {
        val c = counter()
        // 第一次
        c.onFrame(0f, 0)
        c.onFrame(45f, 100)
        c.onFrame(80f, 200)
        c.onFrame(80f, 800)
        // 第二次(放下再抬起)
        c.onFrame(10f, 1000)
        c.onFrame(45f, 1200)
        c.onFrame(80f, 1400)
        val ev = c.onFrame(81f, 2000)
        assertNotNull(ev)
        assertEquals(2, c.count)
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
