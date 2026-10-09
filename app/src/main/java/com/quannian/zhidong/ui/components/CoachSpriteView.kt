package com.quannian.zhidong.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 数字教练动作视图（Phase 1：真实数字教练 PNG 主视觉 + 分运动文字示范 + 状态反馈）。
 *
 *  取代旧的 [CoachSpriteRenderer] 火柴人帧序列：
 *   - **主视觉 = 对应年龄段的真实数字教练 PNG**（不再出现 Canvas 火柴人 / 骨骼人）。
 *   - 教学页（livePose == null）：真实形象 + [CoachMotionEngine.teachingSequence]
 *     的**分运动文字示范**逐步播放（人物保持静止站立，非动作帧动画，诚实标注 Phase 1）。
 *   - 跟练页（livePose != null）：真实形象按 [coachState]（DEMO/CORRECT/GOOD…）做
 *     **克制的状态反馈**（GOOD 上浮 / CORRECT 微倾指向 / DEMO 微前倾），与用户实时
 *     摄像头骨架**明确区分**（教练 = 示范，用户 = 实时识别）。
 *
 *  诚实声明：Phase 1 为**分年龄静态 PNG + 分运动文字示范 + 状态微反馈**，
 *  不是真实连续 3D 动画；分阶段动作素材 / 真 3D 为 Phase 2 预留（见 [CoachFigure]）。
 *
 * @param figureSizePx 保留参数（兼容旧调用），Phase 1 中不驱动火柴人渲染。
 * @param livePose 实时 pose（跟练页驱动）；非空时进入"实时模式"。
 * @param coachState 教练状态（实时模式下驱动数字人的克制状态反馈）。
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
        // 实时模式（跟练页）：真实数字教练 + 按教练状态做克制反馈（默认静止站立）
        CoachFigure(ageId = ageId, modifier = modifier, coachState = coachState, living = false)
    } else {
        // 教学模式：真实数字教练 + 逐步（分运动）文字示范
        TeachingCoach(ageId = ageId, exerciseKey = exerciseKey, modifier = modifier)
    }
}
