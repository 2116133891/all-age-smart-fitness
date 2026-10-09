package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.db.DailyStat
import com.quannian.zhidong.db.TrainingRepository
import com.quannian.zhidong.db.TrainingSession
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * "我的训练 / 成长曲线"页（对照 spec §22）。
 *
 *  顶部：累计统计（训练次数 / 次数 / 平均得分 / 时长）
 *  中部：7 天平均得分趋势折线（Compose Canvas 自绘，不引入第三方图表库）
 *  底部：最近训练记录列表
 *
 *  数据来自 [TrainingRepository]（Room 本地持久化，全离线）。
 */
@Composable
fun MyTrainingScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var summary by remember { mutableStateOf(TrainingRepository.Summary(0, 0, 0, 0, null)) }
    var dailyStats by remember { mutableStateOf<List<DailyStat>>(emptyList()) }
    var recent by remember { mutableStateOf<List<TrainingSession>>(emptyList()) }

    // 异步加载（Room suspend 调用走 IO 线程，不阻塞 UI）
    LaunchedEffect(Unit) {
        val repo = TrainingRepository(context.applicationContext)
        val s = withContext(Dispatchers.IO) { repo.summary() }
        val ds = withContext(Dispatchers.IO) { repo.dailyStats(7) }
        val r = withContext(Dispatchers.IO) { repo.recent(8) }
        summary = s
        dailyStats = ds
        recent = r
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink) }
                Text("我的训练", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("累计训练", "${summary.sessionCount} 次", Modifier.weight(1f))
                StatCard("总次数", "${summary.totalRepetitions}", Modifier.weight(1f))
                StatCard("平均得分", "${summary.avgScore}", Modifier.weight(1f))
                StatCard("总时长", "${summary.totalDurationSec / 60} 分", Modifier.weight(1f))
            }
        }

        item {
            Card() {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("最近一次训练", fontSize = 13.sp, color = Palette.inkSoft, modifier = Modifier.weight(1f))
                    val last = summary.lastSession
                    if (last != null) {
                        Text("${last.exerciseName} · ${last.score} 分", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
                    } else {
                        Text("暂无记录，快去完成一次训练吧", fontSize = 14.sp, color = Palette.inkSoft)
                    }
                }
            }
        }

        item {
            Card() {
                Text("最近 7 天成长趋势", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(10.dp))
                val dayCount = dailyStats.count { it.sessions > 0 }
                TrendChart(stats = dailyStats, hasData = dayCount > 0, dayCount = dayCount)
            }
        }

        item {
            Card() {
                Text("最近训练记录", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(10.dp))
                if (recent.isEmpty()) {
                    Text("还没有训练记录，完成一次跟练后会显示在这里。", fontSize = 13.sp, color = Palette.inkSoft)
                } else {
                    recent.forEach { s ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(s.exerciseName, fontSize = 14.sp, color = Palette.ink, modifier = Modifier.weight(1f))
                            Text("${s.durationSec}s", fontSize = 12.sp, color = Palette.inkSoft)
                            Text("${s.repetitions} 次", fontSize = 12.sp, color = Palette.inkSoft)
                            Pill("${s.score} 分", Palette.accent)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = Palette.inkSoft)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Palette.accent)
    }
}

/**
 * 7 天成长趋势折线图（Canvas 自绘，对照 spec §22）。
 *  横轴 = 最近 7 天（缺失用 0 补位），纵轴 = 平均得分 0-100。
 *  [hasData] 区分"有数据"与"无数据"两种空态，避免把单天/零天画成假增长曲线。
 */
@Composable
private fun TrendChart(
    stats: List<DailyStat>,
    modifier: Modifier = Modifier,
    hasData: Boolean = false,
    dayCount: Int = 0
) {
    val days = 7
    val data = remember(stats) {
        val byDay = stats.associate { it.day to it }
        val today = (System.currentTimeMillis() / 86400_000L).toLong()
        (0 until days).map { i ->
            val dayKey = today - (days - 1 - i).toLong()
            byDay[dayKey]?.avgScore?.toFloat() ?: 0f
        }
    }

    if (!hasData) {
        // 空态：无数据 / 仅 1 天数据时，不画折线（避免误导为"大幅增长"）
        Box(
            modifier = modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            if (dayCount == 1) {
                Text("当前只有 1 天记录，继续训练会看到成长曲线", fontSize = 13.sp, color = Palette.inkSoft)
            } else {
                Text("暂无训练数据，完成 1 次跟练后自动记录", fontSize = 13.sp, color = Palette.inkSoft)
            }
        }
        return
    }

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val w = size.width
        val h = size.height
        val padL = 24f
        val padB = 16f
        val padT = 12f
        val padR = 12f
        val plotW = w - padL - padR
        val plotH = h - padT - padB

        // 网格
        for (v in 0..4) {
            val yy = padT + plotH * (1 - v / 4f)
            drawLine(Color(0xFFE4E9F2), Offset(padL, yy), Offset(w - padR, yy), strokeWidth = 1f)
        }

        // 只显示有数据的天；无数据天跳过，不画假线
        val realPts = mutableListOf<Offset>()
        val dayIdxOf = IntArray(days) { -1 } // 有数据的天在 realPts 里的位置
        var ptCount = 0
        for (i in 0 until days) {
            if (data[i] > 0f) {
                val xx = padL + plotW * i / (days - 1)
                val yy = padT + plotH * (1 - data[i] / 100f)
                dayIdxOf[i] = ptCount
                realPts += Offset(xx, yy)
                ptCount++
            }
        }

        // 相邻有数据的天才连线（中间空天 → 断线）
        for (i in 0 until days - 1) {
            if (data[i] > 0f && data[i + 1] > 0f) {
                val pa = realPts[dayIdxOf[i]]
                val pb = realPts[dayIdxOf[i + 1]]
                drawLine(Palette.accent, pa, pb, strokeWidth = 3f, cap = StrokeCap.Round)
            }
        }

        // 数据点
        realPts.forEach { p ->
            drawCircle(Palette.accent, 5f, p)
            drawCircle(Color.White, 2.5f, p)
        }
    }
}
