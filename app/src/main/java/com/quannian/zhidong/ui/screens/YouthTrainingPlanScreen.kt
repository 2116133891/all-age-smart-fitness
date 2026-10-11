package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.db.BodyTestRepository
import com.quannian.zhidong.db.TrainingPlanTask
import com.quannian.zhidong.db.BodyTestStandardService
import com.quannian.zhidong.service.TrainingPlanService
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.components.TeachingVideoPlayer
import com.quannian.zhidong.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/**
 * V2 体测 备考 训练 计划 页（任务 6）。
 *  1. 选 备考 周期（2/4/8 周）+ 每周 训练 天数
 *  2. 识别 薄弱 项（基于 最近 成绩 + 标准）
 *  3. 生成 每日 任务（数据 模型 + 规则，非 大模型 自由 文本）
 *  4. 每 任务 关联 教学 视频 / 步骤 / 安全 边界
 *  5. 标记 完成 / 跳过 / 重新 安排，保存 完成 情况
 *  6. 阶段 报告（完成率）
 */
@Composable
fun YouthTrainingPlanScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onPlayVideo: (assetPath: String, title: String, subtitle: String) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { BodyTestRepository(context) }
    val scope = rememberCoroutineScope()

    var weakItems by remember { mutableStateOf<List<String>>(emptyList()) }
    var weeks by remember { mutableStateOf(4) }
    var daysPerWeek by remember { mutableStateOf(4) }
    var planId by remember { mutableStateOf<String?>(null) }
    var tasks by remember { mutableStateOf<List<TrainingPlanTask>>(emptyList()) }
    var generating by remember { mutableStateOf(false) }

    // 识别 薄弱 项（基于 最近 成绩 + 标准 阈值）
    LaunchedEffect(repo) {
        val rep = repo.buildReport()
        weakItems = rep.weakItems
    }

    // 生成 计划
    fun generate() {
        generating = true
        scope.launch {
            val pid = planId ?: UUID.randomUUID().toString()
            planId = pid
            val params = TrainingPlanService.PlanParams(weakItems, weeks, daysPerWeek)
            val err = TrainingPlanService.validate(weeks, daysPerWeek)
            if (err == null) {
                val newTasks = TrainingPlanService.generate(params)
                repo.savePlanTasks(newTasks)
                tasks = repo.planTasks(pid)
            }
            generating = false
        }
    }

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
                    Text("备考 训练 计划", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Text("基于 真实 成绩 与 标准，保守 循序渐进（任务 6）", fontSize = 12.sp, color = Palette.inkSoft)
                }
            }
        }

        // 薄弱 项 提示
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("薄弱 项", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    if (weakItems.isEmpty()) {
                        Text("无 已 识别 薄弱 项（将 七 项 均衡 训练）", fontSize = 12.sp, color = Palette.inkSoft,
                            modifier = Modifier.weight(1f))
                    } else {
                        Text(weakItems.joinToString(" · "), fontSize = 12.sp, color = Palette.warn,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("安全 边界：${TrainingPlanService.SAFETY_BOUNDARY}",
                    fontSize = 11.sp, color = Palette.warn, lineHeight = 15.sp)
            }
        }

        // 参数 配置
        item {
            Card() {
                Text("备考 参数", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(8.dp))
                SectionLabel("备考 周期")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 4, 8).forEach { w ->
                        OptionChip(text = "${w} 周", selected = weeks == w, onClick = { weeks = w })
                    }
                }
                Spacer(Modifier.height(8.dp))
                SectionLabel("每周 训练 天数")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..7).forEach { d ->
                        OptionChip(text = "$d", selected = daysPerWeek == d, onClick = { daysPerWeek = d })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.accent, RoundedCornerShape(14.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { generate() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (generating) "生成 中…" else "生成 每日 训练 任务",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (generating) Palette.accent.copy(alpha = 0.7f) else Palette.accent)
                }
            }
        }

        // 阶段 报告（完成率）
        if (planId != null && tasks.isNotEmpty()) {
            val report = TrainingPlanService.completionReport(tasks)
            item {
                Card() {
                    Text("阶段 报告", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatBlock("${(report["rate"] as Double * 100).toInt()}%", "完成率", Palette.good)
                        StatBlock("${report["done"]}/${report["total"]}", "完成 任务", Palette.accent)
                        StatBlock("${report["skipped"]}", "跳过", Palette.inkSoft)
                    }
                }
            }
        }

        // 任务 列表（按 周 分组）
        if (tasks.isNotEmpty()) {
            item { Text("每日 训练 任务", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink) }
            tasks.forEach { t ->
                item {
                    TaskRow(
                        task = t,
                        onToggle = { scope.launch {
                            val newStatus = if (t.status == "done") "pending" else "done"
                            repo.updateTaskStatus(t.id, newStatus)
                            tasks = repo.planTasks(planId!!)
                        } },
                        onSkip = { scope.launch {
                            val newStatus = if (t.status == "skipped") "pending" else "skipped"
                            repo.updateTaskStatus(t.id, newStatus)
                            tasks = repo.planTasks(planId!!)
                        } },
                        onPlayVideo = { asset, title, sub -> onPlayVideo(asset, title, sub) }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, color = Palette.inkSoft, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) Palette.accent else Palette.cardSoft,
                RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) Palette.accent else Palette.inkSoft)
    }
}

@Composable
private fun StatBlock(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = Palette.inkSoft)
    }
}

/** 单个 训练 任务 行。 */
@Composable
private fun TaskRow(
    task: TrainingPlanTask,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onPlayVideo: (String, String, String) -> Unit
) {
    val done = task.status == "done"
    val skipped = task.status == "skipped"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.card, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (done) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (done) Palette.good else Palette.accent,
                modifier = Modifier.size(20.dp)
            )
            Text("第 ${task.dayIndex + 1} 天 · ${task.title}", fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = if (skipped) Palette.inkSoft else Palette.ink)
            Spacer(Modifier.weight(1f))
            Pill("${task.durationMin} min", Palette.accent)
        }
        Spacer(Modifier.height(4.dp))
        Text(task.steps, fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text("⚠ ${task.caution}", fontSize = 11.sp, color = Palette.warn, lineHeight = 14.sp)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (task.videoAsset.isNotBlank()) {
                    Box(
                        modifier = Modifier.clickable {
                            onPlayVideo(
                                task.videoAsset,
                                task.title + " · 教学 视频",
                                task.steps
                            )
                        }
                    ) {
                        Pill("▶ 播 教学 视频", Palette.accent)
                    }
                }
                if (skipped) Pill("已 跳过", Palette.inkSoft)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 操作 按钮
                TaskBtn(if (done) "取消 完成" else "标记 完成", done, onClick = onToggle)
                TaskBtn(if (skipped) "取消 跳过" else "跳过", skipped, onClick = onSkip)
            }
        }
    }
}

@Composable
private fun TaskBtn(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Palette.accentSoft else Palette.cardSoft, RoundedCornerShape(8.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = if (active) Palette.accent else Palette.inkSoft)
    }
}
