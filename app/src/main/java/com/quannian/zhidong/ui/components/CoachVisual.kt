package com.quannian.zhidong.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.R
import com.quannian.zhidong.ui.theme.Palette

// ============================================================================
// 数字教练 · 视觉资源（全项目唯一映射点）
// ============================================================================
//
//  Phase 1（本参赛版）：三个年龄段使用**用户提供的高质量 3D 数字教练 PNG**
//  （coach_child / coach_youth / coach_senior，480x960 透明背景）作为主视觉，
//  取代旧的 Canvas 火柴人 / 骨骼人。
//
//  本对象是 ageId -> 资源 的**唯一**映射点；其它 Screen / 组件不要各自写
//  when(ageId){...} 分支，统一走这里，方便后续替换为 3D / 帧序列资源。
//
//  显示原则（对应验收 P0）：
//   - 首页 / Age / Exercise / Splash 等"静态展示"场景：**人物静止站立**，
//     无任何上下跳动 / 缩放 / 闪烁（本文件**不创建任何 infinite 循环动画**）。
//   - 竖向容器 + ContentScale.Fit：480x960 竖图**全身体可见**，头脚不被裁切；
//     透明背景落在浅色圆角容器上，不出现黑块。

/** 数字教练视觉资源注册表（ageId -> 可绘制的 coach 资源）。 */
object CoachVisual {

    /** 年龄段 -> 真实数字教练 PNG 资源 id（未知年龄回退到青年）。 */
    fun coachResource(ageId: String): Int = when (ageId) {
        "child" -> R.drawable.coach_child
        "senior" -> R.drawable.coach_senior
        else -> R.drawable.coach_youth
    }

    /** 年龄段中文短名（无障碍 / 描述用）。 */
    fun ageOf(ageId: String): String = when (ageId) {
        "child" -> "儿童教练"
        "senior" -> "银龄教练"
        else -> "青年教练"
    }

    /**
     * 静态教练头像（首页 / Age / Exercise / Report / Splash）。
     *
     *  要求（验收 P0-1 / P0-2）：
     *   - 人物静止站立，**无上下跳动 / 无大幅缩放 / 无闪烁**（不创建任何循环动画）。
     *   - 竖向容器（宽 = 高 * 0.7）+ [ContentScale.Fit]：480x960 竖图全身可见，
     *     头脚不被裁切；透明背景落在浅色圆角容器上，不出现黑块。
     *
     * @param size 头像**高度**（Dp）。宽度按 0.7:1 自动取，保证竖图全身可见。
     */
    @Composable
    fun CoachAvatar(
        ageId: String,
        modifier: Modifier = Modifier,
        size: Dp = 120.dp
    ) {
        val soft = Palette.ageSoft(ageId)
        val width = size * 0.7f
        val shape = RoundedCornerShape(size * 0.18f)
        Box(
            modifier = modifier
                .width(width)
                .height(size)
                .clip(shape)
                .background(soft, shape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(coachResource(ageId)),
                contentDescription = "数字教练 · ${ageOf(ageId)}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .matchParentSize()
                    .padding(size * 0.05f)
            )
        }
    }
}

// ============================================================================
// 数字教练 · 大尺寸主视觉（教学 / 跟练页）
// ============================================================================
//
//  Phase 1：渲染**真实数字教练 PNG**（静态站立）。允许显式开启 3D 透视
//  （[living] / [tilt]），但**默认关闭**，保证人物稳定、不跳动。
//
//  Phase 2 预留：本组件对外接口不变，内部 `painterResource` 后续可替换为
//  Lottie / 帧序列 / 真 3D（Filament·Unity），无需再改上层 Screen。
//
//  诚实声明：当前为**静态 PNG 形象**（非真实连续 3D 动画）。

/** 3D 倾斜轴向。 */
enum class TiltAxis { X, Y }

/**
 * 教学 / 跟练大尺寸数字教练主视觉（Phase 1：真实 PNG + 默认静止 + 可选 3D 透视）。
 *
 *  - [coachState] 驱动状态微动效（GOOD 上浮 / CORRECT 额外倾斜 / DEMO 微前倾）。
 *  - [tilt] 3D 透视倾斜（°）：0 = 正立；≠0 = 向某侧倾斜，产生立体视差。
 *  - [living] 保留参数（Phase 2 呼吸接口）。本版**默认静止**，即使传入 true
 *    也不产生持续循环（保持稳定性），仅在状态切换时做一次性位移。
 *  - 人物为页面视觉主体：教学页给 35%~45% 页面高度，跟练页给 25%~35%。
 */
@Composable
fun CoachFigure(
    ageId: String,
    modifier: Modifier = Modifier,
    coachState: CoachState = CoachState.IDLE,
    /** 3D 透视倾斜（°）。 */
    tilt: Float = 0f,
    tiltAxis: TiltAxis = TiltAxis.X,
    /** Phase 2 预留（本版始终静止站立，不产生循环动画）。 */
    living: Boolean = false
) {
    // 状态驱动的微位移/倾斜（仅状态变化时取值，避免每帧重组）
    val goodLift = if (coachState == CoachState.GOOD) 6f else 0f
    val correctExtraTilt = if (coachState == CoachState.CORRECT) 5f else 0f
    val demoLean = if (coachState == CoachState.DEMO || coachState == CoachState.PREPARE) 1.5f else 0f
    val totalTilt = tilt + correctExtraTilt + demoLean

    val cameraDistancePx = with(LocalDensity.current) { 14.dp.toPx() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(CoachVisual.coachResource(ageId)),
            contentDescription = "数字教练 · ${CoachVisual.ageOf(ageId)}",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    // 静态站立：仅状态相关一次性位移/倾斜（GOOD 上浮、CORRECT 微倾），
                    // 无任何持续循环 => 不会"跳动"，也不会在重组时叠加。
                    translationY = -goodLift
                    if (tiltAxis == TiltAxis.X) rotationX = totalTilt else rotationY = totalTilt
                    cameraDistance = cameraDistancePx
                },
            alpha = 0.98f
        )
    }
}

/**
 * 教学页数字教练（Phase 1）：真实 PNG 大尺寸（**默认静止站立**）+ 步骤解说文字同步滚动。
 *
 *  人物为静态形象（非连续 3D 动画）；下方步骤文案按 [CoachMotionEngine.teachingSequence]
 *  每一步 durationMs 循环切换，保留"文字示范 + 按运动区分"的教学节奏。
 *
 *  诚实声明：不同运动用**分年龄形象 + 分运动文字示范**；分阶段动作素材 / 真 3D
 *  为 Phase 2 预留接口，本版本不冒充"真实 3D 动画"。
 */
@Composable
fun TeachingCoach(
    ageId: String,
    exerciseKey: String?,
    modifier: Modifier
) {
    val seq = remember(exerciseKey, ageId) {
        CoachMotionEngine.teachingSequence(exerciseKey, ageId, exerciseKey ?: "")
    }
    var stepIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(seq) {
        while (true) {
            kotlinx.coroutines.delay(seq.steps.getOrNull(stepIdx % seq.steps.size)?.durationMs ?: 2200L)
            stepIdx = (stepIdx + 1) % seq.steps.size
        }
    }
    val step = seq.steps.getOrNull(stepIdx % seq.steps.size)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 真实数字教练主视觉（占主体高度，默认静止站立）
            CoachFigure(
                ageId = ageId,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.72f),
                coachState = CoachState.DEMO,
                living = false
            )
            // 步骤解说（按运动区分；与"统一形象"配套，非动作帧动画）
            Text(
                text = step?.message ?: "数字教练正在示范",
                fontSize = 15.sp,
                color = Color(0xFF1B2233),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}
