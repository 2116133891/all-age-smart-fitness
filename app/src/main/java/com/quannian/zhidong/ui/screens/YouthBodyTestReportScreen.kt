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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.db.BodyTestRepository
import com.quannian.zhidong.db.BodyTestResult
import com.quannian.zhidong.db.BodyTestStandardService
import com.quannian.zhidong.model.BodyTestCatalog
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import kotlin.math.abs

/**
 * V2 青年 体测 报告 页（任务 7）。
 *  - 完成 / 未 录入 项目
 *  - 各 项 最近 成绩 + 较 上次 变化
 *  - 训练 任务 完成率
 *  - 薄弱 项 + 下 阶段 建议
 *  - 历史 趋势（真实 数据；不足 提示 数据 不足）
 */
@Composable
fun YouthBodyTestReportScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onPickItem: (String) -> Unit,
    onPlan: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { BodyTestRepository(context) }
    var report by remember { mutableStateOf<BodyTestRepository.BodyTestReport?>(null) }
    var allHist by remember { mutableStateOf<Map<String, List<BodyTestResult>>>(emptyMap()) }
    // 防重复点击：进入计划后置位；页面重组时保持，避免重复跳转
    var planNavigated by remember { mutableStateOf(false) }
    // 每次进入报告页（key = repo）重置防抖标志，配合导航返回
    LaunchedEffect(repo) { planNavigated = false }

    LaunchedEffect(repo) {
        val planId = repo.latestPlanId()
        report = repo.buildReport(planId)
        val h = mutableMapOf<String, List<BodyTestResult>>()
        for (item in BodyTestCatalog.ITEMS) h[item.key] = repo.history(item.key)
        allHist = h
    }

    val rep = report ?: return

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
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink) }
                Column(Modifier.weight(1f)) {
                    Text("体测 阶段 报告", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Text("基于 真实 成绩 与 训练 记录 生成（不 画 假 曲线）", fontSize = 12.sp, color = Palette.inkSoft)
                }
            }
        }

        // 完成 / 未 录入 概览
        val total = BodyTestCatalog.ITEMS.size
        val completed = rep.latestByItem.size
        item {
            Card() {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Stat("已 录入", "$completed/$total", Palette.good, Modifier.weight(1f))
                    Stat("未 录入", "${total - completed}", Palette.inkSoft, Modifier.weight(1f))
                    Stat("任务 完成率", "${(rep.planCompletion * 100).toInt()}%", Palette.accent, Modifier.weight(1f))
                }
            }
        }

        // 各 项 最近 成绩 + 较 上次 变化
        item { Text("各项 最近 成绩", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink) }
        BodyTestCatalog.ITEMS.forEach { itemDef ->
            val latest = rep.latestByItem[itemDef.key]
            val hist = allHist[itemDef.key] ?: emptyList()
            item {
                ReportRow(
                    item = itemDef, latest = latest, prev = hist.getOrNull(hist.size - 2),
                    onClick = { onPickItem(itemDef.key) }
                )
            }
        }

        // 薄弱 项 + 下 阶段 建议
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Insights, null, tint = Palette.warn, modifier = Modifier.size(20.dp))
                    Text("薄弱 项 与 下 阶段 建议", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                }
                Spacer(Modifier.height(8.dp))
                if (rep.weakItems.isEmpty()) {
                    Text("暂无 已 识别 薄弱 项（成绩 未 低于 及格 阈值 或 未 录入）。保持 节奏，按 计划 循序渐进。",
                        fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 17.sp)
                } else {
                    Text("识别 出 ${rep.weakItems.size} 个 薄弱 项：", fontSize = 12.sp, color = Palette.ink)
                    Spacer(Modifier.height(4.dp))
                    rep.weakItems.forEach { k ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", color = Palette.warn, fontWeight = FontWeight.Bold)
                            val name = BodyTestCatalog.byKey(k)?.name ?: k
                            Text(name + " 建议 优先 针对 训练（见 训练 计划）", fontSize = 12.sp, color = Palette.inkSoft)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("安全 提示：${TrainingPlanSafetyNote()}", fontSize = 11.sp, color = Palette.warn, lineHeight = 15.sp)
            }
        }

        // 趋势 提示（各 项 历史）
        item {
            Card() {
                Text("成绩 趋势（真实 数据）", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(6.dp))
                val withTrend = BodyTestCatalog.ITEMS.filter { (allHist[it.key] ?: emptyList()).size >= 2 }
                if (withTrend.isEmpty()) {
                    Text("数据 不足，完成 更多 测试 后 生成 趋势（当前 各 项 历史 < 2 条，不 画 假 曲线）。",
                        fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 16.sp)
                } else {
                    withTrend.forEach { itd ->
                        val h = allHist[itd.key]!!
                        val delta = h.last().rawValue - h[h.size - 2].rawValue
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(itd.name, fontSize = 12.sp, color = Palette.ink)
                            Spacer(Modifier.weight(1f))
                            val absDelta = kotlin.math.abs(delta)
                            Text(
                                if (itd.inputType == BodyTestCatalog.InputType.SINGLE && delta < 0)
                                    "改善 ${"%.2f".format(absDelta)} ${itd.unit} ↓" else "较 上次 ${"%.2f".format(absDelta)} ${itd.unit}",
                                fontSize = 12.sp,
                                color = if (absDelta < 0.001) Palette.inkSoft else if (itd.isLowerBetter) Palette.good else Palette.warn
                            )
                        }
                    }
                }
            }
        }

        // 行动：正式 Material Button（防重复点击 + 正确导航 + 返回）



        item {
            Button(
                onClick = {
                    if (planNavigated) return@Button
                    planNavigated = true
                    onPlan()
                },
                enabled = !planNavigated,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.accent),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 14.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
            ) {
                Icon(
                    Icons.Filled.FitnessCenter, null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "去生成备考训练计划",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

private fun TrainingPlanSafetyNote(): String =
    "训练 中 如 出现 明显 疼痛、异常 呼吸 困难 或 头晕，立即 停止 并 寻求 医护 帮助；本 平台 不 提供 医疗 诊断。"

private val BodyTestCatalog.Item.isLowerBetter
    get() = key in setOf(
        com.quannian.zhidong.model.BodyTestItemKey.RUN_50M,
        com.quannian.zhidong.model.BodyTestItemKey.RUN_800_1000
    )

@Composable
private fun Stat(
    label: String, value: String, color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = Palette.inkSoft)
    }
}

/** 单项 报告 行（最近 成绩 + 较 上次）。 */
@Composable
private fun ReportRow(
    item: BodyTestCatalog.Item,
    latest: BodyTestResult?,
    prev: BodyTestResult?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.card, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (latest != null) Icons.Filled.CheckCircle else Icons.Filled.Insights,
            null, tint = if (latest != null) Palette.good else Palette.inkSoft.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            if (latest != null) {
                val deltaTxt = if (prev != null) {
                    val d = latest.rawValue - prev.rawValue
                    val arrow = when {
                        d == 0.0 -> "持平"
                        item.isLowerBetter -> if (d < 0) "↓改善" else "↑变差"
                        else -> if (d < 0) "↓下降" else "↑提升"
                    }
                    "较 上次 ${arrow} ${"%.2f".format(kotlin.math.abs(d))}"
                } else "首次 记录"
                Text(deltaTxt, fontSize = 11.sp, color = Palette.inkSoft)
            } else {
                Text("未 录入", fontSize = 11.sp, color = Palette.inkSoft)
            }
        }
        if (latest != null) {
            val grade = if (latest.scoreValid)
                "${latest.score} 分 · ${BodyTestStandardService.gradeLabel(latest.score)}"
            else "已 记录（无 正式 分数）"
            Pill(grade, if (latest.scoreValid) Palette.good else Palette.inkSoft)
        }
    }
}
