package com.quannian.zhidong.model

import android.content.Context
import androidx.core.content.edit

/**
 * 产品统一人群配置（V3 集中定义，全项目唯一来源，避免各页硬编码）。
 *
 *  三大成年年龄段：青年(18-35) / 中年(36-59) / 银龄(60+)。
 *  - 年龄范围是"产品默认分组"，用于入口/推荐/数字人/视频。
 *  - 正式体测标准的适用年龄与年级由 BodyTestStandardService 的标准配置单独决定，
 *    不把产品分组直接当成国家标准分组。
 */
object AgeGroupConfig {
    data class Band(
        val id: String,        // 人群 ID：全应用唯一标识
        val title: String,     // 中文短名
        val range: String,     // 产品默认年龄范围（展示用）
        val desc: String,      // 气质词
        val coachHint: String  // 教练定位提示
    )

    val BANDS = listOf(
        Band("youth", "青年", "18-35 岁", "体测备考 · 科学运动", "体测备考 · 专项训练 · AI 动作指导"),
        Band("middle", "中年", "36-59 岁", "体重管理 · 基础体能", "下班健身 · 肩颈缓解 · 高效代谢"),
        Band("senior", "银龄", "60+ 岁", "太极 · 八段锦 · 平衡", "舒缓运动 · 康养防跌 · 科学运动")
    )

    val GENDERS = listOf("male" to "男", "female" to "女")

    fun band(id: String): Band = BANDS.firstOrNull { it.id == id } ?: BANDS.first()
    fun contains(id: String): Boolean = BANDS.any { it.id == id }

    /** 兼容旧数据：把已废弃的 child 归到 youth（不再显示儿童入口，仅数据保留）。 */
    fun normalizeAgeId(raw: String): String =
        when (raw) { "child" -> "youth"; else -> if (contains(raw)) raw else "youth" }

    fun ageLabel(id: String): String = normalizeAgeId(id).let { band(it).title }

    /** 性别：仅男/女。历史 unknown/空值统一回退"男"，不留"不限"空白态。 */
    fun genderLabel(id: String): String = when (id) { "female" -> "女"; else -> "男" }
}

/**
 * 用户画像（年龄 + 性别），选择页写入，全局读取。
 *  - [ageId]：youth / middle / senior（三大成年人群；child 已废弃，读时归一化）。
 *  - [gender]：male / female（仅男女两选项，无"不限"）。
 *  全本地 SharedPreferences 持久化（符合离线原则，不上云）。
 */
data class Profile(
    val ageId: String = "youth",
    val gender: String = "male",
    val nickname: String = ""
)

/** 用户画像的本地存取（进程内单例 + SharedPreferences）。 */
object ProfileStore {
    private const val PREF = "quanli_profile"
    private const val K_AGE = "age"
    private const val K_GENDER = "gender"
    private const val K_NICK = "nickname"

    private var cached: Profile? = null

    fun load(context: Context): Profile {
        cached?.let { return it }
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val rawAge = p.getString(K_AGE, "youth") ?: "youth"
        val rawGender = p.getString(K_GENDER, "male") ?: "male"
        // 归一化：废弃的 child 归到 youth；历史 unknown 性别归到 male（不留空白态）
        val v = Profile(
            ageId = AgeGroupConfig.normalizeAgeId(rawAge),
            gender = when (rawGender) { "female" -> "female"; else -> "male" },
            nickname = p.getString(K_NICK, "") ?: ""
        )
        cached = v
        return v
    }

    fun save(context: Context, profile: Profile) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit {
            putString(K_AGE, profile.ageId)
            putString(K_GENDER, profile.gender)
            putString(K_NICK, profile.nickname)
            apply()
        }
        cached = profile
    }

    /** 性别中文短名（仅男/女；历史 unknown 归为 男，不再显示"不限"）。 */
    fun genderLabel(id: String): String = when (id) {
        "female" -> "女"
        else -> "男"
    }

    /** 年龄段中文短名（child 已废弃，归一化为 青年）。 */
    fun ageLabel(id: String): String = AgeGroupConfig.ageLabel(id)
}
