package com.quannian.zhidong.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 数字人动作视图（分层 2D 骨骼人形，spec §四/§五/§八~§十六）。
 *
 *  输入 [CoachPose]（由 [CoachMotionEngine] 计算），把一个人形画出来：
 *  头 + 躯干 + 双臂 + 双腿，关节角度由 [CoachPose] 字段决定。
 *  **不同动作（深蹲屈膝、侧拉伸侧倾、开合跳分腿举臂…）得到肉眼可见的不同姿态**，
 *  不再是整张 PNG 上下跳动。
 *
 *  视觉：年龄段配色 + 按年龄调整头身比（儿童头大 / 青年修长 / 银龄稳重）。
 *  纯 Compose Canvas，无 native / Lottie / Rive 依赖，Android 9 稳定。
 *
 *  @param pose 当前要画的关节姿态
 *  @param ageId 年龄段（决定配色 + 体型）
 *  @param coachState 教练状态（影响表情等）
 */
@Composable
fun CoachMotionView(
    pose: CoachPose,
    ageId: String,
    coachState: CoachState,
    modifier: Modifier = Modifier
) {
    val age = when (ageId) {
        "child" -> com.quannian.zhidong.model.AgeGroup.CHILD
        "senior" -> com.quannian.zhidong.model.AgeGroup.SENIOR
        else -> com.quannian.zhidong.model.AgeGroup.YOUTH
    }
    val main = com.quannian.zhidong.ui.theme.Palette.ageColor(ageId)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCoachFigure(
                pose = pose,
                main = main,
                age = age,
                state = coachState
            )
        }
    }
}

/** 按 [CoachPose] 画一个分层 2D 骨骼人形（Canvas DrawScope 内）。 */
private fun DrawScope.drawCoachFigure(
    pose: CoachPose,
    main: Color,
    age: com.quannian.zhidong.model.AgeGroup,
    state: CoachState
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f

    // 年龄差异化的体型参数
    val headR = when (age) {
        com.quannian.zhidong.model.AgeGroup.CHILD -> w * 0.16f
        com.quannian.zhidong.model.AgeGroup.SENIOR -> w * 0.11f
        else -> w * 0.085f
    }
    val torsoH = when (age) {
        com.quannian.zhidong.model.AgeGroup.CHILD -> h * 0.20f
        com.quannian.zhidong.model.AgeGroup.SENIOR -> h * 0.26f
        else -> h * 0.30f
    }
    val limbW = when (age) {
        com.quannian.zhidong.model.AgeGroup.CHILD -> w * 0.05f
        com.quannian.zhidong.model.AgeGroup.SENIOR -> w * 0.035f
        else -> w * 0.03f
    }
    val headNeckRatio = 0.30f

    val skin = Color(0xFFE8B48F)
    val limbColor = main

    // 身体离地（跳绳/开合跳腾空）：整体上移
    val liftPx = pose.bodyLift * h * 0.06f
    // 重心左右偏移
    val shiftPx = pose.weightShift * w * 0.06f
    val baseX = cx + shiftPx

    // 躯干顶/底
    val sinkPx = pose.hipBend / 180f * h * 0.05f
    val torsoTopY = h * headNeckRatio - liftPx + sinkPx
    val torsoBotY = torsoTopY + torsoH

    // 躯干倾斜（绕躯干底旋转）
    val leanRad = Math.toRadians(pose.spineLean.toDouble())

    fun rot(px: Float, py: Float): Offset {
        val dx = px - baseX
        val dy = py - torsoBotY
        val rx = baseX + (dx * cos(leanRad) - dy * sin(leanRad)).toFloat()
        val ry = torsoBotY + (dx * sin(leanRad) + dy * cos(leanRad)).toFloat()
        return Offset(rx, ry)
    }
    // ===== 腿（先画，在躯干后面）=====
    val hipL = rot(baseX - limbW * 1.6f, torsoBotY)
    val hipR = rot(baseX + limbW * 1.6f, torsoBotY)
    val hipSpread = pose.legSpread * w * 0.18f
    val thighLen = h * 0.20f
    val shinLen = h * 0.20f
    val hipBendRad = Math.toRadians(pose.hipBend.toDouble())
    val kneeBendRad = Math.toRadians(pose.kneeBend.toDouble())

    fun leg(hip: Offset, spreadX: Float) {
        val spread = spreadX + hipSpread
        val kneeRad = hipBendRad - kneeBendRad * 0.5
        val knee = Offset(
            hip.x + spread + sin(kneeRad).toFloat() * thighLen,
            hip.y + cos(kneeRad).toFloat() * thighLen
        )
        val ankleRad = kneeRad - kneeBendRad
        val ankle = Offset(
            knee.x + sin(ankleRad).toFloat() * shinLen,
            knee.y + cos(ankleRad).toFloat() * shinLen
        )
        drawLine(limbColor, hip, knee, strokeWidth = limbW * 2.2f, cap = StrokeCap.Round)
        drawLine(limbColor, knee, ankle, strokeWidth = limbW * 2.0f, cap = StrokeCap.Round)
        drawCircle(limbColor, radius = limbW * 1.4f, center = Offset(ankle.x, ankle.y + limbW))
    }
    leg(hipL, -hipSpread * 0.5f)
    leg(hipR, hipSpread * 0.5f)

    // ===== 躯干 =====
    val torsoTopL = rot(baseX - limbW * 1.8f, torsoTopY)
    val torsoTopR = rot(baseX + limbW * 1.8f, torsoTopY)
    val torsoBotL = rot(baseX - limbW * 1.8f, torsoBotY)
    val torsoBotR = rot(baseX + limbW * 1.8f, torsoBotY)
    val torsoPath = Path().apply {
        moveTo(torsoTopL.x, torsoTopL.y)
        lineTo(torsoTopR.x, torsoTopR.y)
        lineTo(torsoBotR.x, torsoBotR.y)
        lineTo(torsoBotL.x, torsoBotL.y)
        close()
    }
    drawPath(
        path = torsoPath,
        brush = Brush.linearGradient(
            colors = listOf(main, main.copy(alpha = 0.7f)),
            start = Offset(torsoTopL.x, torsoTopL.y),
            end = Offset(torsoBotL.x, torsoBotL.y)
        )
    )

    // ===== 手臂 =====
    val shoulderL = rot(baseX - limbW * 1.7f, torsoTopY + torsoH * 0.08f)
    val shoulderR = rot(baseX + limbW * 1.7f, torsoTopY + torsoH * 0.08f)
    val armLen = h * 0.18f

    fun arm(shoulder: Offset, sideSign: Float) {
        val raiseRad = Math.toRadians((pose.shoulderRaise * sideSign).toDouble())
        val spreadRad = Math.toRadians(pose.armSpread.toDouble())
        val baseRad = Math.toRadians(90.0)
        val upRad = baseRad - raiseRad / sideSign + spreadRad * sideSign * 0.3
        val elbow = Offset(
            shoulder.x + sin(upRad).toFloat() * armLen * sideSign,
            shoulder.y + cos(upRad).toFloat() * armLen
        )
        val elbowBendRad = Math.toRadians(pose.elbowBend.toDouble())
        val wristRad = upRad + elbowBendRad * sideSign
        val wrist = Offset(
            elbow.x + sin(wristRad).toFloat() * armLen * 0.8f * sideSign,
            elbow.y + cos(wristRad).toFloat() * armLen * 0.8f
        )
        drawLine(skin, shoulder, elbow, strokeWidth = limbW * 1.8f, cap = StrokeCap.Round)
        drawLine(skin, elbow, wrist, strokeWidth = limbW * 1.6f, cap = StrokeCap.Round)
        drawCircle(main, radius = limbW * 1.2f, center = wrist)
    }
    arm(shoulderL, 1f)
    arm(shoulderR, -1f)

    // ===== 头 =====
    val neck = rot(baseX, torsoTopY)
    val tiltRad = Math.toRadians(pose.headTilt.toDouble())
    val headCenter = Offset(
        neck.x + sin(tiltRad).toFloat() * headR * 1.4f,
        neck.y - headR * 1.5f - headR * 0.4f
    )
    drawCircle(skin, radius = headR, center = headCenter)
    // 头发（银龄白发）
    val hairColor = if (age == com.quannian.zhidong.model.AgeGroup.SENIOR) Color(0xFFC8CDD6) else Color(0xFF2A2E3A)
    drawArc(
        color = hairColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(headCenter.x - headR, headCenter.y - headR),
        size = Size(headR * 2f, headR * 2f)
    )

    // 眼睛
    val eyeSpread = headR * 0.4f
    val eyeY = headCenter.y - headR * 0.05f
    drawCircle(Color.White, radius = headR * 0.13f, center = Offset(headCenter.x - eyeSpread, eyeY))
    drawCircle(Color.White, radius = headR * 0.13f, center = Offset(headCenter.x + eyeSpread, eyeY))
    drawCircle(main, radius = headR * 0.06f, center = Offset(headCenter.x - eyeSpread, eyeY + headR * 0.02f))
    drawCircle(main, radius = headR * 0.06f, center = Offset(headCenter.x + eyeSpread, eyeY + headR * 0.02f))

    // 嘴（face: 0 平静 / 1 微笑 / 2 大笑 / 3 专注）
    when {
        pose.face >= 2f -> drawArc(
            color = Color(0xFF7A3B2E),
            startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(headCenter.x - headR * 0.45f, headCenter.y + headR * 0.15f),
            size = Size(headR * 0.9f, headR * 0.9f)
        )
        pose.face >= 1f -> drawArc(
            color = Color(0xFF7A3B2E),
            startAngle = 30f, sweepAngle = 120f, useCenter = false,
            topLeft = Offset(headCenter.x - headR * 0.35f, headCenter.y + headR * 0.2f),
            size = Size(headR * 0.7f, headR * 0.7f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(limbW * 0.35f)
        )
        else -> drawLine(
            Color(0xFF7A3B2E),
            Offset(headCenter.x - headR * 0.2f, headCenter.y + headR * 0.35f),
            Offset(headCenter.x + headR * 0.2f, headCenter.y + headR * 0.35f),
            strokeWidth = limbW * 0.3f,
            cap = StrokeCap.Round
        )
    }

    // 银龄老花镜
    if (age == com.quannian.zhidong.model.AgeGroup.SENIOR) {
        val gr = eyeSpread * 1.1f
        drawCircle(main, style = androidx.compose.ui.graphics.drawscope.Stroke(2f), radius = gr, center = Offset(headCenter.x - eyeSpread, headCenter.y))
        drawCircle(main, style = androidx.compose.ui.graphics.drawscope.Stroke(2f), radius = gr, center = Offset(headCenter.x + eyeSpread, headCenter.y))
        drawLine(main, Offset(headCenter.x - eyeSpread + gr, headCenter.y), Offset(headCenter.x + eyeSpread - gr, headCenter.y), strokeWidth = 2f)
    }
}

/**
 * 教学演示播放器：按 [CoachMotionEngine.teachingSequence] 逐步播放动作 + 同步解说文字（spec §十七/§十九）。
 *
 *  - [CoachMotionView] 大尺寸数字人（跟随当前 step 的 [CoachPose]）
 *  - 当前 step 的解说文字（与动画同步切换）
 *  - 供教学页占据 35%~45% 页面高度
 */
@Composable
fun CoachTeachingPlayer(
    ageId: String,
    exerciseKey: String?,
    modifier: Modifier = Modifier
) {
    val seq = remember(exerciseKey, ageId) { CoachMotionEngine.teachingSequence(exerciseKey, ageId, exerciseKey ?: "") }
    var stepIdx by remember { mutableIntStateOf(0) }

    // 逐步切换：每个 step 等其 durationMs 后进入下一步（循环）
    LaunchedEffect(seq) {
        while (true) {
            kotlinx.coroutines.delay(seq.steps[stepIdx % seq.steps.size].durationMs)
            stepIdx = (stepIdx + 1) % seq.steps.size
        }
    }

    val step = seq.steps[stepIdx % seq.steps.size]

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            CoachMotionView(
                pose = step.pose,
                ageId = ageId,
                coachState = CoachState.DEMO
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.message ?: "",
                fontSize = 15.sp,
                color = Color(0xFF1B2233),
                textAlign = TextAlign.Center
            )
        }
    }
}
