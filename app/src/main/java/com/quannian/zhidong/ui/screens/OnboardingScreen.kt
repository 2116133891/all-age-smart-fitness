package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

/**
 * 引导页：介绍 App 能做什么。
 *  横向滑动 6 张卡（青年×2 / 中年×2 / 银龄×2，覆盖"体测·日常训练 / 下班健身·减压 / 日常康养·康复"），
 *  逐张左滑浏览，告诉三类人群各自"能得到什么"，末页进入 App。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onFinish: () -> Unit
) {
    val pages = rememberPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg)
            .padding(horizontal = 24.dp, vertical = 30.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { index ->
            OnboardingPage(page = pages[index])
        }

        Spacer(Modifier.height(16.dp))
        // 指示点
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            pages.indices.forEach { i ->
                val selected = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (selected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (selected) Palette.accent else Palette.line)
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        val last = pagerState.currentPage == pages.lastIndex
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.accent, RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!last) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    else onFinish()
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (last) "开始体验全龄智动" else "下一步 · ${pagerState.currentPage + 1}/${pages.size}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun rememberPages(): List<BoardPage> {
    val y = Palette.youth
    val m = Palette.middle
    val s = Palette.senior
    return listOf(
        BoardPage("青年 · 体测 & 日常训练", "科学运动 · 看见进步", y, "📊",
            listOf("AI 动作诊断 + 实时跟练计分", "深蹲 / 开合跳 / 瑜伽 规范评分", "运动前后对比，量化进步")),
        BoardPage("青年 · 体态纠正", "久坐族救星", y, "🙆",
            listOf("肩颈拉伸专项，缓解办公酸痛", "四维评分盯姿势（幅度/躯干/对称/完整）", "数字教练示范，动作不再瞎练")),
        BoardPage("中年 · 下班健身", "高效燃脂 & 减压", m, "🏋️",
            listOf("下班黄金时段 50 分钟力量+有氧方案", "肩颈 / 腰椎 办公久坐专项", "心率区间监测，强度不超量")),
        BoardPage("中年 · 压力管理", "工作生活平衡", m, "⚖️",
            listOf("5 分钟肩颈减压小练，工位即做", "助眠拉伸，运动改善睡眠", "碎片时间运动，不挤占通勤")),
        BoardPage("银龄 · 日常康养", "缓而稳 · 防跌倒", s, "🍃",
            listOf("八段锦 / 太极 传统功法每日养", "单腿站平衡训练，防跌倒", "动作幅度按年龄自动放缓")),
        BoardPage("银龄 · 康复辅助", "关节友好", s, "🌿",
            listOf("膝盖 / 肩关节低冲击版本", "康复向评估（稳·柔·对称）", "基础病量力而行，先问医生"))
    )
}

/** 单张引导卡：年龄段色条 + 主标题 + 三点作用。 */
@Composable
private fun OnboardingPage(page: BoardPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(page.color.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                .padding(28.dp)
        ) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(page.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(page.emoji, fontSize = 30.sp)
                    }
                    Column {
                        Text(page.group, fontSize = 12.sp, color = page.color, fontWeight = FontWeight.SemiBold)
                        Text(page.title, fontSize = 21.sp, color = Palette.ink, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(22.dp))
                page.points.forEach { p ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("●", color = page.color, fontSize = 12.sp)
                        Text(
                            p,
                            fontSize = 15.sp,
                            color = Palette.ink,
                            modifier = Modifier.weight(1f),
                            lineHeight = 22.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

data class BoardPage(
    val group: String,
    val title: String,
    val color: Color,
    val emoji: String,
    val points: List<String>
)
