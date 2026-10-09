package com.quannian.zhidong.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalDensity
import com.quannian.zhidong.R
import com.quannian.zhidong.ui.theme.Palette
import kotlin.math.sin

// ============================================================================
// 数字教练 · 视觉资源（全项目唯一映射点）
// ============================================================================
//
//  Phase 1：三个年龄段使用**用户提供的真实数字教练 PNG**（coach_child /
//  coach_youth / coach_senior）作为主视觉，取代旧的 Canvas 火柴人 / 骨骼人。
//
//  本对象是 ageId → 资源 的**唯一**映射点；其它 Screen / 组件不要各自写
//  when(ageId){...} 分支，统一走这里，方便后续替换为 3D / 帧序列资源。

/** 数字教练视觉资源注册表（ageId → 可绘制的 coach 资源）。 */
object CoachVisual {

    /** 年龄段 → 真实数字教练 PNG 资源 id（未知年龄回退到青年）。 */
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
     * 静态教练头像（首页 / Age / Exercise / Report / Splash：站立、不蹦跳）。
     *  要求：人物静止站立；无上下跳动 / 无大幅 bob。
     */
    @Composable
    fun CoachAvatar(
        ageId: String,
        modifier: Modifier = Modifier,
        size: Dp = 120.dp
    ) {
        val soft = Palette.ageSoft(ageId)
        Box(
            modifier = modifier
                .padding(4.dp)
                .background(soft, RoundedCornerShape(size)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(coachResource(ageId)),
                contentDescription = "数字教练 · ${ageOf(ageId)}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(size))
            )
        }
    }
}

// ============================================================================
// 数字教练 · 大尺寸主视觉（教学 / 跟练页）
// ============================================================================
//
//  渲染**真实数字教练 PNG**，允许 ①静态站立 ②轻微缩放/呼吸 ③状态切换，
//  以及 ④真实 3D 透视倾斜（graphicsLayer，产生可感知的立体视差）。
//
//  **Phase 2 预留**：本组件的对外接口保持不变，内部 `painterResource`
//  后续可替换为 Lottie / 帧序列 / 真 3D（Filament·Unity），无需再改上层 Screen。

/** 3D 倾斜轴向。 */
enum class TiltAxis { X, Y }

/**
 * 教学 / 跟练大尺寸数字教练主视觉（Phase 1：真实 PNG + 3D 透视 + 呼吸浮动）。
 *
 *  - [coachState] 驱动状态动效（GOOD 放大上浮 / CORRECT 额外倾斜指向 / DEMO 微前倾）。
 *  - [tilt] 3D 透视倾斜（°）：0 = 正立；≠0 = 向某侧倾斜，产生立体视差。
 *  - [living] 是否播放轻微呼吸/浮动（静态展示场景可关）。
 *  - 人物为页面视觉主体：教学页给 35%~45% 页面高度，跟练页给 25%~35%。
 *
 *  **3D 效果说明**：
 *  本组件使用 Compose `graphicsLayer` 实现真实 3D 透视倾斜：
 *  - `rotationX` / `rotationY` + `cameraDistance` 产生**可感知的立体视差**，
 *    人物左右/前后倾斜时，近大远小效果让 2D PNG 呈现 3D 体积感。
 *  - 呼吸浮动：缓慢 Y 轴上下微动 + 极轻缩放，给人物"活着"的视觉生命感（非蹦跳）。
 *
 *  **Phase 2 升级路径**：接入 Lottie / 3D 帧序列 / Filament 真 3D 模型，
 *  本函数签名不变，上层 Screen 零改动。
 */
@Composable
fun CoachFigure(
    ageId: String,
    modifier: Modifier = Modifier,
    coachState: CoachState = CoachState.IDLE,
    /** 3D 透视倾斜（°）。 */
    tilt: Float = 0f,
    tiltAxis: TiltAxis = TiltAxis.X,
    /** 是否播放轻微呼吸/浮动。 */
    living: Boolean = true
) {
    // 呼吸：缓慢上下浮动 + 极轻缩放（"站在那但保持生命感"，非蹦跳）
    val transition = rememberInfiniteTransition(label = "coachLiving")
    val breath = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "breath"
    )
    val b = if (living) breath.value else 0f

    // 状态驱动的微动效（仅状态变化时取值，避免每帧重组）
    val goodLift = if (coachState == CoachState.GOOD) 8f else 0f
    val correctExtraTilt = if (coachState == CoachState.CORRECT) 6f else 0f
    val demoLean = if (coachState == CoachState.DEMO || coachState == CoachState.PREPARE) 2f else 0f

    val totalTilt = tilt + correctExtraTilt + demoLean
    val breatheY = b * 4f - goodLift
    val s = 1f + b * 0.02f

    // 透视距离用实际像素，避免不同 DPI 下倾斜强度不一致
    val cameraDistancePx = with(LocalDensity.current) { 12.dp.toPx() }

    // 外层 = 调用方给定 modifier；内层 Image 用 fillMaxSize 填满外层
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxHeight()) {
            Image(
                painter = painterResource(CoachVisual.coachResource(ageId)),
                contentDescription = "数字教练 · ${CoachVisual.ageOf(ageId)}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        // 真实 3D 透视倾斜（产生立体视差）
                        if (tiltAxis == TiltAxis.X) rotationX = totalTilt else rotationY = totalTilt
                        cameraDistance = cameraDistancePx
                        translationY = breatheY
                        scaleX = s
                        scaleY = s
                    },
                alpha = 0.98f
            )
        }
    }
}

/**
 * 教学页数字教练（Phase 1）：真实 PNG 大尺寸 + 3D 呼吸浮动 + 步骤解说文字同步滚动。
 *
 *  人物主视觉固定（站立/轻呼吸/微 3D 透视），下方步骤文案按
 *  [CoachMotionEngine.teachingSequence] 每步 durationMs 循环切换，
 *  保留"动作+文字同步"的教学节奏，去掉了火柴人。
 *
 *  Phase 2 接口预留：本函数签名不变，内部把 [CoachFigure] 换成 3D / 帧序列即可。
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
            // 真实数字教练主视觉（大尺寸，占据视觉主体）
            CoachFigure(
                ageId = ageId,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.78f),
                coachState = CoachState.DEMO,
                living = true
            )
            // 步骤解说（与动作序列同步切换）
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
