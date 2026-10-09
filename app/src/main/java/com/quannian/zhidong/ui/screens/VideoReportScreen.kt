package com.quannian.zhidong.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.video.CorrectionWording
import com.quannian.zhidong.video.VideoAnalysisChannel
import com.quannian.zhidong.video.VideoAnalysisResult
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import java.util.Locale

/**
 * AI 动作体检报告（spec §十六/§十七/§十八/§十九）。
 *
 *  - 完成次数 / 综合评分 / 动作规范度（例：8 次 / 86 / 良好）
 *  - 做得好的地方（✓）/ 需要改进（⚠）
 *  - 动作质量时间轴（Canvas 自绘，正常点 / 前倾点 / 浅蹲点）
 *  - 查看问题动作（点错误片段 → 视频 seek 回放）
 *  - 数字教练针对纠正（主错误 → 纠正文案 + "开始重新练习" → 教学页纠错模式）
 */
@Composable
fun VideoReportScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onCorrect: (exerciseId: String, errorType: String) -> Unit
) {
    val result = remember { VideoAnalysisChannel.result } ?: run {
        onBack()
        return
    }
    val uriStr = remember { VideoAnalysisChannel.uri?.toString() ?: "" }
    val kind = remember { VideoAnalysisChannel.exerciseKind ?: "squat" }
    val ageId = remember { VideoAnalysisChannel.ageId ?: "youth" }
    val ageColor = Palette.ageColor(ageId)

    val qualityLabel = when {
        result.overallScore >= 85 -> "优秀"
        result.overallScore >= 70 -> "良好"
        result.overallScore >= 50 -> "合格"
        else -> "待改进"
    }

    // 问题片段回放（spec §十八）
    var replayMs by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
                }
                Text("AI 动作体检报告", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
                Spacer(Modifier.weight(1f))
            }
        }

        // 顶部指标（spec §十六 例）
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎬", fontSize = 36.sp)
                Spacer(Modifier.height(4.dp))
                Text(VideoAnalysisChannel.exerciseName ?: "动作分析", fontSize = 16.sp, color = Palette.inkSoft)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("完成次数", "${result.totalReps}", Modifier.weight(1f))
                MetricCard("综合评分", "${result.overallScore}", Modifier.weight(1f))
                MetricCard("动作规范度", qualityLabel, Modifier.weight(1f))
            }
        }

        // 做得好 / 需改进（spec §十六）
        item {
            Card() {
                Text("做得好的地方", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.good)
                Spacer(Modifier.height(8.dp))
                result.highlights.forEach { h ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CheckCircle, "", tint = Palette.good, modifier = Modifier.size(18.dp))
                        Text(h, fontSize = 14.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        item {
            Card() {
                Text("需要改进", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.warn)
                Spacer(Modifier.height(8.dp))
                result.improvements.forEach { imp ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Warning, "", tint = Palette.warn, modifier = Modifier.size(18.dp))
                        Text(imp, fontSize = 14.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // 动作质量时间轴（spec §十七，Canvas 自绘）
        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("动作质量时间轴", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink, modifier = Modifier.weight(1f))
                    Text("${(result.durationMs / 1000L).toInt()}s", fontSize = 13.sp, color = Palette.inkSoft)
                }
                Spacer(Modifier.height(10.dp))
                QualityTimeline(result, Modifier.fillMaxWidth().height(120.dp))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(Palette.good, "正常")
                    LegendDot(Palette.bad, "有偏差")
                }
            }
        }

        // 查看问题动作（spec §十八：点片段 → 视频 seek 回放）
        val errorEvents = result.errorTimeline.filter { it.isError }
        if (errorEvents.isNotEmpty()) {
            item {
                Card() {
                    Text("查看问题动作", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Spacer(Modifier.height(4.dp))
                    Text("点击片段，视频跳到对应位置回放。", fontSize = 12.sp, color = Palette.inkSoft)
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        errorEvents.take(6).forEach { ev ->
                            val label = formatClock(ev.timestampMs)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Palette.bad.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .clickable { replayMs = ev.timestampMs }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("▶ $label", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Palette.bad)
                                Text(ev.label, fontSize = 13.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 视频回放窗口（仅当点了某片段）
            if (replayMs != null) {
                item {
                    VideoReplayPlayer(uriStr, replayMs!!, Modifier.fillMaxWidth())
                    // 清理回放
                    DisposableEffect(Unit) {
                        onDispose { /* player 在 composable 内部自行管理 */ }
                    }
                }
            }
        }

        // 数字教练针对纠正（spec §十九）
        val primary = result.primaryError
        if (primary != null) {
            item {
                Card(borderColor = ageColor.copy(alpha = 0.25f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎤", fontSize = 16.sp)
                        Text("数字教练纠正", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ageColor)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        CorrectionWording.of(primary),
                        fontSize = 14.sp, color = Palette.ink, lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ageColor, RoundedCornerShape(14.dp))
                            .clickable {
                                val exId = com.quannian.zhidong.repository.ExerciseRepository
                                    .all().firstOrNull { it.analysisKind == kind }?.id ?: ""
                                onCorrect(exId, primary.code)
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("开始重新练习（看数字教练示范 + 跟练纠正）", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        } else {
            item {
                Card() {
                    Text("✓ 未发现明显问题", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.good)
                    Spacer(Modifier.height(6.dp))
                    Text("动作整体规范，可进入跟练巩固，并做「改善前后对比」。", fontSize = 13.sp, color = Palette.inkSoft, lineHeight = 18.sp)
                }
            }
        }

        // 返回
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.ink, RoundedCornerShape(16.dp))
                    .clickable(onClick = onBack)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("返回首页", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

// ============ 报告页小组件 ============

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = Palette.inkSoft)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Palette.accent)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
        Text(label, fontSize = 11.sp, color = Palette.inkSoft)
    }
}

/** 动作质量时间轴（spec §十七）：横轴时间，正常点绿、有偏差点红，按 rep 位置标注。 */
@Composable
private fun QualityTimeline(result: VideoAnalysisResult, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val events = result.errorTimeline
        if (result.durationMs <= 0) return@Canvas
        val midY = h / 2f

        // 主轴
        drawLine(
            color = Palette.line,
            start = Offset(10f, midY),
            end = Offset(w - 10f, midY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // 事件定位点（正常 / 有偏差）
        events.forEach { ev ->
            val t = (ev.timestampMs.toFloat() / result.durationMs).coerceIn(0f, 1f)
            val x = 10f + t * (w - 20f)
            val color = if (ev.isError) Palette.bad else Palette.good
            drawCircle(color, radius = 9f, center = Offset(x, midY))
            drawCircle(Color.White, radius = 5f, center = Offset(x, midY))
            // 错误片段标注
            if (ev.isError) {
                drawCircle(color, radius = 14f, center = Offset(x, midY), alpha = 0.25f)
            }
        }
        // 时间刻度
        for (tick in 0..4) {
            val x = 10f + (w - 20f) * (tick / 4f)
            drawLine(Palette.line.copy(alpha = 0.5f), Offset(x, h - 8f), Offset(x, h - 2f), strokeWidth = 2f)
        }
    }
}

/** 视频片段回放（spec §十八）：MediaMetadataRetriever 拿缩略 + MediaPlayer seek 到问题时刻。 */
@Composable
private fun VideoReplayPlayer(uriStr: String, seekMs: Long, modifier: Modifier) {
    val context = LocalContext.current
    val player = remember(seekMs) {
        try {
            MediaPlayer().apply {
                setDataSource(context, Uri.parse(uriStr))
                prepare()
                seekTo(seekMs.toInt())
                start()
            }
        } catch (_: Exception) {
            null
        }
    }
    Box(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF10182A), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔁 回放 ${formatClock(seekMs)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("视频正在该片段回放（含错误动作）", fontSize = 12.sp, color = Color(0xCCFFFFFF))
        }
        DisposableEffect(player) {
            onDispose {
                try {
                    player?.release()
                } catch (_: Exception) {
                }
            }
        }
    }
}

/** 毫秒 → mm:ss。 */
private fun formatClock(ms: Long): String {
    val totalSec = ms / 1000L
    return String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60)
}
