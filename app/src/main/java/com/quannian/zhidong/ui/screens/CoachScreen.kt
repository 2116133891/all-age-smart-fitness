package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.repository.ExerciseRepository
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette

/**
 * 数字人教学页：动态数字人 + 实时"正在指导"台词 + 教学进度 + 两个行动按钮。
 *  台词按预设教练话术轮播，体现"数字人教练"的属性。
 */
@Composable
fun CoachScreen(
    exerciseId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onStartFollow: () -> Unit
) {
    val ex = ExerciseRepository.find(exerciseId) ?: return
    val age = ex.ageGroup
    val color = Palette.ageColor(age.id)
    val soft = Palette.ageSoft(age.id)

    // 轮播教练台词
    val lines = listOf(
        ex.coachIntro,
        "请站在摄像头前，保持全身处于画面中央。",
        "准备好以后，我们开始第一组动作。",
        "注意节奏均匀，不要憋气。",
        "保持身体平衡，慢慢来。",
        "你的动作不错，继续保持。"
    )
    var lineIdx by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2600)
            lineIdx = (lineIdx + 1) % lines.size
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
                }
                Text("数字人教学", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
            }
        }

        // 数字人舞台
        item {
            Card(borderColor = color.copy(alpha = 0.18f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(soft, RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CoachAvatar(ageId = age.id, size = 120.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(ex.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                            Pill(ex.level, color)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("数字人运动教练", fontSize = 12.sp, color = Palette.inkSoft)
                        Spacer(Modifier.height(10.dp))
                        Card(background = soft, borderColor = color.copy(alpha = 0.2f), shape = 14.dp) {
                            Text("正在指导：", fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(lines[lineIdx], fontSize = 14.sp, color = Palette.ink, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }

        // 今日计划
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("今天我们一起进行", fontSize = 14.sp, color = Palette.inkSoft)
                    Text("${ex.duration}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
                    Text("基础训练，共 ${ex.targetReps} 组动作。", fontSize = 14.sp, color = Palette.inkSoft)
                }
            }
        }

        // 动作要领速览
        item {
            Card() {
                Text("动作要领", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(10.dp))
                ex.steps.take(3).forEach { s ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Text("·", fontSize = 16.sp, color = color)
                        Text(s, fontSize = 14.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 行动按钮
        item {
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton("▶  开始教学", Palette.ink, Color.White, onStartFollow, Modifier.fillMaxWidth())
                ActionButton("▶  开始跟练", color, Color.White, onStartFollow, Modifier.fillMaxWidth())
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ActionButton(
    text: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
