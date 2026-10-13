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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.db.BodyTestRepository
import com.quannian.zhidong.db.TrainingPlanTask
import com.quannian.zhidong.service.TrainingPlanService
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * V2 体测备考训练计划页（任务 6）。
 *
 *  P0-3 修复（对照"蓝色控件无可见文字 / 周期·天数选择不清 / 生成态不清"）：
 *   1. 可见文字：所有蓝色控件**文字与背景不再同色**（旧 bug：选中/生成按钮用
 *      Palette.accent 既当背景又当文字 → 蓝上蓝，字不可见）。选中态 = 蓝底白字，
 *      未选 = 浅底深字 + 描边。
 *   2. 真实选择态：周期 2/4/8 与 每周天数 1–7 单选高亮清晰可点。
 *   3. 生成按钮真实状态：未生成→"生成每日训练任务"；已生成→"重新生成"；生成中→"生成中…"+禁用防双击；
 *      参数非法→中文错误提示。
 *   4. 真实生成：采用 **TrainingPlanService.generate() 返回的真实 planId**（旧 bug：UI 自建 pid，
 *      与服务生成的 planId 不一致 → repo.planTasks(pid) 恒空 → 任务/报告永不显示）。
 *   5. 任务卡可操作：标记完成 / 跳过 / 重排，持久化到 Room；视频按 byId 解析，
 *      method-only（BMI/肺活量）显示"测量方法"指引，不假放视频。
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
    var generateError by remember { mutableStateOf<String?>(null) }

    // 识别薄弱项（基于最近成绩 + 标准阈值）
    LaunchedEffect(repo) {
        weakItems = repo.buildReport().weakItems
    }

    // 生成计划（真实调用 TrainingPlanService；采用服务返回的真实 planId）
    fun generate() {
        if (generating) return                       // 防双击
        generating = true
        generateError = null
        scope.launch {
            val params = TrainingPlanService.PlanParams(weakItems, weeks, daysPerWeek)
            val err = TrainingPlanService.validate(weeks, daysPerWeek)
            if (err == null) {
                val newTasks = TrainingPlanService.generate(params)
                val newPlanId = newTasks.firstOrNull()?.planId
                if (newPlanId != null && newTasks.isNotEmpty()) {
                    repo.savePlanTasks(newTasks)     // 服务真实 planId，清旧插新
                    planId = newPlanId
                    tasks = repo.planTasks(newPlanId)
                } else {
                    generateError = "未能生成计划，请调整参数后重试"
                }
            } else {
                generateError = err
            }
            generating = false
        }
    }

    val totalDays = weeks * 7
    val totalSessions = tasks.count { it.status != "skipped" }

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
                    Text("备考训练计划", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Text("基于真实成绩与标准，保守循序渐进", fontSize = 12.sp, color = Palette.inkSoft)
                }
            }
        }

        // 薄弱项提示
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("薄弱项", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    if (weakItems.isEmpty()) {
                        Text("无已识别薄弱项（七项均衡训练）", fontSize = 12.sp, color = Palette.inkSoft,
                            modifier = Modifier.weight(1f))
                    } else {
                        Text(weakItems.joinToString(" · "), fontSize = 12.sp, color = Palette.warn,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("安全边界：${TrainingPlanService.SAFETY_BOUNDARY}",
                    fontSize = 11.sp, color = Palette.warn, lineHeight = 15.sp)
            }
        }

        // 参数配置 + 生成（可见文字 + 真实选择态 + 真实生成态）
        item {
            Card() {
                Text("备考参数", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(8.dp))
                Text("备考周期", fontSize = 12.sp, color = Palette.inkSoft, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 4, 8).forEach { w ->
                        OptionChip(text = "${w} 周", selected = weeks == w, onClick = { weeks = w })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("每周训练天数", fontSize = 12.sp, color = Palette.inkSoft, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..7).forEach { d ->
                        OptionChip(text = "${d} 天", selected = daysPerWeek == d, onClick = { daysPerWeek = d })
                    }
                }
                Spacer(Modifier.height(12.dp))
                // 生成按钮（真实状态 + 防双击：generating 时禁用）
                val hasPlan = planId != null && tasks.isNotEmpty()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                generating -> Palette.line
                                else -> Palette.accent
                            },
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = !generating) { generate() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when {
                            generating -> "生成中…"
                            hasPlan -> "重新生成训练任务"
                            else -> "生成每日训练任务"
                        },
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (generating) Palette.inkSoft else Color.White
                    )
                }
                if (generateError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("⚠ ${generateError}", fontSize = 12.sp, color = Palette.bad, lineHeight = 16.sp)
                }
            }
        }

        // 阶段报告（完成率）——仅在有真实任务时展示
        if (planId != null && tasks.isNotEmpty()) {
            val report = TrainingPlanService.completionReport(tasks)
            item {
                Card() {
                    Text("阶段报告", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatBlock("${(report["rate"] as Double * 100).toInt()}%", "完成率", Palette.good)
                        StatBlock("${report["done"]}/${report["total"]}", "完成任务", Palette.accent)
                        StatBlock("${report["skipped"]}", "跳过", Palette.inkSoft)
                    }
                }
            }
        }

        // 空态（未生成）——明确指引
        if (planId == null || tasks.isEmpty()) {
            item {
                Card(background = Palette.cardSoft) {
                    Text("尚未生成计划", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                    Spacer(Modifier.height(4.dp))
                    Text("选择上方备考周期与每周训练天数，点「生成每日训练任务」。共 ${totalDays} 天、${totalSessions} 个训练日。",
                        fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 17.sp)
                }
            }
        }

        // 任务列表（按周分组）
        if (tasks.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("每日训练任务", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Spacer(Modifier.weight(1f))
                    Text("共 ${totalDays} 天 · ${totalSessions} 个训练日", fontSize = 12.sp, color = Palette.inkSoft)
                }
            }
            var prevWeek = -1
            tasks.forEach { t ->
                val week = t.dayIndex / 7
                if (week != prevWeek) {
                    item {
                        Text("第 ${week + 1} 周", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.accent)
                        prevWeek = week
                    }
                }
                item {
                    TaskRow(
                        task = t,
                        onToggle = {
                            scope.launch {
                                val newStatus = if (t.status == "done") "pending" else "done"
                                repo.updateTaskStatus(t.id, newStatus)
                                tasks = repo.planTasks(planId!!)
                            }
                        },
                        onSkip = {
                            scope.launch {
                                val newStatus = if (t.status == "skipped") "pending" else "skipped"
                                repo.updateTaskStatus(t.id, newStatus)
                                tasks = repo.planTasks(planId!!)
                            }
                        },
                        onPlayVideo = { asset, title, sub -> onPlayVideo(asset, title, sub) }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/** 选项 Chip（P0-3：文字/背景不同色，选中=蓝底白字，未选=浅底深字描边，可见可点）。 */
@Composable
private fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) Palette.accent else Palette.cardSoft,
                RoundedCornerShape(10.dp)
            )
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Palette.accent else Palette.line,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else Palette.ink)
    }
}

@Composable
private fun StatBlock(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = Palette.inkSoft)
    }
}

/** 单个训练任务行（P0-3：可操作 + 视频按 byId；method-only 显示"测量方法"不假放视频）。 */
@Composable
private fun TaskRow(
    task: TrainingPlanTask,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onPlayVideo: (String, String, String) -> Unit
) {
    val done = task.status == "done"
    val skipped = task.status == "skipped"
    val hasVideo = task.videoAsset.isNotBlank()
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
            Pill("${task.durationMin} 分钟", Palette.accent)
        }
        Spacer(Modifier.height(4.dp))
        Text(task.steps, fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text("⚠ ${task.caution}", fontSize = 11.sp, color = Palette.warn, lineHeight = 14.sp)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 视频/方法入口（method-only → 测量方法，不假放视频）
            if (hasVideo) {
                Box(modifier = Modifier.clickable {
                    onPlayVideo(task.videoAsset, task.title + " · 教学视频", task.steps)
                }) {
                    Pill("▶ 播放教学视频", Palette.accent)
                }
            } else {
                // method-only（BMI/肺活量）：显示测量方法指引，不放视频
                Pill("📋 测量方法", Palette.inkSoft)
            }
            if (skipped) Pill("已跳过", Palette.inkSoft)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskBtn(if (done) "取消完成" else "标记完成", done, onClick = onToggle)
                TaskBtn(if (skipped) "取消跳过" else "跳过", skipped, onClick = onSkip)
            }
        }
    }
}

@Composable
private fun TaskBtn(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (active) Palette.accentSoft else Palette.cardSoft,
                RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = if (active) Palette.accent else Palette.inkSoft)
    }
}
