package com.quannian.zhidong.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log

/**
 * 本地 TTS 语音教练（对照 spec §26）。
 *
 *  使用 Android 框架自带 TextToSpeech，**不依赖外网**。
 *  - 可开关（默认关闭，用户在设置里打开）
 *  - 生命周期正确：[shutdown] 释放资源
 *  - 不阻塞 UI：[speak] 走框架内部线程
 *
 *  用法：进入跟练页时 `TtsCoach.init(context)`（在 MainActivity），
 *  数字人话术变化时调用 `TtsCoach.speak(text)`；App 销毁时 `shutdown()`。
 */
object TtsCoach {

    private const val TAG = "TtsCoach"

    private var tts: TextToSpeech? = null
    private var initialized = false
    private var ready = false

    /** 是否启用（默认关闭；由用户在 App 内开关）。 */
    var isEnabled: Boolean = false

    /**
     * 初始化 TTS 引擎（幂等）。调用方应在 App 启动时调用一次。
     * 返回是否成功提交初始化（实际 ready 在回调里确认）。
     */
    fun init(context: Context): Boolean {
        if (initialized) return true
        initialized = true
        return try {
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    tts?.language = java.util.Locale("zh", "CN")
                    tts?.setSpeechRate(1.0f)
                }
                Log.i(TAG, "TTS ready=$ready")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "TTS init failed", e)
            false
        }
    }

    /** 设置是否播放语音（用户开关）。改名 setVoiceEnabled 避免与 isEnabled 的 setter 撞 JVM 签名。 */
    fun setVoiceEnabled(on: Boolean) {
        isEnabled = on
        if (!on) tts?.stop()
    }

    /** 说一句教练话术（不阻塞 UI，走框架内部线程）。 */
    fun speak(text: String) {
        if (!isEnabled || !ready) return
        val t = tts ?: return
        val utter = "coach-${System.currentTimeMillis()}"
        t.speak(text, TextToSpeech.QUEUE_ADD, null, utter)
    }

    /** 立即停止当前播放。 */
    fun stop() {
        tts?.stop()
    }

    /** 释放资源（App 销毁时调用）。 */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        initialized = false
        ready = false
        isEnabled = false
    }
}
