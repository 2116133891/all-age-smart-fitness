package com.quannian.zhidong.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 数字教练动作视图（Phase 1：真实数字教练 PNG 主视觉 + 教学步骤解说同步）。
 *
 *  取代旧的 [CoachSpriteRenderer] 火柴人帧序列：
 *   - **主视觉 = 对应年龄段的真实数字教练 PNG**（不再出现 Canvas 火柴人 / 骨骼人）。
 *   - 教学页：[CoachMotionEngine.teachingSequence] 的**解说文案**仍逐步同步播放，
 *     人物保持站立/微动（后续 Phase 2 可接入 3D 或动作帧序列）。
 *   - 跟练页：`livePose` 非空时保持人物随 phase 微动 + 显示当前阶段。
 *
 *  @param figureSizePx 保留参数（兼容旧调用），Phase 1 中不再驱动火柴人渲染。
 *  @param livePose 实时 pose（跟练页驱动）；非空时进入"实时模式"。
 */
@Composable
fun CoachSpriteView(
    ageId: String,
    exerciseKey: String?,
    modifier: Modifier = Modifier,
    figureSizePx: Int = 480,
    livePose: com.quannian.zhidong.ui.components.CoachPose? = null
) {
    if (livePose != null) {
        // 实时模式（跟练页）：真实数字教练 + 微动
        CoachFigure(ageId = ageId, modifier = modifier, living = true)
    } else {
        // 教学模式：真实数字教练 + 逐步解说文字（与动作序列同步）
        TeachingCoach(ageId = ageId, exerciseKey = exerciseKey, modifier = modifier)
    }
}
