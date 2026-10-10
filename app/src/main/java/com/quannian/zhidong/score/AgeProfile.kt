package com.quannian.zhidong.score

import com.quannian.zhidong.model.AgeGroup

/**
 * 年龄段评分权重配置（对照 spec §18）。
 *  每个维度先归一到 0-100，再加权求和。不同年龄段/动作可调权重：
 *  - 青年（力量）：幅度 40 / 姿态 30 / 稳定 20 / 完整 10
 *  - 儿童（节奏）：重视节奏/完整，幅度 30 / 姿态 20 / 稳定 30 / 完整 20
 *  - 中年（减压高效）：兼顾幅度与稳定，幅度 35 / 姿态 30 / 稳定 25 / 完整 10
 *  - 银龄（安全）：重视稳定/完成/安全，幅度 20 / 姿态 25 / 稳定 35 / 完整 20
 *
 *  权重不要求和为 1；[ScoreCalculator] 会自动归一。
 */
data class AgeProfile(
    val rangeWeight: Float,
    val postureWeight: Float,
    val stabilityWeight: Float,
    val completenessWeight: Float
) {
    companion object {
        /** 默认/青年：标准四维。 */
        val DEFAULT = AgeProfile(0.40f, 0.30f, 0.20f, 0.10f)
        /** 中年：下班健身，幅度与稳定并重。 */
        val MIDDLE = AgeProfile(0.35f, 0.30f, 0.25f, 0.10f)

        fun forAge(age: AgeGroup): AgeProfile = when (age) {
            AgeGroup.CHILD -> AgeProfile(0.30f, 0.20f, 0.30f, 0.20f)
            AgeGroup.YOUTH -> DEFAULT
            AgeGroup.MIDDLE -> MIDDLE
            AgeGroup.SENIOR -> AgeProfile(0.20f, 0.25f, 0.35f, 0.20f)
        }

        fun forAge(id: String): AgeProfile = forAge(
            when (id) {
                "child" -> AgeGroup.CHILD
                "middle" -> AgeGroup.MIDDLE
                "senior" -> AgeGroup.SENIOR
                else -> AgeGroup.YOUTH
            }
        )
    }

    /** 归一化（权重和 → 1）。 */
    fun normalized(): AgeProfile {
        val sum = (rangeWeight + postureWeight + stabilityWeight + completenessWeight)
            .takeIf { it > 0f } ?: 1f
        return AgeProfile(
            rangeWeight / sum,
            postureWeight / sum,
            stabilityWeight / sum,
            completenessWeight / sum
        )
    }
}
