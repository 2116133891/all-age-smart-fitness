package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.db.BodyTestRepository
import com.quannian.zhidong.model.BodyTestCatalog
import com.quannian.zhidong.model.BodyTestItemKey
import com.quannian.zhidong.model.ProfileStore
import com.quannian.zhidong.repository.TeachingVideoRepository
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * V3.3 青年体测模块主页。
 *
 *  两类项目分离（spec §四）：
 *   - 动态跟练 · 测试项（50m / 800·1000m / 跳远 / 坐位体前屈 / 引体·仰卧）：
 *     有真实动作示范视频，逐项提供「看示范」「开始跟练」「测试·录入」独立入口。
 *     「看示范」→ 教学视频页；「开始跟练」→ 双栏跟练页 followItem/{key}；
 *     「测试·录入」→ 详情/人工录入页 youthBodyTestItem/{key}。
 *   - 独立测评录入项（BMI / 肺活量）：无动作示范视频，单列「测评·录入」区域，
 *     仅走人工录入，**不**映射到任何动作视频、**不**进入动态跟练列表。
 */
@Composable
fun YouthBodyTestScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onPickItem: (String) -> Unit,
    onWatchItem: (String) -> Unit,
    onFollow: (String) -> Unit,
    onReport: () -> Unit,
    onPlan: () -> Unit
) {
    val context = LocalContext.current
    val profile = remember { ProfileStore.load(context) }
    val gender = profile.gender
    val coachStyleLabel = remember { com.quannian.zhidong.model.CoachStyleStore.load(context).title }
    val scope = rememberCoroutineScope()
    val repo = remember { BodyTestRepository(context) }
    val latest = remember { kotlinx.coroutines.flow.MutableStateFlow<Map<String, String>>(emptyMap()) }

    // 预取各项最近成绩（展示状态）
    LaunchedEffect(repo) {
        val map = mutableMapOf<String, String>()
        for (item in BodyTestCatalog.ITEMS) {
            val r = repo.latestResult(item.key)
            if (r != null) map[item.key] = formatItemValue(item.key, r)
        }
        latest.value = map
    }
    val latestValues by latest.collectAsState()

    // 两类项目分离
    val motionItems = remember { BodyTestCatalog.ITEMS.filter { !TeachingVideoRepository.isMethodOnlyItem(it.key) } }
    val measureItems = remember { BodyTestCatalog.ITEMS.filter { TeachingVideoRepository.isMethodOnlyItem(it.key) } }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
                }
                Column(Modifier.weight(1f)) {
                    Text("青少年体测训练", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Text("练 · 运动训练 + 测 · 体测评估（$coachStyleLabel）", fontSize = 12.sp, color = Palette.inkSoft)
                }
                CoachAvatar(ageId = "youth", size = 44.dp)
            }
        }

        // P0-D：年龄段 + 教练介绍（与中/老年统一模板一致的「介绍」头块）
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("青少年 · 数字教练", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                        Spacer(Modifier.height(3.dp))
                        Text("科学训练 强健体魄：体测项目训练 + 动作示范与跟练，每次进步都记录。", fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 16.sp)
                    }
                }
            }
        }

        // ── 动态跟练 · 测试项（有动作示范视频，逐项独立入口） ──
        item { SectionLabel("动态 跟练 · 测试（有 动作 示范）", Palette.accent) }
        items(motionItems) { item ->
            MotionItemCard(
                item = item,
                gender = gender,
                latestValue = latestValues[item.key],
                onWatchItem = { onWatchItem(item.key) },
                onFollow = { onFollow(item.key) },
                onEnter = { onPickItem(item.key) }
            )
        }

        // ── 独立测评录入区域（BMI / 肺活量，无动作示范视频） ──
        item { SectionLabel("独立 测评 录入（无 动作 示范 · 仅 人工 录入）", Palette.warn) }
        items(measureItems) { item ->
            MeasureItemCard(
                item = item,
                gender = gender,
                latestValue = latestValues[item.key],
                onEnter = { onPickItem(item.key) }
            )
        }

        // 报告 + 计划 入口
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HubEntryCard(
                    title = "体测 报告",
                    sub = "完成 情况 + 成绩 趋势 + 薄弱 项",
                    icon = Icons.Filled.Insights,
                    modifier = Modifier.weight(1f),
                    onClick = onReport
                )
                HubEntryCard(
                    title = "训练 计划",
                    sub = "备考 周期 + 每日 任务",
                    icon = Icons.Filled.Schedule,
                    modifier = Modifier.weight(1f),
                    onClick = onPlan
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/** 分区分组小标题。 */
@Composable
private fun SectionLabel(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(4.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Palette.inkSoft)
    }
}

/** 动态跟练·测试项卡片：看示范 / 开始跟练 / 测试·录入 三个独立入口。 */
@Composable
private fun MotionItemCard(
    item: BodyTestCatalog.Item,
    gender: String,
    latestValue: String?,
    onWatchItem: (String) -> Unit,
    onFollow: () -> Unit,
    onEnter: () -> Unit
) {
    val displayName = BodyTestCatalog.displayName(item, gender)
    val demoVideo = remember(item.key, gender) { TeachingVideoRepository.videoForItem(item.key, gender) }
    val hasDemo = demoVideo != null && demoVideo.assetPath.isNotBlank()
    val capability = TeachingVideoRepository.followCapabilityLabel(item.key)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                if (hasDemo) Pill("有 示范", Palette.accent) else Pill("无 视频", Palette.inkSoft)
            }
            Spacer(Modifier.height(4.dp))
            Text(item.subtitle, fontSize = 11.sp, color = Palette.inkSoft)
            Spacer(Modifier.height(4.dp))
            Pill(capability, Palette.good)
            Spacer(Modifier.height(8.dp))
            if (latestValue != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Videocam, null, tint = Palette.good, modifier = Modifier.size(14.dp))
                    Text("最近：$latestValue", fontSize = 12.sp, color = Palette.good, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("未 录入 成绩", fontSize = 12.sp, color = Palette.inkSoft)
            }
            Spacer(Modifier.height(10.dp))
            // 三个独立入口（spec §3.1）
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EntryButton(text = "看 示范", enabled = hasDemo,
                    onClick = { onWatchItem(item.key) },
                    modifier = Modifier.weight(1f))
                EntryButton(text = "开始 跟练", enabled = true, onClick = onFollow,
                    modifier = Modifier.weight(1f))
                EntryButton(text = "测试·录入", enabled = true, onClick = onEnter,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 独立测评录入项卡片（BMI / 肺活量）：无动作示范，仅 测评·录入。 */
@Composable
private fun MeasureItemCard(
    item: BodyTestCatalog.Item,
    gender: String,
    latestValue: String?,
    onEnter: () -> Unit
) {
    val displayName = BodyTestCatalog.displayName(item, gender)
    val methodNote = when (item.key) {
        BodyTestItemKey.BMI -> "BMI = 体重kg ÷ 身高m²（真实 计算，非 体脂率）"
        BodyTestItemKey.VITAL_CAPACITY -> "肺活量 需 专用 仪器，人工 录入 实测 值（mL）"
        else -> "人工 录入"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Pill("测评 录入", Palette.warn)
            }
            Spacer(Modifier.height(4.dp))
            Text(methodNote, fontSize = 11.sp, color = Palette.inkSoft)
            Spacer(Modifier.height(4.dp))
            if (latestValue != null) {
                Text("最近：$latestValue", fontSize = 12.sp, color = Palette.good, fontWeight = FontWeight.SemiBold)
            } else {
                Text("未 录入", fontSize = 12.sp, color = Palette.inkSoft)
            }
        }
        // 测评·录入 入口（→ 详情/人工录入页）
        EntryButton(text = "测评·录入", enabled = true, onClick = onEnter, modifier = Modifier.width(88.dp))
    }
}

/** 入口小按钮（可见文字 + 独立可点）。 */
@Composable
private fun EntryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) Palette.accentSoft else Palette.cardSoft, RoundedCornerShape(10.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = if (enabled) Palette.accent else Palette.inkSoft.copy(alpha = 0.5f))
    }
}

/** 报告 / 计划 入口 卡。 */
@Composable
private fun HubEntryCard(
    title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                .background(Palette.accentSoft, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Palette.accent, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            Spacer(Modifier.height(2.dp))
            Text(sub, fontSize = 11.sp, color = Palette.inkSoft)
        }
    }
}

/** 把 单项 体测 成绩 格式化 为 展示 字符串（按 项目 单位）。 */
private fun formatItemValue(itemKey: String, r: com.quannian.zhidong.db.BodyTestResult): String {
    return when (itemKey) {
        BodyTestItemKey.BMI -> "BMI ${"%.1f".format(r.rawValue)}（${"%.0f".format(r.weightKg)}kg / ${"%.0f".format(r.heightCm)}cm）"
        BodyTestItemKey.VITAL_CAPACITY -> "${"%.0f".format(r.rawValue)} mL"
        BodyTestItemKey.RUN_50M -> "${"%.2f".format(r.rawValue)} s"
        BodyTestItemKey.SEATED_REACH -> "${"%.1f".format(r.rawValue)} cm"
        BodyTestItemKey.LONG_JUMP -> "${"%.0f".format(r.rawValue)} cm"
        BodyTestItemKey.PULLUP_SITUP -> "${"%.0f".format(r.rawValue)} 次"
        BodyTestItemKey.RUN_800_1000 -> "${"%.0f".format(r.rawValue)} s"
        else -> r.unit
    }
}
