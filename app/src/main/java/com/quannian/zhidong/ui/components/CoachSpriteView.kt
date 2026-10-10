package com.quannian.zhidong.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 数字教练动作视图（会动的 2D 关节人形，替代静态 PNG 站立）。
 *
 *  设计（spec §五/§九/§十七/§十六）：
 *   - **主视觉 = [CoachMotionView] 关节人形**：按 [CoachMotionEngine] 计算的
 *     [CoachPose] 关节角度，把数字人画成会动的人形（深蹲下蹲 / 侧倾 / 开合跳分腿等
 *     肉眼可见不同），不再是整张 PNG 上下跳。
 *   - 教学页（livePose == null）：播放 [CoachMotionEngine.teachingSequence]，
 *     相邻两步之间用 600ms 关节插值平滑过渡（数字人"真的做这个动作"），
 *     下方同步解说文字。
 *   - 跟练页（livePose != null）：直接渲染实时 [livePose]（随动作阶段/纠错状态变化），
 *     数字人跟着用户一起动。
 *   - 顶部保留小 [CoachAvatar] 形象做"数字教练"身份识别，主体是会动的人形。
 *
 *  纯 Compose Canvas，无 native / Lottie / Rive 依赖，Android 9 稳定。
 *
 * @param figureSizePx 保留参数（兼容旧调用），当前由外层 modifier 决定尺寸。
 * @param livePose 实时 pose（跟练页驱动）；非空时进入"实时模式"。
 * @param coachState 教练状态（影响表情等）。
 */
@Composable
fun CoachSpriteView(
    ageId: String,
    exerciseKey: String?,
    modifier: Modifier = Modifier,
    figureSizePx: Int = 480,
    livePose: CoachPose? = null,
    coachState: CoachState = CoachState.IDLE
) {
    if (livePose != null) {
        // 跟练模式：数字人按实时 pose 做动作（随 phase + 纠错状态变化）
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CoachMotionView(
                pose = livePose,
                ageId = ageId,
                coachState = coachState,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // 教学模式：播放 teachingSequence，关节插值让数字人动起来
        TeachingMotionCoach(ageId = ageId, exerciseKey = exerciseKey, modifier = modifier)
    }
}

/**
 * 教学页数字教练（会动版）：播放该运动的示范序列，数字人按关节插值逐步做动作，
 * 下方解说文字同步切换。不同运动 → 不同姿态（深蹲/侧倾/开合跳/八段锦 8 式…肉眼可区分）。
 */
@Composable
private fun TeachingMotionCoach(
    ageId: String,
    exerciseKey: String?,
    modifier: Modifier
) {
    val seq = remember(exerciseKey, ageId) {
        CoachMotionEngine.teachingSequence(exerciseKey, ageId, exerciseKey ?: "")
    }
    val n = seq.steps.size
    var stepIdx by remember { mutableIntStateOf(0) }
    var prevIdx by remember { mutableIntStateOf((n - 1) % n) }
    var t by remember { mutableFloatStateOf(0f) }

    // 逐步推进（循环）：每个 step 停留其 durationMs 后进入下一步
    LaunchedEffect(seq) {
        while (true) {
            kotlinx.coroutines.delay(seq.steps.getOrNull(stepIdx % n)?.durationMs ?: 2200L)
            stepIdx = (stepIdx + 1) % n
        }
    }

    // 切换 step：上一 pose 作为插值起点，0→1 平滑过渡到当前 pose，数字人"动起来"
    LaunchedEffect(stepIdx) {
        prevIdx = (stepIdx - 1 + n) % n
        val anim = androidx.compose.animation.core.Animatable(0f)
        anim.snapTo(0f)
        anim.animateTo(1f, tween(600)) { t = this.value }
    }

    val cur = seq.steps.getOrNull(stepIdx % n)
    val prev = seq.steps.getOrNull(prevIdx % n)
    val pose = lerpPose(prev?.pose ?: cur?.pose ?: CoachPose(), cur?.pose ?: CoachPose(), t)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 小形象做身份识别（静止）
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CoachAvatar(ageId = ageId, size = 64.dp)
        }
        // 会动的关节动作人形（按当前运动逐步演示）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            CoachMotionView(
                pose = pose,
                ageId = ageId,
                coachState = CoachState.DEMO,
                modifier = Modifier.fillMaxSize()
            )
        }
        // 同步解说文字（与动作同步切换）
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cur?.message ?: "数字教练正在示范",
                fontSize = 14.sp,
                color = Color(0xFF1B2233),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** 相邻两步 [CoachPose] 之间做关节插值，让数字人动作平滑过渡（不跳变）。 */
private fun lerpPose(a: CoachPose, b: CoachPose, t: Float): CoachPose {
    fun l(x: Float, y: Float) = x + (y - x) * t
    return CoachPose(
        shoulderRaise = l(a.shoulderRaise, b.shoulderRaise),
        elbowBend = l(a.elbowBend, b.elbowBend),
        armSpread = l(a.armSpread, b.armSpread),
        hipBend = l(a.hipBend, b.hipBend),
        kneeBend = l(a.kneeBend, b.kneeBend),
        spineLean = l(a.spineLean, b.spineLean),
        headTilt = l(a.headTilt, b.headTilt),
        legSpread = l(a.legSpread, b.legSpread),
        bodyLift = l(a.bodyLift, b.bodyLift),
        weightShift = l(a.weightShift, b.weightShift),
        face = l(a.face, b.face),
        progress = l(a.progress, b.progress)
    )
}
