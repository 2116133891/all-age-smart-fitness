package com.quannian.zhidong.model

import com.quannian.zhidong.repository.TeachingVideoRepository

/**
 * V2 青年 七项 体测 的 展示 目录（任务 4）。
 *  每项 定义：名称 / 副 标题 / 单位 / 录入 类型 / 是否 性别 分支。
 *  教学 视频 与 标准 分别 由 [TeachingVideoRepository] 与 [BodyTestStandardService] 提供。
 */
object BodyTestCatalog {

    data class Item(
        val key: String,
        val name: String,
        val subtitle: String,
        val unit: String,
        /** 录入 类型：single / height_weight / none(仅 记录) */
        val inputType: InputType,
        val genderSplit: Boolean = false,
        /** 性别 分支 时 男/女 的 展示 名。 */
        val maleName: String = "",
        val femaleName: String = ""
    )

    enum class InputType { SINGLE, HEIGHT_WEIGHT, COUNT, NONE }

    val ITEMS: List<Item> = listOf(
        Item(BodyTestItemKey.BMI, "BMI", "体重 指数 = 体重(kg)/身高(m)²", "kg/m²",
            InputType.HEIGHT_WEIGHT, maleName = "BMI", femaleName = "BMI"),
        Item(BodyTestItemKey.VITAL_CAPACITY, "肺活量", "最大 呼气 量（专用 肺活量 仪 实测）", "mL",
            InputType.SINGLE),
        Item(BodyTestItemKey.RUN_50M, "50 米 跑", "起跑 · 加速 · 冲刺 计时", "s",
            InputType.SINGLE),
        Item(BodyTestItemKey.SEATED_REACH, "坐位 体前屈", "柔韧性 · 指尖 前伸 距离", "cm",
            InputType.SINGLE),
        Item(BodyTestItemKey.LONG_JUMP, "立定 跳远", "预摆 起跳 腾空 落地 距离", "cm",
            InputType.SINGLE),
        Item(BodyTestItemKey.PULLUP_SITUP, "引体 向上 / 仰卧 起坐", "按 性别 展示 对应 项目", "次",
            InputType.SINGLE, genderSplit = true,
            maleName = "引体 向上", femaleName = "1 分钟 仰卧 起坐"),
        Item(BodyTestItemKey.RUN_800_1000, "1000 / 800 米 跑", "耐力 · 配速 分段", "s",
            InputType.SINGLE, genderSplit = true,
            maleName = "1000 米 跑", femaleName = "800 米 跑")
    )

    fun byKey(key: String): Item? = ITEMS.firstOrNull { it.key == key }

    /** 按 性别 展示 的 名称（性别 分支 项 用）。 */
    fun displayName(item: Item, gender: String): String = when (item.key) {
        BodyTestItemKey.PULLUP_SITUP -> if (gender == "female") item.femaleName else item.maleName
        BodyTestItemKey.RUN_800_1000 -> if (gender == "female") item.femaleName else item.maleName
        else -> item.name
    }

    fun videoFor(item: Item, gender: String = "unknown") =
        TeachingVideoRepository.videoFor(item.key, gender)
}
