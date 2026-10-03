package com.quannian.zhidong.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.model.AgeGroup
import com.quannian.zhidong.ui.theme.Palette
import kotlin.math.sin

// ============================================================================
// 数字教练系统
// ============================================================================
//
//  设计原则：
//  - 纯 Canvas 2D 矢量人物，不引入 3D/Lottie/外部素材，Android 9 兼容
//  - 三个年龄段有**明显不同的人物形态**（头身比、发型、动作风格）
//  - 支持 [CoachState] 状态机驱动不同动作/表情
//  - 与运动 phase 联动（通过参数传入）
//
//  CoachState:
//    IDLE      - 静立，轻微呼吸
//    INTRO     - 打招呼（挥手）
//    PREPARE   - 准备姿势
//    DEMO      - 演示动作（跟运动同步）
//    COUNTDOWN - 准备开始（指表/抬手指）
//    GOOD      - 做得好（竖大拇指/微笑）
//    CORRECT   - 纠错（手指动作指向关键点）
//    REST      - 休息（双手交叉）
//    FINISH    - 结束（挥手告别）
//
//  参数：
//    ageGroup  - 年龄段（决定人物形态）
//    exerciseId - 运动（决定演示动作类型）
//    motionPhase - 当前动作阶段（影响演示动画）
//    animationProgress - 0..1 动画进度（控制动画帧）
//    coachState - 当前教练状态
// ============================================================================

/** 教练状态。 */
enum class CoachState { IDLE, INTRO, PREPARE, DEMO, COUNTDOWN, GOOD, CORRECT, REST, FINISH }

/**
 * 数字教练（全龄）。
 *
 *  - 儿童：大头小身、圆脸、活泼，头身比 1:1.8
 *  - 青年：协调体态、运动型，头身比 1:3
 *  - 银龄：成熟体态、温和，头身比 1:2.8，带白发/老年特征
 *
 *  颜色使用 [Palette.ageColor]。
 */
@Composable
fun CoachAvatar(
    ageId: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    coachState: CoachState = CoachState.IDLE,
    motionPhase: String = "",
    animationProgress: Float = 0f
) {
    val main = Palette.ageColor(ageId)
    val soft = Palette.ageSoft(ageId)
    val ageGroup = when (ageId) {
        "child" -> AgeGroup.CHILD
        "youth" -> AgeGroup.YOUTH
        "senior" -> AgeGroup.SENIOR
        else -> AgeGroup.YOUTH
    }

    // 呼吸动画（IDLE/REST 用，幅度按年龄不同：儿童活泼、银龄沉稳）
    val transition = rememberInfiniteTransition(label = "breath")
    val breath = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (ageGroup) {
                    AgeGroup.CHILD -> 900   // 活泼
                    AgeGroup.YOUTH -> 1400  // 中速
                    AgeGroup.SENIOR -> 2400 // 沉稳
                }
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "b"
    )
    val breathVal = breath.value // 0..1 呼吸

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(24.dp))
            .background(soft, RoundedCornerShape(24.dp))
            .border(1.dp, main.copy(alpha = 0.18f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        val drawSize = size * 0.72f
        Canvas(modifier = Modifier.size(drawSize)) {
            when (ageGroup) {
                AgeGroup.CHILD -> drawChildCoach(
                    main = main,
                    state = coachState,
                    breath = breathVal,
                    animProgress = animationProgress
                )
                AgeGroup.YOUTH -> drawYouthCoach(
                    main = main,
                    state = coachState,
                    breath = breathVal,
                    animProgress = animationProgress
                )
                AgeGroup.SENIOR -> drawSeniorCoach(
                    main = main,
                    state = coachState,
                    breath = breathVal,
                    animProgress = animationProgress
                )
            }
        }
    }
}

// ============================================================================
// 儿童教练：大头、圆脸、小身、活泼
// ============================================================================

private fun DrawScope.drawChildCoach(
    main: Color,
    state: CoachState,
    breath: Float,
    animProgress: Float
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val o = breath * -4f * (w / 100f)

    // 儿童头身比约 1:1.8（头大）
    val headR = w * 0.22f
    val headCy = h * 0.28f + o
    val bodyTop = h * 0.42f + o
    val bodyH = h * 0.35f
    val bodyW = w * 0.32f
    val bodyL = cx - bodyW / 2f

    // 腿（简单圆腿）
    val legW = w * 0.08f
    val legH = h * 0.22f
    val legY = bodyTop + bodyH
    drawRoundedLine(
    start = Offset(cx - bodyW * 0.22f, legY),
    end = Offset(cx - bodyW * 0.22f - w * 0.04f * sin(animProgress * PI), legY + legH),
    color = main,
    strokeWidth = legW
)
    drawRoundedLine(
    start = Offset(cx + bodyW * 0.22f, legY),
    end = Offset(cx + bodyW * 0.22f + w * 0.04f * sin(animProgress * PI), legY + legH),
    color = main,
    strokeWidth = legW
    )

    // 身体（圆角矩形）
    drawRoundRect(
        color = main,
        topLeft = Offset(bodyL, bodyTop),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(w * 0.10f)
    )

    // 手臂（按状态变化）
    val armW = w * 0.07f
    when (state) {
        CoachState.IDLE, CoachState.REST -> {
            // 双手自然下垂，轻摆
            drawRoundedLine(
                start = Offset(bodyL + w * 0.04f, bodyTop + h * 0.1f),
                end = Offset(bodyL - w * 0.05f, bodyTop + bodyH * 0.85f),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.04f, bodyTop + h * 0.1f),
                end = Offset(bodyL + bodyW + w * 0.05f, bodyTop + bodyH * 0.85f),
                color = main, strokeWidth = armW
            )
        }
        CoachState.INTRO, CoachState.FINISH -> {
            // 挥手（右手抬高 + 摆动）
            drawRoundedLine(
                start = Offset(bodyL + w * 0.04f, bodyTop + h * 0.1f),
                end = Offset(bodyL - w * 0.05f, bodyTop + bodyH * 0.85f),
                color = main, strokeWidth = armW
            )
            val swing = sin(animProgress * PI * 2) * w * 0.12f
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.04f, bodyTop + h * 0.1f),
                end = Offset(bodyL + bodyW + w * 0.1f + swing, bodyTop - h * 0.05f - swing * 0.5f),
                color = main, strokeWidth = armW
            )
            // 右手掌
            drawCircle(main, radius = armW * 0.9f, center = Offset(bodyL + bodyW + w * 0.1f + swing, bodyTop - h * 0.05f - swing * 0.5f))
        }
        CoachState.PREPARE, CoachState.DEMO, CoachState.COUNTDOWN -> {
            // 双手抬举（与运动同步：animProgress 0→1 抬举幅度）
            val lift = animProgress.coerceIn(0f, 1f)
            val liftAngle = lift * w * 0.2f
            drawRoundedLine(
                start = Offset(bodyL + w * 0.04f, bodyTop + h * 0.1f),
                end = Offset(bodyL - w * 0.06f - liftAngle, bodyTop - liftAngle),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.04f, bodyTop + h * 0.1f),
                end = Offset(bodyL + bodyW + w * 0.06f + liftAngle, bodyTop - liftAngle),
                color = main, strokeWidth = armW
            )
        }
        CoachState.GOOD -> {
            // 竖大拇指（右手）
            drawRoundedLine(
                start = Offset(bodyL + w * 0.04f, bodyTop + h * 0.1f),
                end = Offset(bodyL - w * 0.05f, bodyTop + bodyH * 0.7f),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.04f, bodyTop + h * 0.1f),
                end = Offset(bodyL + bodyW + w * 0.04f, bodyTop - h * 0.05f),
                color = main, strokeWidth = armW
            )
            // 拇指
            drawCircle(main, radius = armW * 0.7f,
                center = Offset(bodyL + bodyW + w * 0.05f, bodyTop - h * 0.07f))
        }
        CoachState.CORRECT -> {
            // 左手指（指向下方关键点），右手握拳
            drawRoundedLine(
                start = Offset(bodyL + w * 0.04f, bodyTop + h * 0.1f),
                end = Offset(cx - w * 0.05f, bodyTop + bodyH + h * 0.1f),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.04f, bodyTop + h * 0.1f),
                end = Offset(bodyL + bodyW + w * 0.04f, bodyTop + bodyH * 0.5f),
                color = main, strokeWidth = armW
            )
        }
        else -> {}
    }

    // 头（大）
    drawCircle(main, radius = headR, center = Offset(cx, headCy))

    // 眼睛（按状态变化）
    val eyeY = headCy - headR * 0.1f
    val eyeSpread = headR * 0.42f
    when (state) {
        CoachState.GOOD, CoachState.FINISH, CoachState.INTRO -> {
            // 微笑 + 圆眼
            drawCircle(Color.White, radius = headR * 0.13f, center = Offset(cx - eyeSpread, eyeY))
            drawCircle(Color.White, radius = headR * 0.13f, center = Offset(cx + eyeSpread, eyeY))
            drawCircle(main, radius = headR * 0.06f, center = Offset(cx - eyeSpread, eyeY + headR * 0.03f))
            drawCircle(main, radius = headR * 0.06f, center = Offset(cx + eyeSpread, eyeY + headR * 0.03f))
            // 微笑
            drawSmile(cx, headCy + headR * 0.25f, headR * 0.5f, Color.White, w * 0.02f)
        }
        else -> {
            // 普通眼睛
            drawCircle(Color.White, radius = headR * 0.11f, center = Offset(cx - eyeSpread, eyeY))
            drawCircle(Color.White, radius = headR * 0.11f, center = Offset(cx + eyeSpread, eyeY))
            drawCircle(main, radius = headR * 0.05f, center = Offset(cx - eyeSpread, eyeY))
            drawCircle(main, radius = headR * 0.05f, center = Offset(cx + eyeSpread, eyeY))
        }
    }
}

// ============================================================================
// 青年教练：协调、运动型、中等头身比
// ============================================================================

private fun DrawScope.drawYouthCoach(
    main: Color,
    state: CoachState,
    breath: Float,
    animProgress: Float
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val o = breath * -3f * (w / 100f)

    // 青年头身比约 1:3（成人比例）
    val headR = w * 0.13f
    val headCy = h * 0.22f + o
    val bodyTop = h * 0.36f + o
    val bodyH = h * 0.38f
    val bodyW = w * 0.34f
    val bodyL = cx - bodyW / 2f

    // 腿（细长）
    val legW = w * 0.04f
    val legH = h * 0.32f
    val legY = bodyTop + bodyH
    val kneeBend = when (state) {
        CoachState.DEMO -> sin(animProgress * PI) * 0.5f // 下蹲时屈膝
        else -> 0f
    }
    drawRoundedLine(
        start = Offset(cx - bodyW * 0.18f, legY),
        end = Offset(cx - bodyW * 0.18f - kneeBend * w * 0.1f, legY + legH),
        color = main, strokeWidth = legW
    )
    drawRoundedLine(
        start = Offset(cx + bodyW * 0.18f, legY),
        end = Offset(cx + bodyW * 0.18f + kneeBend * w * 0.1f, legY + legH),
        color = main, strokeWidth = legW
    )

    // 躯干
    drawRoundRect(
        color = main,
        topLeft = Offset(bodyL, bodyTop),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(w * 0.08f)
    )

    // 手臂
    val armW = w * 0.045f
    when (state) {
        CoachState.DEMO -> {
            // 深蹲演示：双臂随下蹲抬举
            val lift = sin(animProgress * PI)
            drawRoundedLine(
                start = Offset(bodyL + w * 0.03f, bodyTop + h * 0.08f),
                end = Offset(bodyL - w * 0.08f - lift * w * 0.12f, bodyTop - lift * h * 0.2f),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.03f, bodyTop + h * 0.08f),
                end = Offset(bodyL + bodyW + w * 0.08f + lift * w * 0.12f, bodyTop - lift * h * 0.2f),
                color = main, strokeWidth = armW
            )
        }
        CoachState.INTRO, CoachState.FINISH -> {
            val swing = sin(animProgress * PI * 2) * w * 0.1f
            drawRoundedLine(
                start = Offset(bodyL + w * 0.03f, bodyTop + h * 0.08f),
                end = Offset(bodyL - w * 0.05f, bodyTop + bodyH * 0.6f),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.03f, bodyTop + h * 0.08f),
                end = Offset(bodyL + bodyW + w * 0.08f + swing, bodyTop - swing * 0.5f),
                color = main, strokeWidth = armW
            )
        }
        CoachState.GOOD -> {
            drawRoundedLine(
                start = Offset(bodyL + w * 0.03f, bodyTop + h * 0.08f),
                end = Offset(bodyL - w * 0.05f, bodyTop + bodyH * 0.5f),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.03f, bodyTop + h * 0.08f),
                end = Offset(bodyL + bodyW + w * 0.04f, bodyTop - h * 0.05f),
                color = main, strokeWidth = armW
            )
            drawCircle(main, radius = armW * 0.7f,
                center = Offset(bodyL + bodyW + w * 0.05f, bodyTop - h * 0.06f))
        }
        CoachState.CORRECT -> {
            drawRoundedLine(
                start = Offset(bodyL + w * 0.03f, bodyTop + h * 0.08f),
                end = Offset(cx - w * 0.04f, bodyTop + bodyH + h * 0.08f),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.03f, bodyTop + h * 0.08f),
                end = Offset(bodyL + bodyW + w * 0.03f, bodyTop + bodyH * 0.4f),
                color = main, strokeWidth = armW
            )
        }
        else -> {
            drawRoundedLine(
                start = Offset(bodyL + w * 0.03f, bodyTop + h * 0.08f),
                end = Offset(bodyL - w * 0.04f, bodyTop + bodyH * 0.7f),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.03f, bodyTop + h * 0.08f),
                end = Offset(bodyL + bodyW + w * 0.04f, bodyTop + bodyH * 0.7f),
                color = main, strokeWidth = armW
            )
        }
    }

    // 头（成人比例）
    drawCircle(main, radius = headR, center = Offset(cx, headCy))

    // 短发（青年）
    drawHair(cx, headCy, headR * 1.05f, main)

    // 眼睛
    val eyeY = headCy - headR * 0.08f
    val eyeSpread = headR * 0.42f
    drawCircle(Color.White, radius = headR * 0.11f, center = Offset(cx - eyeSpread, eyeY))
    drawCircle(Color.White, radius = headR * 0.11f, center = Offset(cx + eyeSpread, eyeY))
    drawCircle(main, radius = headR * 0.05f, center = Offset(cx - eyeSpread, eyeY))
    drawCircle(main, radius = headR * 0.05f, center = Offset(cx + eyeSpread, eyeY))

    // 表情
    if (state == CoachState.GOOD || state == CoachState.FINISH || state == CoachState.INTRO) {
        drawSmile(cx, headCy + headR * 0.3f, headR * 0.45f, Color.White, w * 0.015f)
    } else if (state == CoachState.CORRECT) {
        // 认真表情（直线嘴）
        drawLine(
            Color.White,
            start = Offset(cx - headR * 0.3f, headCy + headR * 0.4f),
            end = Offset(cx + headR * 0.3f, headCy + headR * 0.4f),
            strokeWidth = w * 0.015f
        )
    }
}

// ============================================================================
// 银龄教练：成熟、温和、老态特征（白发/老花镜/慢动作）
// ============================================================================

private fun DrawScope.drawSeniorCoach(
    main: Color,
    state: CoachState,
    breath: Float,
    animProgress: Float
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val o = breath * -2f * (w / 100f)

    // 银龄头身比 1:2.8（稍矮，体态成熟）
    val headR = w * 0.15f
    val headCy = h * 0.24f + o
    val bodyTop = h * 0.40f + o
    val bodyH = h * 0.36f
    val bodyW = w * 0.36f
    val bodyL = cx - bodyW / 2f

    // 腿（稍短，稳定）
    val legW = w * 0.05f
    val legH = h * 0.28f
    val legY = bodyTop + bodyH
    drawRoundedLine(
        start = Offset(cx - bodyW * 0.20f, legY),
        end = Offset(cx - bodyW * 0.20f, legY + legH),
        color = main, strokeWidth = legW
    )
    drawRoundedLine(
        start = Offset(cx + bodyW * 0.20f, legY),
        end = Offset(cx + bodyW * 0.20f, legY + legH),
        color = main, strokeWidth = legW
    )

    // 躯干（稍宽，中年体态）
    drawRoundRect(
        color = main,
        topLeft = Offset(bodyL, bodyTop),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(w * 0.10f)
    )

    // 手臂（慢动作，幅度小）
    val armW = w * 0.05f
    when (state) {
        CoachState.DEMO -> {
            // 银龄动作幅度小、速度慢
            val lift = sin(animProgress * PI) * 0.5f // 只到一半
            drawRoundedLine(
                start = Offset(bodyL + w * 0.04f, bodyTop + h * 0.06f),
                end = Offset(bodyL - w * 0.08f - lift * w * 0.1f, bodyTop - lift * h * 0.15f),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.04f, bodyTop + h * 0.06f),
                end = Offset(bodyL + bodyW + w * 0.08f + lift * w * 0.1f, bodyTop - lift * h * 0.15f),
                color = main, strokeWidth = armW
            )
        }
        CoachState.INTRO, CoachState.FINISH -> {
            val swing = sin(animProgress * PI * 2) * w * 0.06f
            drawRoundedLine(
                start = Offset(bodyL + w * 0.04f, bodyTop + h * 0.06f),
                end = Offset(bodyL - w * 0.05f, bodyTop + bodyH * 0.6f),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.04f, bodyTop + h * 0.06f),
                end = Offset(bodyL + bodyW + w * 0.06f + swing, bodyTop - swing * 0.3f),
                color = main, strokeWidth = armW
            )
        }
        CoachState.GOOD -> {
            drawRoundedLine(
                start = Offset(bodyL + w * 0.04f, bodyTop + h * 0.06f),
                end = Offset(bodyL - w * 0.05f, bodyTop + bodyH * 0.5f),
                color = main, strokeWidth = armW
            )
            drawRoundedLine(
                start = Offset(bodyL + bodyW - w * 0.04f, bodyTop + h * 0.06f),
                end = Offset(bodyL + bodyW + w * 0.03f, bodyTop - h * 0.04f),
                color = main, strokeWidth = armW
            )
        }
        else -> {
            // 双手自然下垂或背后交叉（REST）
            if (state == CoachState.REST) {
                drawRoundedLine(
                    start = Offset(bodyL + w * 0.04f, bodyTop + h * 0.06f),
                    end = Offset(cx + w * 0.1f, bodyTop + bodyH * 0.7f),
                    color = main, strokeWidth = armW
                )
                drawRoundedLine(
                    start = Offset(bodyL + bodyW - w * 0.04f, bodyTop + h * 0.06f),
                    end = Offset(cx - w * 0.1f, bodyTop + bodyH * 0.7f),
                    color = main, strokeWidth = armW
                )
            } else {
                drawRoundedLine(
                    start = Offset(bodyL + w * 0.04f, bodyTop + h * 0.06f),
                    end = Offset(bodyL - w * 0.04f, bodyTop + bodyH * 0.7f),
                    color = main, strokeWidth = armW
                )
                drawRoundedLine(
                    start = Offset(bodyL + bodyW - w * 0.04f, bodyTop + h * 0.06f),
                    end = Offset(bodyL + bodyW + w * 0.04f, bodyTop + bodyH * 0.7f),
                    color = main, strokeWidth = armW
                )
            }
        }
    }

    // 头（稍大 + 白发）
    drawCircle(main, radius = headR, center = Offset(cx, headCy))
    // 白发（顶部弧形）
    drawHair(cx, headCy, headR, Color.White)

    // 眼睛（温和）
    val eyeY = headCy - headR * 0.05f
    val eyeSpread = headR * 0.4f
    drawCircle(Color.White, radius = headR * 0.12f, center = Offset(cx - eyeSpread, eyeY))
    drawCircle(Color.White, radius = headR * 0.12f, center = Offset(cx + eyeSpread, eyeY))
    drawCircle(main, radius = headR * 0.06f, center = Offset(cx - eyeSpread, eyeY))
    drawCircle(main, radius = headR * 0.06f, center = Offset(cx + eyeSpread, eyeY))

    // 老花镜（银龄特征）
    drawStrokeRoundRect(
        left = cx - eyeSpread - headR * 0.2f, top = eyeY - headR * 0.15f,
        width = headR * 0.4f, height = headR * 0.3f,
        color = main, strokeW = w * 0.01f, radius = w * 0.02f
    )
    drawStrokeRoundRect(
        left = cx + eyeSpread - headR * 0.2f, top = eyeY - headR * 0.15f,
        width = headR * 0.4f, height = headR * 0.3f,
        color = main, strokeW = w * 0.01f, radius = w * 0.02f
    )

    // 嘴（温和微笑）
    drawSmile(cx, headCy + headR * 0.4f, headR * 0.3f, Color.White, w * 0.012f)
}

// ============================================================================
// 通用绘图辅助
// ============================================================================

private const val PI = 3.14159265f

/** 圆角线段。 */
private fun DrawScope.drawRoundedLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
}

/** 描边圆角矩形（用 topLeft/size 形式）。 */
private fun DrawScope.drawStrokeRoundRect(
    left: Float, top: Float, width: Float, height: Float,
    color: Color, strokeW: Float, radius: Float
) {
    drawRoundRect(
        topLeft = Offset(left, top),
        size = Size(width, height),
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW),
        cornerRadius = CornerRadius(radius)
    )
}

/** 画下半圆弧（微笑嘴）。topLeft/size 形式。 */
private fun DrawScope.drawSmile(
    cx: Float, cy: Float, r: Float, color: Color, strokeW: Float, sweep: Float = 160f
) {
    drawArc(
        color = color,
        startAngle = 10f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(cx - r, cy),
        size = Size(r * 2f, r * 2f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW)
    )
}

/** 头发：头顶半圆（实心）。 */
private fun DrawScope.drawHair(cx: Float, cy: Float, r: Float, color: Color) {
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2f, r * 2f)
    )
}

// ============================================================================
// 通用 UI 组件
// ============================================================================

/** 通用卡片容器。 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    background: Color = Palette.card,
    borderColor: Color = Palette.line,
    shape: Dp = 20.dp,
    content: @Composable (ColumnScope) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(shape))
            .background(background, RoundedCornerShape(shape))
            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(shape))
            .padding(16.dp)
    ) {
        content(this)
    }
}

/** 小标签。 */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, color = Color.White)
    }
}

/** 带 emoji 的运动卡片。 */
@Composable
fun ExerciseCard(
    icon: String,
    title: String,
    tagline: String,
    duration: String,
    level: String,
    accent: Color,
    accentSoft: Color,
    detected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
            .padding(16.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentSoft, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 26.sp)
            }
            Column {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(2.dp))
                Text(tagline, fontSize = 12.sp, color = Palette.inkSoft)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Pill(if (detected) "已支持实时检测" else "姿态识别", accent)
            Spacer(Modifier.height(6.dp))
            Text("$duration · $level", fontSize = 11.sp, color = Palette.inkSoft)
        }
    }
}
