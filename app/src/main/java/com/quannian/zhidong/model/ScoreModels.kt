package com.quannian.zhidong.model

/**
 * 错误类型枚举。规则引擎输出这些类型，再映射为自然语言纠正建议。
 * 这里不接大模型，用预设文案；未来可替换为 LLM 生成。
 */
enum class ErrorType(val code: String, val suggestion: String) {
    ARMS_TOO_LOW("armsTooLow", "手臂抬高一些，尽量达到目标幅度"),
    BODY_TOO_FORWARD("bodyTooForward", "保持躯干稳定，身体不要过度前倾"),
    KNEE_ANGLE_INVALID("kneeAngleInvalid", "膝关节保持稳定，下蹲时膝盖不要超过脚尖"),
    UNSTABLE("unstablePose", "请保持身体平衡，动作节奏放慢一些"),
    RANGE_TOO_SHALLOW("rangeTooShallow", "适当增加动作幅度，下蹲更深一些"),
    SPEED_TOO_FAST("speedTooFast", "动作速度可以再均匀一些，避免过快"),
    NO_PERSON("noPerson", "请站在摄像头前，保持全身处于画面中央"),
    POSTURE_DRIFT("postureDrift", "注意身体重心，保持左右对称")
}

/**
 * 一次完整训练结束后的报告数据。
 *
 *  除总分外保留四维细分（幅度/姿态/稳定/完整），供报告页逐项展示。
 *  [coachSummary] 与 [nextSuggestion] 由 CoachAIProvider 生成（本地或 LLM）。
 */
data class TrainingReport(
    val exerciseName: String,
    val ageGroup: String = "",            // 新增：年龄段 id（child/youth/senior），用于个性化
    val repCount: Int,
    val targetReps: Int,
    val formScorePercent: Int,            // 动作规范度 0-100
    val overallScore: Int,               // 综合评分 0-100
    val durationSec: Int,
    val highlights: List<String>,         // 表现良好
    val improvements: List<String>,       // 建议改进
    val errorTypesSeen: Set<String>,
    // 四维细分（0-100）
    val rangeScore: Float = 0f,
    val postureScore: Float = 0f,
    val stabilityScore: Float = 0f,
    val completenessScore: Float = 0f,
    // AI 教练总结与下一次建议（CoachAIProvider 生成）
    val coachSummary: String = "",
    val nextSuggestion: String = ""
)
