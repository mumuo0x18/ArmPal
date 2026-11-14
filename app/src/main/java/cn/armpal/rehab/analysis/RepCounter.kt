package cn.armpal.rehab.analysis

/**
 * 计数状态机事件。
 */
sealed interface RepEvent {
    /** 完成一次达标重复。count 为累计次数,peakDeg 为本次顶点角度。 */
    data class RepCompleted(val count: Int, val peakDeg: Float) : RepEvent
}

enum class RepPhase { IDLE, RAISING, HOLD, LOWERING }

/**
 * 达标判定与计数状态机。
 *
 * 流程:IDLE →(抬过目标一半)→ RAISING →(达到目标角)→ HOLD →(保持足够时长)→ LOWERING → 计数+1
 * - HOLD 中跌破 0.6×目标角:视为未保持住,回 RAISING(允许再次冲顶)
 * - **LOWERING 必须先把手臂放回到"释放阈值"(0.25×目标角)以下,才允许开始下一次计数**,
 *   否则顶点持续保持会被误判为多次重复(2026-11-14 评审 P0-1)
 * - 顶点角度记录取 HOLD 期间最大值
 *
 * 纯 Kotlin 无 SDK 依赖,输入只有(角度, 时间戳),可表驱动单测。
 */
class RepCounter(
    val targetDeg: Float = 80f,
    private val holdMs: Long = 500L,
) {
    var phase: RepPhase = RepPhase.IDLE
        private set
    var count: Int = 0
        private set
    var peakDeg: Float = 0f
        private set

    private var holdStartMs: Long = 0L
    private var currentPeak: Float = 0f

    /** 每帧喂入当前抬举角与时间戳;完成一次重复时返回事件。 */
    fun onFrame(elevationDeg: Float, nowMs: Long): RepEvent? {
        if (!elevationDeg.isFinite()) return null
        if (elevationDeg > peakDeg) peakDeg = elevationDeg

        when (phase) {
            RepPhase.IDLE -> {
                if (elevationDeg > targetDeg * RAISE_TRIGGER_RATIO) {
                    phase = RepPhase.RAISING
                    currentPeak = elevationDeg
                }
            }

            RepPhase.LOWERING -> {
                // 释放:手臂回到低位才重新武装,避免顶点保持/小幅晃动被重复计数
                if (elevationDeg < targetDeg * RELEASE_RATIO) {
                    phase = RepPhase.IDLE
                }
            }

            RepPhase.RAISING -> {
                if (elevationDeg > currentPeak) currentPeak = elevationDeg
                if (elevationDeg >= targetDeg) {
                    phase = RepPhase.HOLD
                    holdStartMs = nowMs
                } else if (elevationDeg < targetDeg * ABORT_RATIO) {
                    // 半路放下,回到待机,不计次
                    phase = RepPhase.IDLE
                }
            }

            RepPhase.HOLD -> {
                if (elevationDeg > currentPeak) currentPeak = elevationDeg
                when {
                    elevationDeg < targetDeg * HOLD_KEEP_RATIO -> {
                        // 未保持住,回 RAISING 允许再次冲顶
                        phase = RepPhase.RAISING
                    }

                    nowMs - holdStartMs >= holdMs -> {
                        count++
                        phase = RepPhase.LOWERING
                        val ev = RepEvent.RepCompleted(count, currentPeak)
                        currentPeak = 0f
                        return ev
                    }
                }
            }
        }
        return null
    }

    /** 重置(新组开始)。 */
    fun reset() {
        phase = RepPhase.IDLE
        count = 0
        peakDeg = 0f
        holdStartMs = 0L
        currentPeak = 0f
    }

    companion object {
        private const val RAISE_TRIGGER_RATIO = 0.5f
        private const val ABORT_RATIO = 0.25f
        private const val HOLD_KEEP_RATIO = 0.6f

        /** 释放阈值:低于该比例才允许开始下一次重复。 */
        private const val RELEASE_RATIO = 0.25f
    }
}
