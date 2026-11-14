package cn.armpal.rehab.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * 语音引导(PRD 适老化:多语音、少文字):
 * - Android 框架 TextToSpeech(zh-CN),SDK 6.2.8 无 TTS 包装(已核实)
 * - 触发点由 HomeStage 订阅 SessionState 驱动(反馈等级变化 / 完成提示),内置限频防轰炸
 * - 初始化失败静默降级(UI 文案本来就是主要反馈通道,语音是增强)
 * - 空间音频化(TTS PCM → Entity.playAudioStream)留作 W4 升级,当前走系统默认声道
 */
object SpatialVoiceGuide {

    private const val TAG = "VoiceGuide"

    @Volatile
    var enabled: Boolean = true

    private var tts: TextToSpeech? = null
    private var ready = false

    /** 每类提示的最小间隔(毫秒),防语音轰炸。 */
    private val lastSpokenAt = mutableMapOf<String, Long>()

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                val result = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.w(TAG, "zh-CN TTS not available, fallback to default language")
                    tts?.setLanguage(Locale.getDefault())
                }
                tts?.setSpeechRate(0.95f)
                Log.i(TAG, "TTS ready")
            } else {
                Log.w(TAG, "TTS init failed status=$status (语音降级为纯文字)")
                tts = null
            }
        }
    }

    /** 朗读一句;同类提示限频 [throttleMs]。 */
    fun speak(text: String, throttleMs: Long = 0L) {
        if (!enabled || !ready) return
        if (throttleMs > 0) {
            val now = android.os.SystemClock.elapsedRealtime()
            val last = lastSpokenAt[text] ?: 0L
            if (now - last < throttleMs) return
            lastSpokenAt[text] = now
        }
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString()) }
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}
