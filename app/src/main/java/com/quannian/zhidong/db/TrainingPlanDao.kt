package com.quannian.zhidong.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/** V2 训练计划任务 数据访问。 */
@Dao
interface TrainingPlanDao {

    @Insert
    suspend fun insertAll(tasks: List<TrainingPlanTask>)

    @Insert
    suspend fun insert(task: TrainingPlanTask): Long

    @Update
    suspend fun update(task: TrainingPlanTask)

    /** 按计划取全部任务（按 dayIndex 升序）。 */
    @Query("SELECT * FROM training_plan_tasks WHERE planId = :planId ORDER BY dayIndex ASC, id ASC")
    suspend fun byPlan(planId: String): List<TrainingPlanTask>

    /** 按 id 取单个。 */
    @Query("SELECT * FROM training_plan_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): TrainingPlanTask?

    /** 更新单个任务状态（done/skipped/rescheduled 写 completedAt）。 */
    @Query("UPDATE training_plan_tasks SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String, completedAt: Long)

    /** 删除某计划全部任务（重新生成前清理）。 */
    @Query("DELETE FROM training_plan_tasks WHERE planId = :planId")
    suspend fun clearByPlan(planId: String)

    /** 最近一个计划的 id（用户当前备考周期）。 */
    @Query("SELECT planId FROM training_plan_tasks ORDER BY id DESC LIMIT 1")
    suspend fun latestPlanId(): String?
}
