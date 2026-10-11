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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.db.BodyTestRepository
import com.quannian.zhidong.db.BodyTestResult
import com.quannian.zhidong.model.BodyTestItemKey
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * V2 中年 / 银龄 健康 管理 页（任务 8，复用 现有 数据 模型 与 仓库，不 复制 架构）。
 *  - 中年：体重 / BMI 历史 记录 + 低 冲击 有氧 / 基础 力量 / 柔韧 快 捷 入口 + 阶段 目标
 *  - 银龄：大 字体 / 大 按钮 / 清晰 反馈 + 训练 前 热身 / 平衡 / 安全 提醒 + 太极 / 八段锦
 *  - 明确 区分 动作 教学 与 医疗 建议（银龄 强制 医疗 免责 条）
 *  复用 [BodyTestRepository]（itemKey=BMI / VITAL_CAPACITY，ageGroup=middle/senior）记 体重 / BMI 历史。
 */
@Composable
fun AgeHealthScreen(
    ageId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isSenior = ageId == "senior"
    // 银龄 大 字体 / 大 按钮 系数（任务 8 明确要求）
    val fontScale = if (isSenior) 1.25f else 1.0f
    val titleSize = (20 * fontScale).sp

    val repo = remember { BodyTestRepository(context) }
    var history by remember { mutableStateOf<List<BodyTestResult>>(emptyList()) }
    var revision by remember { mutableStateOf(0) }
    LaunchedEffect(revision) { history = repo.history(BodyTestItemKey.BMI).filter { it.ageGroup == ageId } }

    var hStr by remember { mutableStateOf("") }
    var wStr by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val h = hStr.toDoubleOrNull()
    val w = wStr.toDoubleOrNull()
    val bmi = if (h != null && w != null && h > 0) w / (h * h) else null

    val bmiColor = Palette.ageColor(ageId)

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
                    Text(
                        if (isSenior) "银龄 · 科学 运动 健康" else "中年 · 健康 管理",
                        fontSize = titleSize, fontWeight = FontWeight.Bold, color = Palette.ink
                    )
                    Text(if (isSenior) "太极 / 八段锦 · 平衡 · 安全 运动" else "体重 / 基础 体能 / 柔韧 / 日常 运动",
                        fontSize = (13 * fontScale).sp, color = Palette.inkSoft)
                }
            }
        }

        // 安全 / 教学 免责（银龄 强制 医疗 免责 条，任务 8）
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Info, null, tint = bmiColor, modifier = Modifier.size(22.dp))
                    Text("教学 ≠ 医疗 建议", fontSize = (14 * fontScale).sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (isSenior)
                        "本页 为 动作 教学 与 运动 辅助，**不 构成 医疗 建议**。有 高血压 / 心脏病 / 糖尿病 / 关节 疾病 等 基础 病，或 头晕 / 胸闷 / 明显 疼痛 时，请 先 咨询 医生 再 运动；跌倒 风险 高 者 全程 扶 物。"
                    else
                        "本页 为 运动 与 体重 管理 辅助，**不 构成 医疗 诊断**。有 基础 疾病 请 先 咨询 医生。",
                    fontSize = (12 * fontScale).sp, color = Palette.inkSoft, lineHeight = (16 * fontScale).sp
                )
            }
        }

        // 银龄 训练 前 热身 / 平衡 / 安全 提醒（任务 8）
        if (isSenior) {
            item {
                Card() {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Warning, null, tint = Palette.warn, modifier = Modifier.size(22.dp))
                        Text("训练 前 热身 · 平衡 · 安全", fontSize = (14 * fontScale).sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    }
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SeniorTip("热身", "慢 走 / 关节 活动 5 分钟，再 进 入 太极 / 八段锦。", bmiColor)
                        SeniorTip("平衡", "单 腿 站 / 串联 走 全程 扶 物；头晕 立即 停。", Palette.warn)
                        SeniorTip("安全", "穿 防滑 鞋，地 面 干 燥，旁 置 椅 子 防 跌倒。", Palette.bad)
                    }
                }
            }
        }

        // 体重 / BMI 历史 记录（中年 + 银龄 通用，任务 8）
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.FitnessCenter, null, tint = bmiColor, modifier = Modifier.size(22.dp))
                    Text("体重 / BMI 历史", fontSize = (15 * fontScale).sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Spacer(Modifier.weight(1f))
                    if (bmi != null) Pill("BMI ${"%.1f".format(bmi)}", bmiColor)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hStr, onValueChange = { hStr = it },
                        label = { Text("身高 (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = wStr, onValueChange = { wStr = it },
                        label = { Text("体重 (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(bmiColor, RoundedCornerShape(14.dp))
                        .clickable {
                            val bh = hStr.toDoubleOrNull(); val bw = wStr.toDoubleOrNull()
                            if (bh != null && bw != null && bh > 0) {
                                scope.launch {
                                    repo.saveResult(BodyTestResult(
                                        itemKey = BodyTestItemKey.BMI,
                                        ageGroup = ageId,
                                        gender = "unknown",
                                        testDate = System.currentTimeMillis(),
                                        rawValue = bw / (bh * bh),
                                        heightCm = bh, weightKg = bw,
                                        unit = "kg/m²", source = "manual",
                                        note = "${ageId} 体重/BMI 管理"
                                    ))
                                    revision++
                                }
                            }
                        }
                        .padding(vertical = (if (isSenior) 18.dp else 14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (bmi != null) "保存 体重 / BMI 记录" else "请 先 填写 身高 体重",
                        fontSize = (15 * fontScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
                // 趋势（真实 数据）
                Spacer(Modifier.height(10.dp))
                Text("历史 记录（${history.size} 条）", fontSize = (13 * fontScale).sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                if (history.isEmpty()) {
                    Text("暂 无 记录。完成 更多 测量 后 生成 趋势。", fontSize = (12 * fontScale).sp, color = Palette.inkSoft)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        history.takeLast(5).forEach { r ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .background(Palette.cardSoft, RoundedCornerShape(8.dp)).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${r.weightKg}kg / ${r.heightCm}cm", fontSize = (12 * fontScale).sp, color = Palette.ink)
                                Text("BMI ${"%.1f".format(r.rawValue)}", fontSize = (12 * fontScale).sp, color = bmiColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 快捷 训练 入口（中年：低 冲击 有氧/力量/柔韧；银龄：太极/八段锦 大 按钮）
        item {
            Text(if (isSenior) "推荐 动作（循序渐进）" else "推荐 训练",
                fontSize = (15 * fontScale).sp, fontWeight = FontWeight.Bold, color = Palette.ink)
        }
        (if (isSenior)
            listOf("八段锦 · 8 式", "太极 · 起势 推掌", "平衡 防跌", "舒缓 拉伸")
            else
            listOf("低 冲击 有氧 · 快走", "基础 力量 · 深蹲/划船", "柔韧性 · 拉伸", "日常 减脂 · 间歇"))
            .forEach { t ->
                item {
                    QuickExerciseRow(title = t, large = isSenior, color = bmiColor)
                }
            }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SeniorTip(label: String, text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(label, color)
        Text(text, fontSize = 15.sp, color = Palette.inkSoft, modifier = Modifier.weight(1f), lineHeight = 20.sp)
    }
}

@Composable
private fun QuickExerciseRow(title: String, large: Boolean, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .padding(if (large) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = (if (large) 17 else 14).sp, fontWeight = FontWeight.Bold, color = Palette.ink)
        Text("进入 教学 →", fontSize = (if (large) 15 else 12).sp, color = color)
    }
}
