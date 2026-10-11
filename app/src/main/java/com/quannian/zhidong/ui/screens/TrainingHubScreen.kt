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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.db.TrainingRepository
import com.quannian.zhidong.model.AgeGroupConfig
import com.quannian.zhidong.model.ProfileStore
import com.quannian.zhidong.repository.TeachingVideoRepository
import com.quannian.zhidong.ui.theme.Palette

/**
 * "训练" 底部 Tab（V3）：集中管理训练计划、任务、教学视频、跟练记录、训练历史。
 *  替代旧 "功能" 宫格页。
 */
@Composable
fun TrainingHubScreen(
    modifier: Modifier = Modifier,
    onBodyTest: () -> Unit = {},
    onVideoPick: () -> Unit = {},
    onWearable: () -> Unit = {},
    onTraining: () -> Unit = {},
    onYouthTrainingPlan: () -> Unit = {},
    onYouthBodyTestReport: () -> Unit = {},
    onTeachingVideo: (assetPath: String, title: String, subtitle: String) -> Unit = { _, _, _ -> },
    onProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile = remember { ProfileStore.load(context) }
    var summary by remember { mutableStateOf<TrainingRepository.Summary?>(null) }
    var videos by remember { mutableStateOf(emptyList<com.quannian.zhidong.model.BodyTestVideo>()) }

    LaunchedEffect(Unit) {
        val s = runCatching { TrainingRepository(context.applicationContext).summary() }.getOrNull()
        summary = s
        videos = TeachingVideoRepository.allVideoItems()
    }

    val ageColor = Palette.ageColor(profile.ageId)

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("训练中心", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
                Text("${AgeGroupConfig.ageLabel(profile.ageId)} · ${ProfileStore.genderLabel(profile.gender)}",
                    fontSize = 13.sp, color = Palette.inkSoft)
            }
        }

        // ── 主要任务卡片 ──
        item {
            val sessions = summary?.sessionCount ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Palette.card, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBlock(if (sessions > 0) "$sessions 次" else "—", "累计训练", Palette.accent)
                StatBlock(if (sessions > 0) "${summary?.avgScore ?: 0} 分" else "—", "平均得分", Palette.good)
                StatBlock(if (sessions > 0) "${(summary?.totalDurationSec ?: 0) / 60} 分钟" else "—", "总时长", ageColor)
                StatBlock("—", "连续天数", Palette.warn)
            }
        }

        // ── 训练计划 & 任务 ──
        item {
            SectionHeader("训练计划 & 任务")
            HubRow("青年体测 · 备考计划", "周期 / 天数 / 薄弱项优先 / 渐进强度", Icons.Filled.Schedule, Palette.accent,
                onYouthTrainingPlan)
            HubRow("训练报告 & 趋势", "阶段报告 / 弱项 / 成绩变化", Icons.Filled.Insights, Palette.good,
                onYouthBodyTestReport)
            HubRow("我的训练 · 历史", "成长曲线 / 成就徽章 / 最近记录", Icons.Filled.History, Palette.middle,
                onTraining)
        }

        // ── 教学视频 ──
        item {
            SectionHeader("教学视频")
            if (videos.isEmpty()) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.card)
                    .padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("暂无视频资源", fontSize = 14.sp, color = Palette.inkSoft)
                }
            } else {
                // 前 4 个 + "查看全部"
                videos.take(4).forEach { v ->
                    HubRow(v.title, v.summary, Icons.Filled.PlayCircle, Palette.accent,
                        { onTeachingVideo(v.assetPath, v.title, v.summary) })
                }
                if (videos.size > 4) {
                    HubRow("查看全部 ${videos.size} 条视频", "AI 诊断 + 教学示范", Icons.Filled.PlayCircle, Palette.accent,
                        onVideoPick)
                }
            }
        }

        // ── 其他 ──
        item {
            SectionHeader("其他")
            HubRow("体测自评 · 按人群", "青年七项 / 中年银龄自评", Icons.Filled.FitnessCenter, Palette.senior,
                onBodyTest)
            HubRow("运动手环 · 配对", "心率 / 步数 / 睡眠 联训（演示）", Icons.Filled.History, Palette.warn,
                onWearable)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@Composable
private fun StatBlock(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = Palette.inkSoft)
    }
}

@Composable
private fun HubRow(
    title: String,
    sub: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
            Text(sub, fontSize = 12.sp, color = Palette.inkSoft)
        }
        Text("→", fontSize = 20.sp, color = color, fontWeight = FontWeight.Bold)
    }
}
