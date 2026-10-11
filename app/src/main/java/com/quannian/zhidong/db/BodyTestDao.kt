package com.quannian.zhidong.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * V2 体测成绩 + 训练计划任务 数据访问（Room，全本地）。
 *  与 [TrainingSessionDao] 同库，但独立表，不破坏旧训练记录。
 */
@Dao
interface BodyTestDao {

    @Insert
    suspend fun insert(result: BodyTestResult): Long

    /** 本地单用户档案 id（MVP 无账号体系，任务 9 明确要求）。 */
    companion object {
        const val LOCAL_PROFILE = "local"
    }

    /** 某个 itemKey 最近一次成绩（按测试日期倒序）。 */
    @Query(
        "SELECT * FROM body_test_results WHERE itemKey = :itemKey AND profileId = :profileId " +
            "ORDER BY testDate DESC LIMIT 1"
    )
    suspend fun latest(itemKey: String, profileId: String): BodyTestResult?

    /** 某个 itemKey 的全部历史（趋势图用，按日期升序）。 */
    @Query(
        "SELECT * FROM body_test_results WHERE itemKey = :itemKey AND profileId = :profileId " +
            "ORDER BY testDate ASC"
    )
    suspend fun history(itemKey: String, profileId: String): List<BodyTestResult>

    /** 全部成绩（报告聚合用）。 */
    @Query("SELECT * FROM body_test_results WHERE profileId = :profileId ORDER BY testDate ASC")
    suspend fun all(profileId: String): List<BodyTestResult>

    @Query("DELETE FROM body_test_results WHERE profileId = :profileId")
    suspend fun clearAll(profileId: String): Int
}

/** 某 itemKey 的历史趋势点（趋势图）。 */
data class TrendPoint(
    val testDate: Long,
    val value: Double,
    val unit: String
)
