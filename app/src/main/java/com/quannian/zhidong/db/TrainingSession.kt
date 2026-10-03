package com.quannian.zhidong.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一次训练会话（持久化到本地 Room 数据库，对照 spec §21）。
 *
 *  全本地，无后端/云同步（符合 MVP 要求）。
 */
@Entity(tableName = "training_sessions")
data class TrainingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ageGroup: String,
    val exerciseId: String,
    val exerciseName: String,
    val dateTime: Long,
    val durationSec: Int,
    val repetitions: Int,
    val targetReps: Int,
    val score: Int,
    val correctionCount: Int,
    val averageScore: Int
)
