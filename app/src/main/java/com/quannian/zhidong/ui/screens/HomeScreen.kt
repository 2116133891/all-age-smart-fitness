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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PlayCircle
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
 * 首页（参赛版 V2）：全龄智动 · 全龄 智能 运动 指导 平台
 *   定位：面向 青年 体质 测试、中年 健康 管理、银龄 科学 运动 的 全龄 平台。
 *   三大 人群 入口（青年 体测 / 中年 健康 / 银龄 运动），青年 体测 为 重点 展示 模块。
 *   P0 稳定性：教练 头像 静止 站立（无 循环 动画）；卡片 一次性 220ms 淡入；点击 克制 ripple。
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onPickAge: (String) -> Unit,
    onMyTraining: () -> Unit,
    onAiDiagnose: () -> Unit,
    onAssistant: () -> Unit = {},
    onWearable: () -> Unit = {},
    onProfile: () -> Unit = {},
    onYouthBodyTest: () -> Unit = {},
    onContinueLast: () -> Unit = {}
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
        // 顶部 品牌 hero（青绿→深蓝 渐变 卡：定位 + 画像 + 数字 人 形象）
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Palette.senior, Palette.accent, Palette.youth)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("全龄智动", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = androidx.compose.ui.graphics.Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("青年 体测 · 中年 健康 · 银龄 科学 运动",
                        fontSize = 13.sp, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f))
                    Spacer(Modifier.height(2.dp))
                    Text("${ProfileStore.ageLabel(profile.ageId)} · ${ProfileStore.genderLabel(profile.gender)} · 数字 人 智能 运动 指导 平台",
                        fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f))
                }
                // 数字 人 形象（当前 画像 年龄）
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(
                        com.quannian.zhidong.ui.components.CoachVisual.coachResource(profile.ageId)
                    ),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.size(76.dp)
                )
            }
        }

        // 重点：青年 体测 模块（本次 版本 重点 展示）
        item {
            MainTasksCard(onContinueLast = onContinueLast, onYouthBodyTest = onYouthBodyTest)
        }

        // 三大 人群 入口（教练 头像 静止 站立；卡片 一次性 淡入 + 克制 ripple）
        items(AgeGroup.entries.toList()) { age ->
            AgeCard(
                age = age,
                onClick = { if (age == AgeGroup.YOUTH) onYouthBodyTest() else onPickAge(age.id) },
                icon = when (age) {
                    AgeGroup.MIDDLE -> Icons.Filled.FitnessCenter
                    AgeGroup.SENIOR -> Icons.Filled.Accessibility
                    else -> Icons.Filled.SportsTennis
                }
            )
        }

        // 推荐 教学 内容（真实 资源 索引；按 当前 人群 展示 视频；无 记录 时 空 态 引导）
        item { RecommendedContentCard(profile = profile, onYouthBodyTest = onYouthBodyTest) }

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

/** 重点展示：青年体测模块入口（本次版本核心，置顶高亮卡）。 */
@Composable
private fun YouthBodyTestFeatureCard(onClick: () -> Unit) {
    val color = Palette.accent
    var hasEntered by remember { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(
        targetValue = if (hasEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "youthFeatureEnter"
    )
    LaunchedEffect(Unit) { hasEntered = true }

    Row(
        modifier = Modifier
            .graphicsLayer { alpha = enterAlpha }
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.card, RoundedCornerShape(24.dp))
            .border(2.dp, color.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
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
                .size(84.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Palette.accentSoft, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            CoachAvatar(ageId = "youth", size = 72.dp)
        }
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill("重点 模块", color)
                Pill("七项 体测", Palette.good)
            }
            Spacer(Modifier.height(5.dp))
            Text("青年 · 体质 测试", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            Spacer(Modifier.height(3.dp))
            Text("体测 备考 · 专项 训练 · AI 动作 指导", fontSize = 13.sp, color = Palette.inkSoft)
            Spacer(Modifier.height(6.dp))
            Text("BMI · 肺活量 · 50m · 坐位体前屈 · 立定 跳远 · 引体/仰卧 · 1000/800m",
                fontSize = 11.sp, color = Palette.inkSoft, lineHeight = 15.sp)
        }
        Text("→", fontSize = 24.sp, color = color, fontWeight = FontWeight.Bold)
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
                Text(age.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                if (age.focus == "重点 展示") Pill("重点", color)
            }
            Spacer(Modifier.height(3.dp))
            Text(age.tagline, fontSize = 13.sp, color = Palette.inkSoft)
            Spacer(Modifier.height(8.dp))
            Pill(age.focus, color)
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

/**
 * 当前 用户 的 主要 任务（V3 首页 第三部分）。
 *  只 用 真实 数据：上次 训练 / 体测 进度。无 记录 时 显示 首次 使用 引导，
 *  不 生成 虚假 统计。
 */
@Composable
private fun MainTasksCard(onContinueLast: () -> Unit, onYouthBodyTest: () -> Unit) {
    val context = LocalContext.current
    var lastSession by remember { mutableStateOf<com.quannian.zhidong.db.TrainingSession?>(null) }
    var planRate by remember { mutableStateOf(-1.0) }

    LaunchedEffect(Unit) {
        val tr = com.quannian.zhidong.db.TrainingRepository(context.applicationContext)
        lastSession = runCatching { tr.summary() }.getOrNull()?.lastSession
        val br = com.quannian.zhidong.db.BodyTestRepository(context.applicationContext)
        val planId = runCatching { br.latestPlanId() }.getOrNull()
        if (planId != null) {
            planRate = runCatching { br.planCompletionRate(planId) }.getOrNull() ?: -1.0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.card, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Text("当前任务", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.accentSoft, RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onContinueLast() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.History, null, tint = Palette.accent, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text("继续上次训练", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Text(
                    if (lastSession != null) "上次「${lastSession?.exerciseName}」${lastSession?.score} 分"
                    else "还没有训练记录，先完成一次跟练",
                    fontSize = 11.sp, color = Palette.inkSoft
                )
            }
            Text("→", fontSize = 18.sp, color = Palette.accent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.good.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onYouthBodyTest() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Insights, null, tint = Palette.good, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text("体测备考进度", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Text(
                    if (planRate >= 0.0) "当前计划完成率 ${(planRate * 100).toInt()}%"
                    else "进入体测模块，生成你的备考计划",
                    fontSize = 11.sp, color = Palette.inkSoft
                )
            }
            Text("→", fontSize = 18.sp, color = Palette.good, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * 推荐 教学 内容（V3 首页 第四部分）。
 *  视频 一律 来自 真实 资源 索引（TeachingVideoRepository），按 当前 人群 展示。
 *  无 视频 / 未 接入 时 显示 空 态 引导，不 用 假 封面。
 */
@Composable
private fun RecommendedContentCard(profile: com.quannian.zhidong.model.Profile, onYouthBodyTest: () -> Unit) {
    val repo = com.quannian.zhidong.repository.TeachingVideoRepository
    val videos = remember { repo.allVideoItems() }
    val color = Palette.ageColor(profile.ageId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.card, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("推荐教学", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
            Pill(ProfileStore.ageLabel(profile.ageId), color)
        }
        Spacer(Modifier.height(10.dp))

        if (videos.isEmpty()) {
            Text("当前还没有可播放的教学视频。", fontSize = 13.sp, color = Palette.inkSoft)
        } else {
            videos.take(2).forEach { v ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.cardSoft, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onYouthBodyTest() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Palette.accentSoft, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayCircle, null, tint = Palette.accent, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(v.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                        Text(v.summary, fontSize = 11.sp, color = Palette.inkSoft, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
