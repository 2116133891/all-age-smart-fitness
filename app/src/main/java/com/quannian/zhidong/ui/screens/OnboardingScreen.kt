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
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.theme.Palette

/**
 * 全屏引导页（企业级 · 对标京东健康 onboarding）：
 *  3 页可滑动，每页用**对应年龄段数字人形象**（青年/中年/银龄，无儿童）主打"TA 能帮到你什么"。
 *  - 右上角"跳过"直达下一步。
 *  - 底部圆点指示 + 主 CTA（前 2 页"下一步"，末页"立即开始"）。
 *  - 整页沉浸式全屏（fillMaxSize + 品牌色渐变），不是小卡片。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onFinish: () -> Unit
) {
    val pages = remember { onboardingPages() }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.bg(pages[pagerState.currentPage].color)
            )
            .padding(horizontal = 28.dp)
    ) {
        // 顶部：跳过
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFinish() }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text("跳过", fontSize = 14.sp, color = Palette.inkSoft, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))

        // 全屏内容（3 页数字人）
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { index ->
            BoardPageView(page = pages[index])
        }

        // 指示点
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            pages.indices.forEach { i ->
                val sel = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (sel) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (sel) pages[i].color else Color.White.copy(alpha = 0.5f))
                )
            }
        }

        // 主 CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 28.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pages[pagerState.currentPage].color, RoundedCornerShape(16.dp))
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
                text = if (last) "立即开始体验" else "下一步 · ${pagerState.currentPage + 1}/3",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 单页：数字人 + 年龄段 + 主标题 + 三点作用。 */
@Composable
private fun BoardPageView(page: BoardPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(page.kicker, fontSize = 15.sp, color = page.color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(page.title, fontSize = 26.sp, color = Palette.ink, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(20.dp))
        // 数字人形象（全屏大尺寸）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            CoachAvatar(ageId = page.ageId, size = 260.dp)
        }
        Spacer(Modifier.height(12.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            page.points.forEach { p ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("●", color = page.color, fontSize = 10.sp)
                    Text(p, fontSize = 15.sp, color = Palette.ink, modifier = Modifier.weight(1f), lineHeight = 22.sp)
                }
            }
        }
    }
}

private fun onboardingPages(): List<BoardPage> = listOf(
    BoardPage("youth", Palette.youth, "青年 · 科学运动", "科学训练 · 体态纠正",
        listOf("AI 动作诊断 + 实时跟练计分", "深蹲 / 瑜伽 / 肩颈 规范四维评分", "前后对比，量化看见进步")),
    BoardPage("middle", Palette.middle, "中年 · 高效减压", "下班健身 · 肩颈缓解",
        listOf("下班黄金时段 50 分钟力量+有氧", "肩颈 / 腰椎 久坐减压专项", "心率区间监测，强度不超量")),
    BoardPage("senior", Palette.senior, "银龄 · 康养防跌", "舒缓运动 · 康复辅助",
        listOf("八段锦 / 太极 每日康养", "单腿站平衡训练，预防跌倒", "动作幅度按年龄自动放缓"))
)

data class BoardPage(
    val ageId: String,
    val color: Color,
    val kicker: String,
    val title: String,
    val points: List<String>
)

/** 全屏背景：白底 + 该年龄段主色的顶部柔光渐变，企业级沉浸感。 */
internal object Brush {
    @Composable
    fun bg(color: Color): androidx.compose.ui.graphics.Brush =
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = 0.10f),
                androidx.compose.ui.graphics.Color(0xFFF5F7FB),
                androidx.compose.ui.graphics.Color(0xFFF5F7FB)
            ),
            startY = 0f,
            endY = 900f
        )
}
