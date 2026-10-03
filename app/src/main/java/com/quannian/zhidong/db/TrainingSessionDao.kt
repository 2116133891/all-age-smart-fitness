package com.quannian.zhidong.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

/**
 * 训练记录数据访问。
 *  历史数据 / 成长趋势全部基于此（全本地）。
 */
@Dao
interface TrainingSessionDao {

    @Insert
    suspend fun insert(session: TrainingSession): Long

    /** 最近 [limit] 条记录（按时间倒序），用于"我的训练"和趋势图。 */
    @Query("SELECT * FROM training_sessions ORDER BY dateTime DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<TrainingSession>

    /** 全部记录（用于累计统计）。 */
    @Query("SELECT * FROM training_sessions ORDER BY dateTime ASC")
    suspend fun all(): List<TrainingSession>

    /** 最近 [days] 天内，按天聚合平均得分 + 次数（成长曲线用）。 */
    @Query(
        "SELECT CAST(dateTime/1000/86400 AS INTEGER) as day," +
            " AVG(score) as avgScore, SUM(repetitions) as totalReps, COUNT(*) as sessions" +
            " FROM training_sessions" +
            " WHERE dateTime >= :sinceMillis" +
            " GROUP BY day ORDER BY day ASC"
    )
    suspend fun dailyStats(sinceMillis: Long): List<DailyStat>
}

/** 按天聚合的统计结果。 */
data class DailyStat(
    val day: Long,
    val avgScore: Double,
    val totalReps: Long,
    val sessions: Long
)
