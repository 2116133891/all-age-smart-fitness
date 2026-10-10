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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.model.AgeGroup
import com.quannian.zhidong.model.ProfileStore
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import androidx.compose.ui.platform.LocalContext

/**
 * 首页（参赛版）：全龄智动 · 数字人智能运动指导
 *   儿童 / 青年 / 中年 / 银龄 四个年龄段入口 + AI 动作诊断 + 我的训练
 *   + 新增：AI 小助手（本地离线问答）/ 运动手环 / 个性化设置。
 *  P0 稳定性：三个教练头像静止站立（无循环动画）；卡片一次性 220ms 淡入；点击克制 ripple。
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onPickAge: (String) -> Unit,
    onMyTraining: () -> Unit,
    onAiDiagnose: () -> Unit,
    onAssistant: () -> Unit = {},
    onWearable: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile = remember { ProfileStore.load(context) }

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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("全龄智动", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
                    Pill("你的画像 · ${ProfileStore.ageLabel(profile.ageId)}", Palette.ageColor(profile.ageId))
                }
                Spacer(Modifier.height(4.dp))
                Text("数字人智能运动指导", fontSize = 14.sp, color = Palette.inkSoft, letterSpacing = 1.sp)
                Spacer(Modifier.height(20.dp))
                Text("选择您的运动陪练", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                Spacer(Modifier.height(14.dp))
            }
        }

        // 四个年龄入口（教练头像静止站立；卡片一次性淡入 + 克制 ripple）
        items(AgeGroup.entries.toList()) { age ->
            AgeCard(
                age = age,
                onClick = { onPickAge(age.id) },
                icon = when (age) {
                    AgeGroup.CHILD -> Icons.Filled.ChildCare
                    AgeGroup.MIDDLE -> Icons.Filled.FitnessCenter
                    AgeGroup.SENIOR -> Icons.Filled.Accessibility
                    else -> Icons.Filled.SportsTennis
                }
            )
        }

        // AI 动作诊断入口（视频复盘核心亮点）
        item {
            AiDiagnoseCard(onClick = onAiDiagnose)
        }

        // 新增：AI 小助手（本地离线问答）
        item {
            FeatureCard(
                title = "AI 小助手",
                sub = "离线问答 · 运动/肩颈/康复/体测/手环",
                icon = Icons.Filled.SmartToy,
                color = Palette.accent,
                soft = Palette.accentSoft,
                onClick = onAssistant
            )
        }

        // 新增：运动手环
        item {
            FeatureCard(
                title = "运动手环",
                sub = "配对同步 · 心率/步数/睡眠 联训",
                icon = Icons.Filled.Sync,
                color = Palette.middle,
                soft = Palette.middleSoft,
                onClick = onWearable,
                badge = "演示"
            )
        }

        // 我的训练入口
        item {
            MyTrainingCard(onClick = onMyTraining)
        }

        // 个性化设置（年龄性别）
        item {
            FeatureCard(
                title = "个性化设置",
                sub = "年龄 · 性别 → 定制数字教练与推荐",
                icon = Icons.Filled.Accessibility,
                color = Palette.good,
                soft = Palette.good.copy(alpha = 0.12f),
                onClick = onProfile,
                badge = "已设 ${ProfileStore.ageLabel(profile.ageId)}"
            )
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

/** 年龄段入口卡片（静止教练头像 + 一次性淡入 + 克制 ripple）。 */
@Composable
private fun AgeCard(age: AgeGroup, onClick: () -> Unit, icon: ImageVector) {
    val color = Palette.ageColor(age.id)
    val soft = Palette.ageSoft(age.id)

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
            .clickable(onClick = onClick),
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
            CoachAvatar(ageId = age.id, size = 60.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Text(age.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            }
            Spacer(Modifier.height(3.dp))
            Text(age.tagline, fontSize = 13.sp, color = Palette.inkSoft)
            Spacer(Modifier.height(8.dp))
            Pill("含练 · 测 模块", color)
        }
        Text("→", fontSize = 24.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

/** 通用功能卡（图标 + 标题 + 描述 + 可选徽标）。 */
@Composable
private fun FeatureCard(
    title: String,
    sub: String,
    icon: ImageVector,
    color: Color,
    soft: Color,
    onClick: () -> Unit,
    badge: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.card, RoundedCornerShape(22.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(16.dp)
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
                .background(soft, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(28.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                if (badge != null) Pill(badge, color)
            }
            Spacer(Modifier.height(3.dp))
            Text(sub, fontSize = 12.sp, color = Palette.inkSoft)
        }
        Text("→", fontSize = 22.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

/** "AI 动作诊断" 入口卡片。 */
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
            Icon(Icons.Filled.Insights, contentDescription = "AI 动作诊断", tint = Palette.accent, modifier = Modifier.size(30.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("AI 动作诊断", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
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
