package com.quannian.zhidong.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * V2 体测备考训练计划的单个每日任务（Room，全本地）。
 *
 *  设计原则（任务 6）：
 *   - 计划由"数据模型 + 规则"生成，不依赖大模型自由文本（任务 6 明确要求）。
 *   - [planId] 把一批同日生成的任务归组；[dayIndex] 在周期内的第几天（0 起）。
 *   - [status]：pending / done / skipped / rescheduled。
 *   - [weakItemKey] 关联薄弱项，[videoAsset] 关联教学视频，[steps] 为训练步骤文本。
 *   - 安全边界：[caution] 固定携带"不适即停"提示，见 [TrainingPlanService]。
 */
@Entity(
    tableName = "training_plan_tasks",
    indices = [androidx.room.Index(value = ["planId"])]
)
data class TrainingPlanTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val planId: String,
    val dayIndex: Int,
    val itemKey: String,            // 关联 BodyTestItemKey
    val weakItemKey: String,        // 触发本任务的薄弱项
    val title: String,
    val steps: String,
    val videoAsset: String,         // 可空 ""（无视频，仅方法说明）
    val durationMin: Int,
    val caution: String,
    val status: String = "pending", // pending / done / skipped / rescheduled
    val completedAt: Long = 0L,
    val profileId: String = "local"
)
