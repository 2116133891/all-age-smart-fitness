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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.coach.LocalCoachProvider
import com.quannian.zhidong.db.TrainingRepository
import com.quannian.zhidong.model.TrainingReport
import com.quannian.zhidong.repository.ExerciseRepository
import com.quannian.zhidong.repository.ReportChannel
import com.quannian.zhidong.tts.TtsCoach
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 训练报告页（竞赛版，对照 spec §23）。
 *
 *  布局：
 *   - 顶部 "训练完成 🎉"
 *   - 训练动作 / 完成次数 / 综合评分 / 动作质量
 *   - 四维细分（幅度 / 姿态 / 稳定 / 完整）
 *   - AI 教练总结（LocalCoachProvider 生成）
 *   - 下一次训练建议
 *   - 表现良好 / 建议改进
 *   - 再次训练 / 返回首页
 *
 *  数据来自 [ReportChannel]（跟练页结束时写入），并自动持久化到 Room。
 */
@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    onAgain: () -> Unit,
    onHome: () -> Unit
) {
    val report = remember { ReportChannel.take() } ?: return
    val context = androidx.compose.ui.platform.LocalContext.current

    // 自动持久化到 Room（后台线程，不阻塞 UI）
    LaunchedEffect(report) {
        withContext(Dispatchers.IO) {
            try {
                TrainingRepository(context.applicationContext)
                    .save(report, exerciseId = ExerciseRepository.all().firstOrNull { it.name == report.exerciseName }?.id ?: "", report.exerciseName)
            } catch (_: Exception) {
            }
        }
    }

    val coach = remember(report) { LocalCoachProvider }
    val summary = remember(report) { if (report.coachSummary.isNotBlank()) report.coachSummary else coach.generateSummary(report) }
    val nextSuggestion = remember(report) { if (report.nextSuggestion.isNotBlank()) report.nextSuggestion else coach.nextSuggestion(report) }

    // TTS：若开启则朗读总结
    LaunchedEffect(summary) {
        if (TtsCoach.isEnabled) TtsCoach.speak("本次训练已完成，综合评分 ${report.overallScore} 分。$summary")
    }

    val qualityLabel = when {
        report.overallScore >= 85 -> "优秀"
        report.overallScore >= 70 -> "良好"
        report.overallScore >= 50 -> "合格"
        else -> "待改进"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 顶部 "训练完成 🎉"
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 40.sp)
                Spacer(Modifier.height(6.dp))
                Text("训练完成", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
                Text(report.exerciseName, fontSize = 16.sp, color = Palette.inkSoft)
            }
        }

        // 顶部数据：次数 + 综合评分 + 动作质量
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricCard("完成次数", "${report.repCount}/${report.targetReps}", Modifier.weight(1f))
                MetricCard("综合评分", "${report.overallScore}", Modifier.weight(1f))
                MetricCard("动作质量", qualityLabel, Modifier.weight(1f))
            }
        }

        // 四维细分
        item {
            Card() {
                Text("动作分项评分", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(10.dp))
                DimRow("幅度", report.rangeScore)
                Spacer(Modifier.height(8.dp))
                DimRow("姿态", report.postureScore)
                Spacer(Modifier.height(8.dp))
                DimRow("稳定", report.stabilityScore)
                Spacer(Modifier.height(8.dp))
                DimRow("完整", report.completenessScore)
            }
        }

        // AI 教练总结
        item {
            Card() {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎤", fontSize = 16.sp)
                    Text("AI 教练总结", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.accent)
                }
                Spacer(Modifier.height(8.dp))
                Text(summary, fontSize = 14.sp, color = Palette.ink, lineHeight = 20.sp)
            }
        }

        // 下一次训练建议
        item {
            Card() {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡", fontSize = 16.sp)
                    Text("下一次训练建议", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.warn)
                }
                Spacer(Modifier.height(8.dp))
                Text(nextSuggestion, fontSize = 14.sp, color = Palette.ink, lineHeight = 20.sp)
            }
        }

        // 表现良好
        item {
            Card() {
                Text("表现良好", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.good)
                Spacer(Modifier.height(8.dp))
                report.highlights.forEach { h ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CheckCircle, "", tint = Palette.good, modifier = Modifier.size(18.dp))
                        Text(h, fontSize = 14.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // 建议改进
        item {
            Card() {
                Text("建议改进", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.warn)
                Spacer(Modifier.height(8.dp))
                report.improvements.forEach { imp ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Warning, "", tint = Palette.warn, modifier = Modifier.size(18.dp))
                        Text(imp, fontSize = 14.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // 行动按钮
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.accent, RoundedCornerShape(16.dp))
                        .clickable(onClick = onAgain)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("再次训练", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.ink, RoundedCornerShape(16.dp))
                        .clickable(onClick = onHome)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("返回首页", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/** 数据小卡片。 */
@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = Palette.inkSoft)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.accent)
    }
}

/** 四维细分行（标签 + 进度条 + 数值）。 */
@Composable
private fun DimRow(label: String, score: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(label, fontSize = 13.sp, color = Palette.inkSoft, modifier = Modifier.width(36.dp))
        DimBar(score, Modifier.weight(1f))
        Text("${score.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink, modifier = Modifier.width(32.dp))
    }
}

@Composable
private fun DimBar(score: Float, modifier: Modifier = Modifier) {
    val progress = (score / 100f).coerceIn(0f, 1f)
    val color = when {
        score >= 85f -> Palette.good
        score >= 70f -> Palette.accent
        score >= 50f -> Palette.warn
        else -> Palette.bad
    }
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Palette.line, RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color, RoundedCornerShape(4.dp))
        )
    }
}

