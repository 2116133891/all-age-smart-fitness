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
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * V2 青年 体测 完整 模块 主页（任务 4）。
 *  展示 七项 体测 的 最近 成绩 + 是否 已 录入，点击 进 详情（教学 视频 + 录入 + 标准 评分 + 趋势 + 训练）。
 *  底部 入口：体测 报告 / 训练 计划。
 *
 *  旧 的 5 项 星级 自评（BodyTestScreen）仍 保留 给 中年/银龄 用；青年 改 用 本 页。
 */
@Composable
fun YouthBodyTestScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onPickItem: (String) -> Unit,
    onReport: () -> Unit,
    onPlan: () -> Unit
) {
    val context = LocalContext.current
    val profile = remember { ProfileStore.load(context) }
    val gender = profile.gender
    val scope = rememberCoroutineScope()
    val repo = remember { BodyTestRepository(context) }
    val latest = remember { kotlinx.coroutines.flow.MutableStateFlow<Map<String, String>>(emptyMap()) }

    // 预 取 各 项 最近 成绩（展示 状态）
    LaunchedEffect(repo) {
        val map = mutableMapOf<String, String>()
        for (item in BodyTestCatalog.ITEMS) {
            val r = repo.latestResult(item.key)
            if (r != null) map[item.key] = formatItemValue(item.key, r)
        }
        latest.value = map
    }
    val latestValues by latest.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Text("青年 · 体质 测试", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Text("七 项 体测 + 教学 视频 + 标准 评分 + 训练 计划", fontSize = 12.sp, color = Palette.inkSoft)
                }
                CoachAvatar(ageId = "youth", size = 44.dp)
            }
        }

        // 七 项 体测
        items(BodyTestCatalog.ITEMS) { item ->
            val latestVal = latestValues[item.key]
            ItemCard(
                item = item,
                gender = gender,
                latestValue = latestVal,
                onClick = { onPickItem(item.key) }
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

/** 单项 体测 卡片（显示 名称/单位/最近 成绩）。 */
@Composable
private fun ItemCard(
    item: BodyTestCatalog.Item,
    gender: String,
    latestValue: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val displayName = BodyTestCatalog.displayName(item, gender)
    val hasVideo = !BodyTestCatalog.videoFor(item, gender)?.assetPath.isNullOrEmpty()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                if (hasVideo) Pill("视频", Palette.accent)
            }
            Spacer(Modifier.height(2.dp))
            Text(item.subtitle, fontSize = 11.sp, color = Palette.inkSoft)
            Spacer(Modifier.height(6.dp))
            if (latestValue != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Videocam, null, tint = Palette.good, modifier = Modifier.size(14.dp))
                    Text("最近：$latestValue", fontSize = 12.sp, color = Palette.good, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("未 录入 成绩", fontSize = 12.sp, color = Palette.inkSoft)
            }
        }
        Text("→", fontSize = 20.sp, color = Palette.accent, fontWeight = FontWeight.Bold)
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

/** 把 单项 体测 成绩 格式 化 为 展示 字符串（按 项目 单位）。 */
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
