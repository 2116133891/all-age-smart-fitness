package com.quannian.zhidong.model

import android.content.Context
import androidx.core.content.edit

/**
 * 用户画像（年龄 + 性别），选择页写入，全局读取。
 *  - [ageId]：child / youth / middle / senior。
 *  - [gender]：male / female / unknown。
 *  全本地 SharedPreferences 持久化（符合离线原则，不上云）。
 */
data class Profile(
    val ageId: String = "youth",
    val gender: String = "unknown",
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
        val v = Profile(
            ageId = p.getString(K_AGE, "youth") ?: "youth",
            gender = p.getString(K_GENDER, "unknown") ?: "unknown",
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

    /** 性别中文短名（用于个性化文案）。 */
    fun genderLabel(id: String): String = when (id) {
        "male" -> "男"
        "female" -> "女"
        else -> "不限"
    }

    /** 年龄段中文短名。 */
    fun ageLabel(id: String): String = when (id) {
        "child" -> "儿童"
        "middle" -> "中年"
        "senior" -> "银龄"
        else -> "青年"
    }
}
