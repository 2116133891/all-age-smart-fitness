package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.ui.components.CoachVisual
import com.quannian.zhidong.ui.theme.Palette

/**
 * 启动页（V3.5）：干净明亮的运动科技视觉，品牌名 + 副标题 + 教练形象。
 *
 * - 无模拟进度条、无"准备摄像头/加载模型"文案（冷启动不做 CameraX/MediaPipe 初始化，
 *   相关能力在进入跟练/姿态识别页时才惰性创建）。
 * - 仅保留短暂的品牌定版停留，随后直接进入主流程；不阻塞任何真实服务。
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        // 品牌定版：短促停留即可，无任何假加载
        kotlinx.coroutines.delay(700)
        onDone()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg),
        contentAlignment = Alignment.Center
    ) {
        // 背景轻光斑（运动科技感，不使用任何进度指示）
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(260.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Palette.accentSoft, Palette.bg),
                        center = Offset(0f, 0f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Palette.accentSoft, Palette.bg),
                        center = Offset(0f, 0f)
                    )
                )
        )

        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 竖版透明全身教练立绘（与引导/个人中心统一主视觉）
            CoachVisual.CoachPortrait(
                ageId = "youth",
                modifier = Modifier,
                size = 300.dp
            )
            Spacer(Modifier.height(20.dp))
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
        }
    }
}
