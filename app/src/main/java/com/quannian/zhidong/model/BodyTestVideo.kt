package com.quannian.zhidong.model

/**
 * V2 体测教学视频条目（与本地 assets 视频对应）。
 *  全本地：视频文件放在 `app/src/main/assets/videos/`，通过 [assetPath] 加载。
 *
 *  数据模型用途：
 *   - 青年体测详情页展示"视频教学"卡片，点击进播放器页播放 [assetPath]。
 *   - 资源索引 / 缺失资源处理（任务 12：视频资源索引测试）。
 */
data class BodyTestVideo(
    /** 稳定资源 id（与 [BodyTestItemKey] 一致），如 "long_jump"。 */
    val id: String,
    /** 展示标题。 */
    val title: String,
    /** 教学要点（简短一句）。 */
    val summary: String,
    /** 常见错误（简短）。 */
    val commonErrors: String,
    /** 安全提示（简短）。 */
    val safetyTip: String,
    /** 动作要点列表（播放器下方逐条展示）。 */
    val keyPoints: List<String>,
    /** 本地 assets 路径（如 "videos/video_youth_long_jump.mp4"）。 */
    val assetPath: String,
    /** 年龄段 id（"youth" 等），用于归组。 */
    val ageId: String = "youth"
)

/**
 * V2 七项体测项目的标准 key（与 [BodyTestItem] 一一对应）。
 *  每个 key 都有独立的视频、标准、录入逻辑，不混用。
 */
object BodyTestItemKey {
    const val BMI = "bmi"
    const val VITAL_CAPACITY = "vital_capacity"
    const val RUN_50M = "run_50m"
    const val SEATED_REACH = "seated_reach"
    const val LONG_JUMP = "long_jump"
    const val PULLUP_SITUP = "pullup_situp"
    const val RUN_800_1000 = "run_800_1000"

    /** 全部七项 key（含按性别分支）。 */
    val ALL = listOf(
        BMI, VITAL_CAPACITY, RUN_50M, SEATED_REACH,
        LONG_JUMP, PULLUP_SITUP, RUN_800_1000
    )
}

/**
 * 一项体测（青年七项之一）的展示元数据。
 *  与 [BodyTestVideo] 区分：这是"项目"元信息，不指向视频。
 *  [unit] 与 [valueType] 决定录入框类型。
 */
data class BodyTestItem(
    val key: String,              // BodyTestItemKey.*
    val name: String,             // 展示名
    val subtitle: String,         // 一句话说明
    val unit: String,             // 单位（kg/m / mL / s / cm / 次 / s/1000m）
    /** 数值类型，决定录入方式。 */
    val valueType: ValueType,
    /** 是否按性别分支展示（引体/仰卧起坐 · 1000m/800m）。 */
    val genderSplit: Boolean = false,
    /** 教学视频（可为 null，如 BMI/肺活量无视频，仅有方法说明）。 */
    val video: BodyTestVideo? = null
)

/** 数值录入类型，决定 UI 与校验。 */
enum class ValueType {
    /** 双值录入：BMI（身高+体重）。 */
    DOUBLE,
    /** 单值录入：cm / s / 次。 */
    SINGLE,
    /** 双值选择：性别决定引体/仰卧起坐、1000m/800m。 */
    GENDER_CHOICE
}
