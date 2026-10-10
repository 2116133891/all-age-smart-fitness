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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.model.ProfileStore
import com.quannian.zhidong.ui.theme.Palette

/** 一道体测自评题。 */
data class TestItem(val text: String, val hint: String)

/** 年龄段定制的体测题目（青年体测 / 中年肩颈评估 / 银龄康复评估）。 */
fun bodyTestItems(ageId: String): List<TestItem> = when (ageId) {
    "middle" -> listOf(
        TestItem("能否轻松完成 20 个标准深蹲（膝盖不内扣）？", "下肢力量 · 关节稳定"),
        TestItem("单腿闭眼站立能保持 30 秒以上？", "平衡能力 · 防跌倒"),
        TestItem("肩部能把直臂举过头顶并外展 90°？", "肩颈灵活度 · 办公减压"),
        TestItem("30 秒内跳绳/开合跳能超 40 次？", "心肺耐力 · 燃脂能力"),
        TestItem("久坐 2 小时后肩颈是否有明显僵硬？", "久坐劳损自测")
    )
    "senior" -> listOf(
        TestItem("从椅子起身时是否需要撑扶？", "下肢力量 · 康复"),
        TestItem("单腿站立（可扶物）能保持 10 秒？", "平衡 · 防跌倒"),
        TestItem("头部能缓慢左右侧倾不头晕？", "颈椎灵活 · 舒缓"),
        TestItem("握力是否较一年前明显下降？", "肌力维持 · 康复"),
        TestItem("快走 10 分钟是否气短明显？", "心肺 · 康方向")
    )
    else -> listOf(
        TestItem("1 分钟跳绳能超 60 个？", "心肺耐力"),
        TestItem("标准深蹲 15 次，膝盖朝脚尖不内扣？", "下肢力量 · 体态"),
        TestItem("坐姿前屈指尖能过脚尖？", "柔韧性"),
        TestItem("平板支撑能保持 60 秒？", "核心稳定"),
        TestItem("肩颈久坐 4 小时后是否明显酸紧？", "久坐劳损自测")
    )
}

/**
 * 体测页（"测"模块）：按年龄段定制自测题，逐题 1~5 星自评 → 综合分 + 等级 + 针对性建议。
 *  与"练"（跟练计分）互补：练看动作规范，测看体能基线，形成"测→练→再测"闭环。
 */
@Composable
fun BodyTestScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onGoTrain: () -> Unit
) {
    val context = LocalContext.current
    val profile = remember { ProfileStore.load(context) }
    val ageId = profile.ageId
    val items = remember(ageId) { bodyTestItems(ageId) }
    val color = Palette.ageColor(ageId)

    // 每题评分 0(未评) / 1..5
    val scores = remember { Array(items.size) { mutableIntStateOf(0) } }

    // 综合分（0-100）与等级
    val rated = scores.count { it.intValue > 0 }
    val scored = scores.filter { it.intValue > 0 }.map { it.intValue }
    val overall = if (scored.isEmpty()) 0 else (scored.sum() * 20 / 5 / scored.size)
    val level = when {
        overall == 0 -> "未完成"
        overall >= 85 -> "优秀"
        overall >= 70 -> "良好"
        overall >= 50 -> "达标"
        else -> "待提升"
    }
    val weak = items.indices
        .filter { scores[it].intValue in 1..2 }
        .mapNotNull { idx -> items[idx].hint }
        .firstOrNull()

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
                Text("体测 · ${ProfileStore.ageLabel(ageId)}", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.weight(1f))
            }
            Text("逐项自评（1=很弱 · 5=很强），看看你的基线", fontSize = 13.sp, color = Palette.inkSoft)
        }

        // 结果卡
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(overall.toString(), fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = color)
                    Text("综合分", fontSize = 12.sp, color = Palette.inkSoft)
                }
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val (gColor, grade) = when (level) {
                            "优秀" -> Color(0xFF21B57A) to "优秀"
                            "良好" -> color to "良好"
                            "达标" -> Color(0xFFF5A623) to "达标"
                            else -> Palette.inkSoft to level
                        }
                        Text("等级：$grade", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = gColor)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (weak != null) "短板集中在：$weak，建议优先针对性练习。"
                        else if (overall > 0) "基线不错，保持节奏、逐步进阶。"
                        else "完成下面 5 项自评，即可生成你的体测报告。",
                        fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 17.sp
                    )
                }
            }
        }

        // 自评题
        items(items.size) { i ->
            val item = items[i]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Palette.card, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text((i + 1).toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
                    Text(item.hint, fontSize = 11.sp, color = Palette.inkSoft)
                }
                Spacer(Modifier.height(6.dp))
                Text(item.text, fontSize = 14.sp, color = Palette.ink, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    (1..5).forEach { star ->
                        val sel = scores[i].intValue >= star
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) color.copy(alpha = 0.18f) else Palette.cardSoft, RoundedCornerShape(10.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { scores[i].intValue = if (sel && scores[i].intValue == star) star - 1 else star }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (sel) "★" else star.toString(),
                                fontSize = 14.sp,
                                color = if (sel) color else Palette.inkSoft,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 行动
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.accent, RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onGoTrain() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("根据结果去跟练（练模块）", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
