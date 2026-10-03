package com.quannian.zhidong.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.theme.Palette

/**
 * 启动页：数字人 + 品牌名 + 加载进度条。
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier, onDone: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    var ready by remember { mutableStateOf(false) }

    // 模拟"准备摄像头 + 加载模型"过程，避免空屏
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(450); progress = 0.33f
        kotlinx.coroutines.delay(450); progress = 0.7f
        kotlinx.coroutines.delay(500); progress = 1f
        kotlinx.coroutines.delay(400); ready = true
    }
    LaunchedEffect(ready) {
        if (ready) onDone()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(Palette.accentSoft, RoundedCornerShape(75.dp))
                    .padding(30.dp),
                contentAlignment = Alignment.Center
            ) {
                CoachAvatar(ageId = "youth", size = 100.dp)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "全龄智动",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Palette.ink,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "数字人智能运动指导平台",
                fontSize = 14.sp,
                color = Palette.inkSoft,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(30.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Palette.accent,
                    trackColor = Palette.line,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("正在准备摄像头与姿态识别模型…", fontSize = 12.sp, color = Palette.inkSoft)
        }
    }
}
