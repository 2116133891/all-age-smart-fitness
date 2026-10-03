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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * 数字人教学页：
 *  数字人 + 动作名称 + 步骤 + 注意事项 + "开始教学"/"开始跟练" 两个按钮。
 */
@Composable
fun ExerciseScreen(
    exerciseId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onStartTeach: () -> Unit,
    onStartFollow: () -> Unit
) {
    val ex = ExerciseRepository.find(exerciseId) ?: return
    val age = ex.ageGroup
    val color = Palette.ageColor(age.id)
    val soft = Palette.ageSoft(age.id)

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
                Text("数字人动作教学", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
            }
        }

        item {
            Card(borderColor = color.copy(alpha = 0.18f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CoachAvatar(ageId = age.id, size = 90.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(ex.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                            Pill(ex.level, color)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(ex.tagline, fontSize = 13.sp, color = Palette.inkSoft)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(background = soft, borderColor = color.copy(alpha = 0.2f), shape = 14.dp) {
                    Text("数字人教练", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
                    Spacer(Modifier.height(4.dp))
                    Text(ex.coachIntro, fontSize = 14.sp, color = Palette.ink, lineHeight = 22.sp)
                }
            }
        }

        item {
            Card() {
                Text("动作步骤", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(10.dp))
                ex.steps.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StepNumber(i + 1, color)
                        Text(s, fontSize = 14.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                    if (i < ex.steps.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }

        item {
            Card() {
                Text("注意事项", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(10.dp))
                ex.cautions.forEach { c ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CheckCircle, "", tint = color, modifier = Modifier.size(18.dp))
                        Text(c, fontSize = 14.sp, color = Palette.inkSoft, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Card() {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("跟练目标：${ex.targetReps} 次", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                    Spacer(Modifier.weight(1f))
                    if (ex.analysisKind != null) Pill("支持实时姿态检测", Palette.good) else Pill("暂为教学", Palette.warn)
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton(
                    text = "▶  开始教学",
                    bg = Palette.ink,
                    fg = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = onStartTeach
                )
                BigButton(
                    text = "▶  开始跟练",
                    bg = color,
                    fg = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = onStartFollow
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** 步骤编号圆点。 */
@Composable
private fun StepNumber(n: Int, color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("$n", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/** 全宽实心按钮。 */
@Composable
private fun BigButton(
    text: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
