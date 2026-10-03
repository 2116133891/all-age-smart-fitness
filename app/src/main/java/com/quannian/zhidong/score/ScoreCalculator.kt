package com.quannian.zhidong.score

import com.quannian.zhidong.model.ErrorType
import kotlin.math.roundToInt

/**
 * 动作评分模型。
 *
 * 综合评分（0-100）拆为四个维度加权：
 *  - 动作幅度  rangeWeight    40%
 *  - 动作姿态  postureWeight  30%
 *  - 动作稳定性  stabilityWeight 20%
 *  - 动作完整度  completenessWeight 10%
 *
 * 每个维度先归一到 0-100，再加权求和。各维度由 [ScoreCalculator] 的
 * 具体计算函数给出（不同动作可覆写阈值）。
 *
 *  注意：这是"动作规范度评分"，用于运动教学辅助，并非医学级评估。
 */
data class ScoreBreakdown(
    val rangeScore: Float,        // 动作幅度 0-100
    val postureScore: Float,      // 动作姿态 0-100
    val stabilityScore: Float,    // 动作稳定性 0-100
    val completenessScore: Float, // 动作完整度 0-100
    val overall: Int             // 综合评分 0-100
) {
    val formPercent: Int get() = overall
}

object ScoreCalculator {

    const val RANGE_WEIGHT = 0.40f
    const val POSTURE_WEIGHT = 0.30f
    const val STABILITY_WEIGHT = 0.20f
    const val COMPLETENESS_WEIGHT = 0.10f

    /**
     * 计算综合评分。
     * @param repCount      实际完成次数
     * @param targetReps    目标次数（用于完整度）
     * @param avgDepth      平均动作深度/幅度的"达标比"（0-1，越接近 1 越达标）
     * @param postureSteady 躯干是否稳定（倾角是否可控），0-1
     * @param symSteady     左右对称/稳定度，0-1
     * @param errorCount    期间触发的错误提示数量（越少越好）
     * @param weights       年龄段/动作权重（默认 [AgeProfile.DEFAULT] = 青年标准）
     */
    fun calculate(
        repCount: Int,
        targetReps: Int,
        avgDepth: Float,
        postureSteady: Float,
        symSteady: Float,
        errorCount: Int,
        weights: AgeProfile = AgeProfile.DEFAULT
    ): ScoreBreakdown {
        val w = weights.normalized()
        // 幅度：达到目标深度的比例
        val range = (avgDepth * 100f).coerceIn(0f, 100f)
        // 姿态：躯干稳定性（1 = 完全稳定）
        val posture = (postureSteady * 100f).coerceIn(0f, 100f)
        // 稳定性：对称度 - 错误惩罚
        val stability = ((symSteady * 100f) - errorCount * 3f).coerceIn(0f, 100f)
        // 完整度：完成次数 / 目标次数
        val completeness = if (targetReps <= 0) 0f
        else ((repCount.toFloat() / targetReps) * 100f).coerceIn(0f, 100f)

        val overall = (
            range * w.rangeWeight +
            posture * w.postureWeight +
            stability * w.stabilityWeight +
            completeness * w.completenessWeight
        )
        return ScoreBreakdown(
            range,
            posture,
            stability,
            completeness,
            overall.roundToInt()
        )
    }

    /** 把分数映射为状态描述。银龄用更温和的语气。 */
    fun stateOf(score: Int, profile: AgeProfile = AgeProfile.DEFAULT): String = when {
        score >= 85 -> "动作规范，表现优秀"
        score >= 70 -> if (profile == AgeProfile.forAge("senior")) "动作舒缓到位，继续保持"
        else "基本规范，继续保持"
        score >= 50 -> if (profile == AgeProfile.forAge("senior")) "节奏偏快，可再舒缓一些"
        else "需要调整，注意细节"
        else -> if (profile == AgeProfile.forAge("senior")) "动作可以更放松，注意安全"
        else "动作偏差较大，重新练习"
    }
}
