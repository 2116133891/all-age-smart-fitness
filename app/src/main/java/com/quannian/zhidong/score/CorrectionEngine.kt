package com.quannian.zhidong.score

import com.quannian.zhidong.model.ErrorType

/**
 * 纠错引擎：规则判断 + 自然语言映射。
 *
 *  输入：某一帧的关键指标（关节角度、躯干倾角、左右对称度、幅度达标比、是否有完整姿态）。
 *  输出：命中的 [ErrorType] 列表。UI 据此展示"纠错提示"。
 *
 *  第一阶段用预设规则（阈值）+ 固定文案；未来可替换为大模型生成自然语言建议。
 */
object CorrectionEngine {

    /** 单次纠错输入。字段可按动作扩展。 */
    data class FrameSignals(
        val personPresent: Boolean,
        val depthRatio: Float,      // 当前动作幅度 / 目标幅度，0-1（越高越好）
        val torsoTilt: Float,       // 躯干倾角（度），越大越不稳定
        val symmetryRatio: Float,   // 左右对称度，0-1（越高越稳）
        val hasFullPose: Boolean    // 是否检测到完整骨架（防止误判无人）
    )

    fun evaluate(sig: FrameSignals): List<ErrorType> {
        val out = mutableListOf<ErrorType>()
        if (!sig.personPresent || !sig.hasFullPose) {
            out += ErrorType.NO_PERSON
            return out
        }
        // 幅度不足
        if (sig.depthRatio < 0.6f) out += ErrorType.RANGE_TOO_SHALLOW
        // 躯干过前倾（以倾角 > 20° 视为明显不直）
        if (sig.torsoTilt > 20f) out += ErrorType.BODY_TOO_FORWARD
        // 左右不对称 / 失衡
        if (sig.symmetryRatio < 0.55f) out += ErrorType.UNSTABLE
        // 幅度很浅时同时提示手臂/抬举幅度
        if (sig.depthRatio < 0.4f) out += ErrorType.ARMS_TOO_LOW
        return out
    }

    /** 从错误类型集合里挑出最适合展示的一条（优先级序）。 */
    fun topSuggestion(errors: Set<ErrorType>): String? =
        errors.firstOrNull()?.suggestion

    /** 展示多条建议（取前 3 条，去重）。 */
    fun suggestions(errors: Set<ErrorType>, limit: Int = 3): List<String> =
        errors.take(limit).map { it.suggestion }.distinct()
}
