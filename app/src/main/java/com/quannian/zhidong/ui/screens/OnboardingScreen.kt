package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.quannian.zhidong.model.AgeGroupConfig
import com.quannian.zhidong.ui.components.CoachVisual
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette

/**
 * V3.5 三张引导页（spec P0-4）：统一竖屏版式，大居中竖版透明教练为主视觉。
 *
 *  每页版式：右上角"跳过" → 中部大居中竖版透明全身教练（Fit，不裁切头脚）→
 *  数字人下方年龄段主标题（Pill，居中）→ 一句简短介绍 → 2~3 条短功能说明 →
 *  底部进度指示 + 主 CTA（前 2 页"下一步"，末页"开始体验"）。
 *
 *  - 顶部只保留右上角"跳过"，去掉冗余年龄/品牌小标签。
 *  - 三页使用同一容器比例（立绘 736:1312，Fit 缩放），仅文案/主色/立绘素材不同。
 *  - 大居中竖版透明立绘用 [CoachVisual.CoachPortrait]（真透明 alpha + 全身居中 + Fit）。
 *  - 整页结构对三页完全一致，保证窄屏/大系统字体下主 CTA 始终可见。
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
    // P0-C/P1-E：引导页立绘跟随用户所选教练风格（REALISTIC→真人立绘 / ANIMATED→3D 卡通）。
    val context = androidx.compose.ui.platform.LocalContext.current
    val coachStyle = remember { com.quannian.zhidong.model.CoachStyleStore.load(context) }
    val last = pagerState.currentPage == pages.lastIndex
    val current = pages[pagerState.currentPage]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(onboardBrush(current.color))
            .padding(horizontal = 24.dp)
    ) {
        // 顶部：仅右上角"跳过"（去掉冗余小标签）
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
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

        Spacer(Modifier.height(6.dp))

        // 中部：三页可滑动内容（大居中竖版透明教练主体）
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { index ->
            BoardPageView(page = pages[index], coachStyle = coachStyle)
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

/** 单页：大居中竖版透明教练主体 → 年龄段主标题 → 短简介 → 功能短句。 */
@Composable
private fun BoardPageView(page: BoardPage, coachStyle: com.quannian.zhidong.model.CoachStyle) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(4.dp))

        // 大居中竖版透明教练：占页面上部主体，Fit 保持立绘真实比例（不裁切/拉伸）。
        // 三页同一容器比例（立绘 736:1312），高度随可用空间自适应并封顶。
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(0.52f),
            contentAlignment = Alignment.Center
        ) {
            // maxHeight 已是 Dp（ConstrainedLayoutScope 扩展属性），直接比例换算
            val portraitH = minOf(430.dp, maxOf(240.dp, maxHeight * 0.88f))
            CoachVisual.CoachPortrait(
                ageId = page.ageId,
                modifier = Modifier,
                size = portraitH,
                style = coachStyle
            )
        }

        Spacer(Modifier.height(10.dp))

        // 数字人下方：年龄段主标题（Pill 居中，统一配置层，不写死）
        Pill(AgeGroupConfig.band(page.ageId).title, page.color)
        Spacer(Modifier.height(8.dp))

        Text(
            page.title,
            fontSize = 22.sp,
            color = Palette.ink,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            page.intro,
            fontSize = 14.sp,
            color = Palette.inkSoft,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        // 2~3 条短功能说明（每条 ≤12 字）。
        // 注意：Compose 的 weight 必须 > 0；此处用自然高度（wrap content），不占剩余权重。
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

/** 三页固定内容（spec P0-4 指定文案）。主名称统一走 AgeGroupConfig。 */
private fun onboardingPages(): List<BoardPage> = listOf(
    BoardPage(
        ageId = "youth", color = Palette.youth,
        title = "科学训练，逐步提升",
        intro = "体测训练与动作指导，让每一步练习更有方向。",
        points = listOf("体测项目训练", "动作示范与跟练", "记录每次进步")
    ),
    BoardPage(
        ageId = "middle", color = Palette.middle,
        title = "高效运动，改善状态",
        intro = "灵活安排力量、柔韧与日常运动。",
        points = listOf("力量与体能", "肩颈与柔韧", "个性化运动建议")
    ),
    BoardPage(
        ageId = "senior", color = Palette.senior,
        title = "科学活动，安心运动",
        intro = "从适合自身的活动开始，循序渐进。",
        points = listOf("八段锦与太极", "平衡与柔韧", "安全运动指导")
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
