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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.quannian.zhidong.ui.theme.Palette

/** 一条手环同步数据。 */
data class WdMetric(val label: String, val value: String, val unit: String, val hint: String, val emoji: String)

/**
 * 运动手环连接页（对标主流运动 App 的"设备同步"场景）。
 *  诚实标注：MVP 为**演示连接**——展示手环可同步的数据维度（步数/心率/睡眠/卡路里）
 *  与"连上后"的联训闭环；真实蓝牙配对接入 Phase 2（BLE 服务）预留。
 */
@Composable
fun WearableScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var paired by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var lastSync by remember { mutableStateOf("尚未同步") }

    // 演示数据（连接后填充）
    val metrics = remember(paired) {
        if (!paired) emptyList()
        else listOf(
            WdMetric("今日步数", "8", "千步", "已同步 · 达标 80%", "👣"),
            WdMetric("静息心率", "62", "bpm", "昨夜 02:10 测量 · 正常", "❤️"),
            WdMetric("深睡时长", "1", "小时42分", "环比 +12% · 睡眠质量良好", "😴"),
            WdMetric("今日消耗", "480", "kcal", "含 2 次训练 · 达标 60%", "🔥")
        )
    }

    fun doSync() {
        if (!paired || syncing) return
        syncing = true
        scope.launch {
            kotlinx.coroutines.delay(1200)
            lastSync = "刚刚 · 已同步 4 项"
            syncing = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg),
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
                Text("运动手环", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.weight(1f))
            }
            Text("连接手环 · 数据驱动更精准的训练与恢复", fontSize = 13.sp, color = Palette.inkSoft)
        }

        // 连接状态卡
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Palette.card, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Palette.accentSoft, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⌚", fontSize = 30.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("全龄智动手环", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                        Text(if (paired) "已连接 · $lastSync" else "未连接", fontSize = 12.sp,
                            color = if (paired) Palette.good else Palette.inkSoft)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    // 连接 / 断开
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (paired) Palette.cardSoft else Palette.accent, RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { paired = !paired }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (paired) "断开" else "扫码 / 蓝牙配对",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = if (paired) Palette.ink else Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                    // 手动同步
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (paired && !syncing) Palette.accentSoft else Palette.cardSoft, RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { doSync() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (paired) {
                                Icon(Icons.Filled.Sync, null,
                                    tint = if (syncing) Palette.accent else Palette.inkSoft,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(if (syncing) "同步中…" else "立即同步", fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (syncing) Palette.accent else Palette.inkSoft)
                            } else {
                                Text("未连接", fontSize = 14.sp, color = Palette.inkSoft)
                            }
                        }
                    }
                }
            }
        }

        // 数据卡
        if (metrics.isNotEmpty()) {
            item {
                Text("同步数据", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
            }
            items(metrics) { mtr ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.card, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(mtr.emoji, fontSize = 24.sp)
                    Column(Modifier.weight(1f)) {
                        Text(mtr.label, fontSize = 13.sp, color = Palette.inkSoft)
                        Text(mtr.hint, fontSize = 11.sp, color = Palette.inkSoft)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${mtr.value} ${mtr.unit}",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.accent)
                    }
                }
            }
        }

        // 联训说明
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text("手环 × 训练 闭环", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "训练时手环实时心率 → 自动判定运动强度区间",
                    "步数/活动量 → 补全\"非训练日\"的日常消耗",
                    "睡眠数据 → 次日训练强度自适应（睡不好自动降强度）",
                    "跟练/视频报告可叠加手环心率做双维评估"
                ).forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("•", color = Palette.accent)
                        Text(line, fontSize = 12.sp, color = Palette.inkSoft,
                            modifier = Modifier.weight(1f), lineHeight = 17.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text("⚠ 演示连接：Phase 2 接入真实 BLE 设备（当前展示手环可提供的数据维度）。",
                    fontSize = 11.sp, color = Palette.warn)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
