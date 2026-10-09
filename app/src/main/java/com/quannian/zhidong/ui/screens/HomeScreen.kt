package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.model.AgeGroup
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette

/**
 * 首页（竞赛版，对照 spec §40）：
 *   全龄智动
 *   数字人智能运动指导
 *   👧 儿童  轻运动 · 快乐成长
 *   🏃 青年  科学运动 · 姿态纠正
 *   🧓 银龄  舒缓运动 · 健康相伴
 *   + "我的训练" 入口（历史数据 / 成长曲线）
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onPickAge: (String) -> Unit,
    onMyTraining: () -> Unit,
    onAiDiagnose: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 顶部品牌区
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text("全龄智动", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
                Spacer(Modifier.height(4.dp))
                Text("数字人智能运动指导", fontSize = 14.sp, color = Palette.inkSoft, letterSpacing = 1.sp)
                Spacer(Modifier.height(20.dp))
                Text("选择您的运动陪练", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                Spacer(Modifier.height(14.dp))
            }
        }

        // 三个年龄入口
        items(AgeGroup.entries.toList()) { age ->
            AgeCard(
                age = age,
                onClick = { onPickAge(age.id) },
                icon = when (age) {
                    AgeGroup.CHILD -> "👧"
                    AgeGroup.YOUTH -> "🏃"
                    AgeGroup.SENIOR -> "🧓"
                }
            )
        }

        // AI 动作诊断入口（视频复盘核心亮点，spec §十一/§三十一）
        item {
            AiDiagnoseCard(onClick = onAiDiagnose)
        }

        // 我的训练入口
        item {
            MyTrainingCard(onClick = onMyTraining)
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun AgeCard(age: AgeGroup, onClick: () -> Unit, icon: String) {
    val color = Palette.ageColor(age.id)
    val soft = Palette.ageSoft(age.id)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.card, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(soft, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            CoachAvatar(ageId = age.id, size = 56.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 18.sp)
                Text(age.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            }
            Spacer(Modifier.height(3.dp))
            Text(age.tagline, fontSize = 13.sp, color = Palette.inkSoft)
            Spacer(Modifier.height(8.dp))
            Pill("3 项运动", color)
        }
        Text("→", fontSize = 24.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

/** "AI 动作诊断" 入口卡片（视频复盘：上传视频→分析→纠正→再练→前后对比，spec §十一/§三十一）。 */
@Composable
private fun AiDiagnoseCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.card, RoundedCornerShape(24.dp))
            .border(1.5.dp, Palette.accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(18.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.accentSoft, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("🎯", fontSize = 30.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("AI 动作诊断", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            }
            Spacer(Modifier.height(3.dp))
            Text("上传视频 · 发现问题 · 数字教练针对性纠正", fontSize = 12.sp, color = Palette.inkSoft)
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.quannian.zhidong.ui.components.Pill("仅本机分析", Palette.accent)
                com.quannian.zhidong.ui.components.Pill("改善前后对比", Palette.good)
            }
        }
        Text("→", fontSize = 22.sp, color = Palette.accent, fontWeight = FontWeight.Bold)
    }
}

/** "我的训练" 入口卡片。 */
@Composable
private fun MyTrainingCard(onClick: () -> Unit) {    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.card, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Palette.accentSoft, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("📊", fontSize = 26.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("我的训练", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            Spacer(Modifier.height(3.dp))
            Text("历史数据 · 成长曲线 · 训练记录", fontSize = 12.sp, color = Palette.inkSoft)
        }
        Text("→", fontSize = 22.sp, color = Palette.accent, fontWeight = FontWeight.Bold)
    }
}
