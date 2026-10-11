package com.quannian.zhidong.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * V2 体测原始成绩记录（Room，全本地，可追溯）。
 *
 *  设计原则（任务 4/5/9）：
 *   - 原始成绩可追溯：[rawValue]/[rawValue2] 保留录入原值 + [unit]。
 *   - 数据来源：[source]（manual/device）+ [deviceVerified]（是否设备验证）。
 *   - 是否人工录入：[manualEntry]。
 *   - 适用标准版本：[standardVersion]（空 = 未选/无权威标准）。
 *   - 评分结果：[score] 可空，仅当数据充分且标准 verified 时生成；否则为 0 且 [scoreValid]=false。
 *   - 单用户本地档案：本 MVP 无账号体系，[profileId] 固定为本地单用户（"local"），
 *     预留字段以便未来多档案隔离，但当前不做云同步/多用户。
 */
@Entity(
    tableName = "body_test_results",
    indices = [
        androidx.room.Index(value = ["itemKey"]),
        androidx.room.Index(value = ["testDate"])
    ]
)
data class BodyTestResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemKey: String,            // BodyTestItemKey.*
    val ageGroup: String,           // "youth"
    val gender: String,             // male / female / unknown
    val testDate: Long,            // 毫秒时间戳
    val rawValue: Double,          // 主值（如 50m 秒数、坐位体前屈 cm、引体次数、BMI kg/m2）
    val rawValue2: Double = 0.0,   // 第二值（BMI 记录体重 kg；跑步记录配速等；否则 0）
    val heightCm: Double = 0.0,    // BMI 用：身高
    val weightKg: Double = 0.0,    // BMI 用：体重
    val unit: String = "",
    val source: String = "manual", // manual / device / phone_assisted
    val manualEntry: Boolean = true,
    val deviceVerified: Boolean = false,
    val standardVersion: String = "",
    val score: Int = 0,            // 正式分数（仅 scoreValid 时有效）
    val scoreValid: Boolean = false,
    val note: String = "",
    val profileId: String = "local"
)
