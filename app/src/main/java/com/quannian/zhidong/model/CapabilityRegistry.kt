package com.quannian.zhidong.model

/**
 * 动作分析能力等级（任务 21：为每个运动项目建立明确的分析能力）。
 *  低置信度时 UI 显示"未能稳定识别，请调整站位或手动记录"，不伪造计数。
 */
enum class AnalysisCapability {
    /** 已验证的自动计数。 */
    VERIFIED_AUTO_COUNT,
    /** 可提供辅助姿态分析（角度/幅度参考，非正式计数）。 */
    ASSIST_POSE,
    /** 仅支持手动成绩录入。 */
    MANUAL_ENTRY,
    /** 尚未实现可靠自动识别。 */
    NOT_IMPLEMENTED
}

/**
 * 各项目/动作的分析能力映射（任务 21 + 任务 26 统一项目模型的"识别能力状态"字段）。
 *  基于真实实现：
 *  - 深蹲/开合跳/八段锦/太极/肩颈/舒缓/跳绳：已有专属分析器（姿态参考，非官方计数）→ ASSIST_POSE。
 *  - 体测 7 项：本版本以视频教学 + 手工录入为主（引体/仰卧未复制深蹲冒充姿态识别）→ MANUAL_ENTRY。
 *  - 50m/耐力计时、肺活量：必须依赖可靠实际数据源 → MANUAL_ENTRY（不自动计）。
 *  不得把"已实现姿态参考"描述成"已验证自动计数"。
 */
object CapabilityRegistry {

    /** 现有分析器对应的动作 key。 */
    val EXERCISE_CAPABILITY: Map<String, AnalysisCapability> = mapOf(
        "squat" to AnalysisCapability.ASSIST_POSE,
        "shoulder" to AnalysisCapability.ASSIST_POSE,
        "yoga" to AnalysisCapability.ASSIST_POSE,
        "jumprope" to AnalysisCapability.ASSIST_POSE,
        "jack" to AnalysisCapability.ASSIST_POSE,
        "baduanjin" to AnalysisCapability.ASSIST_POSE,
        "taichi" to AnalysisCapability.ASSIST_POSE,
        "easy" to AnalysisCapability.ASSIST_POSE
    )

    /** 体测 7 项（本版本手工录入为主）。 */
    val BODY_TEST_CAPABILITY: Map<String, AnalysisCapability> = mapOf(
        BodyTestItemKey.BMI to AnalysisCapability.MANUAL_ENTRY,
        BodyTestItemKey.VITAL_CAPACITY to AnalysisCapability.MANUAL_ENTRY,
        BodyTestItemKey.RUN_50M to AnalysisCapability.MANUAL_ENTRY,
        BodyTestItemKey.SEATED_REACH to AnalysisCapability.ASSIST_POSE,
        BodyTestItemKey.LONG_JUMP to AnalysisCapability.NOT_IMPLEMENTED, // 自动测距需标定+误差验证
        BodyTestItemKey.PULLUP_SITUP to AnalysisCapability.MANUAL_ENTRY,
        BodyTestItemKey.RUN_800_1000 to AnalysisCapability.MANUAL_ENTRY
    )

    /** 动作 key（跟练）能力。 */
    fun forExercise(key: String): AnalysisCapability =
        EXERCISE_CAPABILITY[key] ?: AnalysisCapability.NOT_IMPLEMENTED

    /** 体测 itemKey 能力。 */
    fun forBodyTest(itemKey: String): AnalysisCapability =
        BODY_TEST_CAPABILITY[itemKey] ?: AnalysisCapability.NOT_IMPLEMENTED

    /** 面向用户的中文说明（诚实，不夸大）。 */
    fun describe(cap: AnalysisCapability): String = when (cap) {
        AnalysisCapability.VERIFIED_AUTO_COUNT -> "已验证自动计数"
        AnalysisCapability.ASSIST_POSE -> "可辅助姿态分析（非正式计数）"
        AnalysisCapability.MANUAL_ENTRY -> "仅支持手动成绩录入"
        AnalysisCapability.NOT_IMPLEMENTED -> "暂未实现可靠自动识别"
    }
}
