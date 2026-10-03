package com.quannian.zhidong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.quannian.zhidong.tts.TtsCoach
import com.quannian.zhidong.ui.screens.NavigationHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化本地 TTS 引擎（默认关闭，用户在 App 内可打开；不阻塞 UI）
        TtsCoach.init(this)
        setContent {
            NavigationHost(Modifier.fillMaxSize())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放 TTS 资源，避免泄漏
        TtsCoach.shutdown()
    }
}
