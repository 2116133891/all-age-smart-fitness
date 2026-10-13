package com.quannian.zhidong.model

import android.content.Context
import androidx.core.content.edit

/**
 * V3.3 数字教练呈现风格（全应用唯一配置来源）。
 *
 *  两个明确风格：
 *   - [REALISTIC]：真人风格教练（自然、真实运动教学形象；已有真人教学视频）。
 *   - [ANIMATED] ：动画数字教练（风格化 3D 动画形象 + 对应动画教学视频）。
 *
 *  业务约束（对应 spec §3.2）：
 *   - 风格切换**不改变**动作 ID、训练记录 ID、评分逻辑与历史成绩；
 *     风格只影响"头像 / 示范视频"的资源选择。
 *   - 各年龄段默认风格：青少年=REALISTIC（复用已有真人风格视频），
 *     中青年/中老年=ANIMATED（复用现有动画风格素材）。
 *   - 用户可自行更改；选择持久化到本地 SharedPreferences，重启后仍然有效。
 */
enum class CoachStyle(val id: String, val title: String, val subtitle: String) {
    REALISTIC("realistic", "真人风格教练", "自然运动教学形象 · 真人示范视频"),
    ANIMATED("animated", "动画数字教练", "风格化 3D 形象 · 动画示范视频");

    companion object {
        fun fromRaw(raw: String): CoachStyle =
            entries.firstOrNull { it.id == raw } ?: defaultForAgeGroup("youth")

        /** 各年龄段默认风格（spec §3.2-4）。 */
        fun defaultForAgeGroup(ageId: String): CoachStyle = when (AgeGroupConfig.normalizeAgeId(ageId)) {
            "youth" -> REALISTIC
            else -> ANIMATED
        }
    }
}

/** 用户画像扩展：教练风格（V3.3 新增，与 [Profile] 一起持久化）。 */
data class CoachPreference(
    val ageId: String = "youth",
    val gender: String = "male",
    val style: CoachStyle = CoachStyle.REALISTIC
)

/** 教练风格本地存取（全本地 SharedPreferences，离线原则，不上云）。 */
object CoachStyleStore {
    private const val PREF = "quanli_coach"
    private const val K_STYLE = "style"
    private const val K_DEFAULTED = "defaulted"

    private var cached: CoachStyle? = null

    /** 读取当前风格。无记录时按当前年龄段返回默认风格并落盘（保证永不空白）。 */
    fun load(context: Context): CoachStyle {
        cached?.let { return it }
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = p.getString(K_STYLE, null)
        val v = if (raw.isNullOrBlank()) {
            CoachStyle.defaultForAgeGroup(AgeGroupConfig.normalizeAgeId(p.getString(K_DEFAULTED, "youth") ?: "youth"))
        } else {
            CoachStyle.fromRaw(raw)
        }
        cached = v
        return v
    }

    fun save(context: Context, style: CoachStyle, ageId: String = "youth") {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit {
            putString(K_STYLE, style.id)
            putString(K_DEFAULTED, AgeGroupConfig.normalizeAgeId(ageId))
            apply()
        }
        cached = style
    }
}
