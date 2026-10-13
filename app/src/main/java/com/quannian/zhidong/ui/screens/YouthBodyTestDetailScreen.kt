package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.db.BodyTestRepository
import com.quannian.zhidong.db.BodyTestResult
import com.quannian.zhidong.db.BodyTestStandardService
import com.quannian.zhidong.model.BodyTestCatalog
import com.quannian.zhidong.model.ProfileStore
import com.quannian.zhidong.repository.TeachingVideoRepository
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.components.TeachingVideoPlayer
import com.quannian.zhidong.ui.theme.Palette
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * V3.3 单项体测详情/测评页。
 *  - 教学视频（真实播放；method-only 项显示"方法说明"，不假放动作视频）
 *  - 动作要点 / 常见错误 / 安全提示
 *  - 手工录入成绩（BMI 双值；其余单值 + 日期）
 *  - 标准评分（仅当适用标准且数据充分时输出分数；否则标注不输出正式分数）
 *  - 历史趋势（<2 条提示数据不足）
 *  - 开始跟练入口（仅运动型项；method-only 项无跟练入口）
 */
@Composable
fun YouthBodyTestDetailScreen(
    itemKey: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onTrain: () -> Unit = {},
    onFollow: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile = remember { ProfileStore.load(context) }
    val gender = profile.gender
    val item = remember(itemKey) { BodyTestCatalog.byKey(itemKey) }
    val isMethodOnly = remember(itemKey) { TeachingVideoRepository.isMethodOnlyItem(itemKey) }
    val video = remember(itemKey, gender) { TeachingVideoRepository.videoFor(itemKey, gender) }
    val color = Palette.accent

    val repo = remember { BodyTestRepository(context) }
    var history by remember { mutableStateOf<List<BodyTestResult>>(emptyList()) }
    var revision by remember { mutableStateOf(0) }

    // 初始与每次 save 后刷新历史
    LaunchedEffect(itemKey, revision) {
        history = repo.history(itemKey)
    }
    val latest = history.maxByOrNull { it.testDate }

    if (item == null) {
        // key 不存在兜底（导航错误）
        Column(modifier = modifier.fillMaxSize().background(Palette.bg)) {
            Spacer(Modifier.size(16.dp))
            Text("未找到该体测项目", fontSize = 14.sp, color = Palette.inkSoft)
        }
        return
    }

    val displayName = BodyTestCatalog.displayName(item, gender)

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
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
                    Text(displayName, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Text(item.subtitle, fontSize = 12.sp, color = Palette.inkSoft)
                }
            }
        }

        // 跟练入口（仅运动型项；method-only 项无动作可跟）
        if (!isMethodOnly) {
            item {
                FollowEntryCard(
                    title = "开始跟练（双栏：示范 + 实时摄像头）",
                    sub = TeachingVideoRepository.followCapabilityLabel(itemKey),
                    onClick = onFollow
                )
            }
        }

        // ── 教学视频（真实播放） ──
        if (video != null && video.assetPath.isNotBlank()) {
            item {
                Card() {
                    TeachingVideoPlayer(assetPath = video.assetPath, title = video.title, subtitle = video.summary)
                }
            }
        } else if (video != null) {
            item {
                Card() {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Warning, null, tint = Palette.warn, modifier = Modifier.size(20.dp))
                        Text("无动作视频", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                        Pill("方法说明", color)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(video.summary, fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 16.sp)
                }
            }
        }

        // ── 动作要点 / 错误 / 安全 ──
        if (video != null) {
            item {
                Card() {
                    Text("动作要点", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Spacer(Modifier.height(6.dp))
                    video.keyPoints.forEach { kp ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(kp, fontSize = 12.sp, color = Palette.inkSoft,
                                modifier = Modifier.weight(1f), lineHeight = 17.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TipRow("常见错误", Palette.bad, video.commonErrors)
                    Spacer(Modifier.height(4.dp))
                    TipRow("安全提示", Palette.warn, video.safetyTip)
                }
            }
        }

        // ── 手工录入成绩 ──
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("录入真实成绩", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Spacer(Modifier.weight(1f))
                    Text(item.unit, fontSize = 12.sp, color = Palette.inkSoft)
                }
                Spacer(Modifier.height(8.dp))

                EntryForm(
                    item = item, gender = gender, repo = repo, profileGender = gender,
                    onSave = { revision++; }
                )

                Spacer(Modifier.height(8.dp))
                // 标准评分（诚实标注）
                if (BodyTestStandardService.applicable(gender)) {
                    Text(
                        "评分依据：${BodyTestStandardService.STANDARD_NAME}（${BodyTestStandardService.STANDARD_VERSION}）。" +
                            "保守实现：仅取及格阈值判定弱项；完整 100/90/80/70/60 分档需依据官方原表核对。",
                        fontSize = 11.sp, color = Palette.inkSoft, lineHeight = 15.sp
                    )
                } else {
                    Text(
                        "当前项目无可追溯的权威标准或不适用 —— 仅记录原始成绩与趋势，不输出正式分数（任务 5 要求）。",
                        fontSize = 11.sp, color = Palette.warn, lineHeight = 15.sp
                    )
                }

                if (latest != null && latest.scoreValid) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Check, null, tint = Palette.good, modifier = Modifier.size(16.dp))
                        Text("最近官方评分：${latest.score} 分（${BodyTestStandardService.gradeLabel(latest.score)}）",
                            fontSize = 12.sp, color = Palette.good, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onTrain,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 14.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
                ) {
                    Icon(
                        Icons.Filled.FitnessCenter, null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("生成备考训练计划", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }

        // ── 历史趋势 ──
        item {
            Card() {
                Text("历史成绩趋势", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(6.dp))
                val points = history.map { it.rawValue }.filter { it > 0.0 }
                if (points.size < 2) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Warning, null, tint = Palette.warn, modifier = Modifier.size(18.dp))
                        Text("数据不足，完成更多测试后生成趋势（任务 7：不画假曲线）",
                            fontSize = 12.sp, color = Palette.inkSoft, modifier = Modifier.weight(1f), lineHeight = 16.sp)
                    }
                } else {
                    TrendChart(history = history, unit = item.unit, color = color)
                    Spacer(Modifier.height(6.dp))
                    val prevVal = if (history.size >= 2) history[history.size - 2].rawValue else 0.0
                    val delta = points.last() - prevVal
                    Text(
                        "最近一次：${"%.2f".format(points.last())} $item.unit" +
                            if (history.size >= 2) "（较上次 ${if (delta >= 0) "↑" else "↓"} ${"%.2f".format(kotlin.math.abs(delta))}）" else "",
                        fontSize = 12.sp, color = Palette.inkSoft
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/** 开始跟练 入口卡（运动型项）。 */
@Composable
private fun FollowEntryCard(title: String, sub: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.accentSoft, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                .background(Palette.accent, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            Spacer(Modifier.height(2.dp))
            Text(sub, fontSize = 11.sp, color = Palette.inkSoft)
        }
        Text("→", fontSize = 18.sp, color = Palette.accent, fontWeight = FontWeight.Bold)
    }
}

/** 提示行（标签 + 说明）。 */
@Composable
private fun TipRow(label: String, labelColor: Color, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
        Text(text, fontSize = 12.sp, color = Palette.inkSoft, modifier = Modifier.weight(1f), lineHeight = 16.sp)
    }
}

/** 手工录入表单（按录入类型分支）：BMI 双值 / 单值 + 日期。 */
@Composable
private fun EntryForm(
    item: BodyTestCatalog.Item,
    gender: String,
    repo: BodyTestRepository,
    profileGender: String,
    onSave: () -> Unit
) {
    val scope = rememberCoroutineScope()

    when (item.inputType) {
        BodyTestCatalog.InputType.HEIGHT_WEIGHT -> {
            var height by remember { mutableStateOf("") }
            var weight by remember { mutableStateOf("") }
            var saved by remember { mutableStateOf(false) }
            val h = height.toDoubleOrNull()
            val w = weight.toDoubleOrNull()
            val bmi = if (h != null && w != null && h > 0) w / (h * h) else null
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = height, onValueChange = { height = it },
                        label = { Text("身高 (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weight, onValueChange = { weight = it },
                        label = { Text("体重 (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (bmi != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("BMI =", fontSize = 13.sp, color = Palette.inkSoft)
                        Text("${"%.1f".format(bmi)} kg/m²", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.accent)
                        Text("（= 体重kg ÷ 身高m²，非体脂率）", fontSize = 11.sp, color = Palette.inkSoft)
                    }
                }
                SaveButton(
                    enabled = bmi != null, saved = saved,
                    onClick = {
                        if (bmi == null) return@SaveButton
                        val bh = h ?: 0.0; val bw = w ?: 0.0
                        saved = true
                        scope.launch {
                            repo.saveResult(BodyTestResult(
                                itemKey = item.key, ageGroup = "youth", gender = profileGender,
                                testDate = System.currentTimeMillis(),
                                rawValue = bmi, rawValue2 = 0.0,
                                heightCm = bh, weightKg = bw, unit = "kg/m²",
                                source = "manual",
                                note = "BMI 手工计算"
                            ))
                            onSave()
                        }
                    }
                )
            }
        }
        BodyTestCatalog.InputType.SINGLE,
        BodyTestCatalog.InputType.COUNT -> {
            var value by remember { mutableStateOf("") }
            var saved by remember { mutableStateOf(false) }
            val v = value.toDoubleOrNull()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value, onValueChange = { value = it },
                    label = { Text("成绩（$item.unit）· 手工录入真实实测值") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                SaveButton(
                    enabled = v != null, saved = saved,
                    onClick = {
                        if (v == null) return@SaveButton
                        saved = true
                        scope.launch {
                            val applicable = BodyTestStandardService.applicable(profileGender)
                            val (score, scoreValid) =
                                if (applicable) BodyTestStandardService.scoreFor(item.key, profileGender, v)
                                else 0 to false
                            repo.saveResult(BodyTestResult(
                                itemKey = item.key, ageGroup = "youth", gender = profileGender,
                                testDate = System.currentTimeMillis(),
                                rawValue = v, rawValue2 = 0.0, unit = item.unit,
                                source = "manual",
                                standardVersion = if (applicable) BodyTestStandardService.STANDARD_VERSION else "",
                                score = score, scoreValid = scoreValid,
                                note = "手工录入真实成绩"
                            ))
                            onSave()
                        }
                    }
                )
            }
        }
        else -> {
            Text("本项目暂仅记录备注，不支持数值录入。", fontSize = 12.sp, color = Palette.inkSoft)
        }
    }
}

/** 保存按钮 + 已保存反馈。 */
@Composable
private fun SaveButton(enabled: Boolean, saved: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.accentSoft, RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Check, null, tint = if (enabled) Palette.accent else Palette.inkSoft.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp))
        Text(
            if (saved) "已保存本次成绩 ✓" else "保存成绩",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (enabled) Palette.accent else Palette.inkSoft.copy(alpha = 0.5f)
        )
        Spacer(Modifier.weight(1f))
        if (!enabled) Text("请先填写数值", fontSize = 11.sp, color = Palette.inkSoft)
    }
}

/** 成绩趋势折线（Canvas；仅 >=2 条数据才画，任务 7 诚实）。 */
@Composable
fun TrendChart(
    history: List<BodyTestResult>,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val points = remember(history) { history.map { it.rawValue }.filter { it > 0.0 } }
    if (points.size < 2) {
        Text("数据不足", modifier = modifier, fontSize = 12.sp, color = Palette.inkSoft)
        return
    }
    val maxV = points.max()
    val minV = points.min()
    val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val stepX = size.width / points.size
        val ys = points.map { v ->
            val norm = ((v - minV) / range).toFloat()
            (size.height - norm * size.height).toFloat()
        }
        // 折线（逐段 drawLine）
        for (i in 0 until points.size - 1) {
            val x0 = i * stepX
            val x1 = (i + 1) * stepX
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(x0, ys[i]),
                end = androidx.compose.ui.geometry.Offset(x1, ys[i + 1]),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
        // 数据点
        points.forEachIndexed { i, _ ->
            drawCircle(
                color = color,
                radius = 8f,
                center = androidx.compose.ui.geometry.Offset(i * stepX, ys[i])
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        points.forEach { v ->
            Text("${"%.1f".format(v)}", fontSize = 10.sp, color = Palette.inkSoft)
        }
    }
}
