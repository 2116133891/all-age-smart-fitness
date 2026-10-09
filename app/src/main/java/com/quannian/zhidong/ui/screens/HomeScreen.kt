package com.quannian.zhidong.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.model.AgeGroup
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette

/**
 * 首页（竞赛版，对照 spec §40）：
 *   全龄智动 · 数字人智能运动指导
 *   儿童 / 青年 / 银龄 三个年龄段入口 + 「AI 动作诊断」+「我的训练」
 *
 *  P0 稳定性（对应验收）：
 *   - 三个教练头像**静止站立**（[CoachAvatar] 委托 CoachVisual，无任何循环动画）。
 *   - 点击卡片 = **克制**的 Material ripple（有界，不造成布局抖动）。
 *   - 首次进入卡片 = **一次性** 220ms 淡入（graphicsLayer，不改布局；按卡片身份 key，
 *     不会在快速点击 / 状态更新 / 重复进出时重复叠加）。
 *
 *  P1 视觉一致性：
 *   - 入口图标统一为 Material 图标（统一线条图标风格，避免 emoji 与系统图标混搭）；
 *     教练形象仍为真实 3D 数字人 PNG（人物用图片，控件用图标，二者不混用）。
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

        // 三个年龄入口（教练头像静止站立；卡片一次性淡入 + 克制 ripple）
        items(AgeGroup.entries.toList()) { age ->
            AgeCard(
                age = age,
                onClick = { onPickAge(age.id) },
                icon = when (age) {
                    AgeGroup.CHILD -> Icons.Filled.ChildCare
                    AgeGroup.YOUTH -> Icons.Filled.SportsTennis
                    AgeGroup.SENIOR -> Icons.Filled.Accessibility
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

/**
 * 年龄段入口卡片。
 *  - 一次性 220ms 淡入（首次组合时播放，key 为 [age.id]，不会重复 / 叠加 / 抖动布局）。
 *  - 点击 = 有界 ripple（克制的按压反馈），不改变卡片尺寸 => 无位置跳变。
 */
@Composable
private fun AgeCard(age: AgeGroup, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val color = Palette.ageColor(age.id)
    val soft = Palette.ageSoft(age.id)

    // 一次性淡入（graphicsLayer alpha，纯绘制，不改布局）
    var hasEntered by remember(age.id) { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(
        targetValue = if (hasEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "ageCardEnter_${age.id}"
    )
    LaunchedEffect(age.id) { hasEntered = true }

    Row(
        modifier = Modifier
            .graphicsLayer { alpha = enterAlpha }
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.card, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember(age.id) { MutableInteractionSource() }
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
            // 静止站立的真实数字教练（不跳动 / 不缩放 / 不闪烁）
            CoachAvatar(ageId = age.id, size = 60.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // 统一线条图标（替代 emoji），tint = 年龄段主色
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
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
                interactionSource = remember { MutableInteractionSource() }
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
            Icon(Icons.Filled.Insights, contentDescription = "AI 动作诊断", tint = Palette.accent, modifier = Modifier.size(30.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("AI 动作诊断", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            }
            Spacer(Modifier.height(3.dp))
            Text("上传视频 · 发现问题 · 数字教练针对性纠正", fontSize = 12.sp, color = Palette.inkSoft)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill("仅本机分析", Palette.accent)
                Pill("改善前后对比", Palette.good)
            }
        }
        Text("→", fontSize = 22.sp, color = Palette.accent, fontWeight = FontWeight.Bold)
    }
}

/** "我的训练" 入口卡片。 */
@Composable
private fun MyTrainingCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.card, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() }
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
            Icon(Icons.Filled.History, contentDescription = "我的训练", tint = Palette.accent, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("我的训练", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            Spacer(Modifier.height(3.dp))
            Text("历史数据 · 成长曲线 · 训练记录", fontSize = 12.sp, color = Palette.inkSoft)
        }
        Text("→", fontSize = 22.sp, color = Palette.accent, fontWeight = FontWeight.Bold)
    }
}
