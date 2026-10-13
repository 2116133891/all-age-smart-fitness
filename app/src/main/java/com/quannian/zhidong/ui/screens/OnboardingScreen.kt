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
import androidx.compose.foundation.layout.width
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
import com.quannian.zhidong.model.AgeGroupConfig
import com.quannian.zhidong.ui.components.CoachVisual
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * V3.3 三张引导页（spec §2）：统一竖屏版式。
 *
 *  每页版式：顶部"跳过" → 上方小标签 → 中部醒目竖版数字教练全身 →
 *  数字人下方年龄段主标题（居中）→ 一句简短介绍 → 2~3 条短功能说明 →
 *  底部进度指示 + 主 CTA（前 2 页"下一步"，末页"开始体验"）。
 *
 *  - 三段内容固定（spec §2.2）：青少年 / 中青年 / 中老年。
 *  - 主名称取自 [AgeGroupConfig.band(id).title]（统一配置层，不写死）。
 *  - 数字人用 [CoachVisual] 按真实宽高比展示（Fit，不 fillBounds 拉伸）。
 *  - 整页结构对三页完全一致，只有文案/主色/角色素材不同。
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
    val current = pages[pagerState.currentPage]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(onboardBrush(current.color))
            .padding(horizontal = 24.dp)
    ) {
        // 顶部：小标签（左） + 跳过（右）
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "全龄智动 · 科学运动",
                fontSize = 13.sp, color = Palette.inkSoft, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp, modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFinish() }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text("跳过", fontSize = 14.sp, color = Palette.inkSoft, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // 中部：三页可滑动内容（竖版数字人主体）
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { index ->
            BoardPageView(page = pages[index])
        }

        // 底部：进度点 + 主 CTA
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.indices.forEach { i ->
                val sel = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (sel) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (sel) current.color else Color.White.copy(alpha = 0.5f))
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(current.color, RoundedCornerShape(16.dp))
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
                text = if (last) "开始体验" else "下一步 · ${pagerState.currentPage + 1}/3",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 单页：小标签下 → 竖版数字人主体 → 年龄段主标题 → 短简介 → 功能短句。 */
@Composable
private fun BoardPageView(page: BoardPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))

        // 数字人：占页面主体视觉面积（Fit 保持真实比例，不裁切/拉伸）。
        Box(
            modifier = Modifier.fillMaxWidth().weight(0.62f),
            contentAlignment = Alignment.Center
        ) {
            // 竖版角色容器：按 3:4 画布，ContentScale.Fit 让全身完整可见
            Image(
                painter = painterResource(CoachVisual.coachResource(page.ageId)),
                contentDescription = "数字教练 · ${AgeGroupConfig.ageLabel(page.ageId)}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(220.dp)
                    .height(290.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        // 数字人下方：年龄段主标题（居中）
        Pill(AgeGroupConfig.band(page.ageId).title, page.color)
        Spacer(Modifier.height(10.dp))

        Text(page.title, fontSize = 24.sp, color = Palette.ink, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            page.intro,
            fontSize = 14.sp, color = Palette.inkSoft,
            textAlign = TextAlign.Center, lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // 2~3 条短功能说明（每条 ≤12 字）
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0f)) {
            page.points.forEach { p ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        Modifier.size(6.dp).clip(CircleShape)
                            .background(page.color, CircleShape)
                    )
                    Text(p, fontSize = 14.sp, color = Palette.ink)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** 三页固定内容（spec §2.2）。主名称统一走 AgeGroupConfig。 */
private fun onboardingPages(): List<BoardPage> = listOf(
    BoardPage(
        ageId = "youth", color = Palette.youth,
        title = "科学训练，逐步提升",
        intro = "体测训练与动作指导，一步步练得更科学。",
        points = listOf("体测项目训练", "动作示范与跟练", "训练记录与计划")
    ),
    BoardPage(
        ageId = "middle", color = Palette.middle,
        title = "高效运动，改善状态",
        intro = "从力量、柔韧到肩颈舒缓，合理安排日常训练。",
        points = listOf("日常力量训练", "肩颈与柔韧改善", "个性化运动建议")
    ),
    BoardPage(
        ageId = "senior", color = Palette.senior,
        title = "科学活动，安心运动",
        intro = "从低强度活动开始，逐步改善平衡与活动能力。",
        points = listOf("八段锦与太极", "平衡与柔韧练习", "适合自身能力的训练")
    )
)

data class BoardPage(
    val ageId: String,
    val color: Color,
    val title: String,
    val intro: String,
    val points: List<String>
)

/** 全屏背景：白底 + 该年龄段主色顶部柔光渐变。 */
@Composable
internal fun onboardBrush(color: Color): androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            color.copy(alpha = 0.10f),
            Color(0xFFF5F7FB),
            Color(0xFFF5F7FB)
        ),
        startY = 0f,
        endY = 900f
    )
