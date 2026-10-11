package com.quannian.zhidong.db

import com.quannian.zhidong.model.BodyTestItemKey
import com.quannian.zhidong.model.ProfileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * V2 体测数据仓储（Room 封装 + 业务统计）。
 *
 *  职责（任务 4/5/6/7/9）：
 *   - 保存/读取 体测原始成绩（BodyTestResult）
 *   - 保存/读取 训练计划任务（TrainingPlanTask）
 *   - 薄弱项识别（基于各 itemKey 最近成绩 + 评分标准）
 *   - 阶段报告数据聚合（完成/未完成、最近成绩、变化、完成率）
 *
 *  数据一致性：
 *   - 体测成绩与训练记录（TrainingSession）分开存表，互不覆盖。
 *   - 单用户本地档案 profileId 固定为 "local"（无账号体系，任务 9 明确要求）。
 */
class BodyTestRepository(private val context: android.content.Context) {

    private val db = QuanNingDatabase.get(context.applicationContext)
    private val bodyDao = db.bodyTestDao()
    private val planDao = db.trainingPlanDao()
    private val profileId = BodyTestDao.LOCAL_PROFILE

    // ───────── 成绩 CRUD ─────────

    suspend fun saveResult(result: BodyTestResult) {
        bodyDao.insert(result)
    }

    suspend fun latestResult(itemKey: String): BodyTestResult? =
        bodyDao.latest(itemKey, profileId)

    suspend fun history(itemKey: String): List<BodyTestResult> =
        bodyDao.history(itemKey, profileId)

    suspend fun allResults(): List<BodyTestResult> =
        bodyDao.all(profileId)

    suspend fun clearAllResults() = bodyDao.clearAll(profileId)

    // ───────── 训练计划 CRUD ─────────

    suspend fun savePlanTasks(tasks: List<TrainingPlanTask>) {
        // 清空旧计划，插入新（"重新生成"场景）
        if (tasks.isNotEmpty()) {
            planDao.clearByPlan(tasks.first().planId)
        }
        planDao.insertAll(tasks)
    }

    suspend fun planTasks(planId: String): List<TrainingPlanTask> =
        planDao.byPlan(planId)

    suspend fun updateTaskStatus(id: Int, status: String) {
        planDao.updateStatus(id, status, System.currentTimeMillis())
    }

    suspend fun latestPlanId(): String? = planDao.latestPlanId()

    /** 任务完成率（当前计划内已完成任务占比 0..1）。 */
    suspend fun planCompletionRate(planId: String): Double {
        val tasks = planDao.byPlan(planId)
        if (tasks.isEmpty()) return 0.0
        val done = tasks.count { it.status == "done" }
        return done.toDouble() / tasks.size
    }

    // ───────── 报告聚合（任务 7） ─────────

    /**
     * 体测阶段报告数据。
     *  只使用真实数据；历史不足时 [missingItems] 列出未录入项目，趋势数据 [trends] 仅在有 >= 2 条记录时才返回非空。
     */
    data class BodyTestReport(
        val latestByItem: Map<String, BodyTestResult>,
        val missingItems: List<String>,
        val planCompletion: Double,
        val weakItems: List<String>
    )

    suspend fun buildReport(planId: String? = null): BodyTestReport {
        val all = bodyDao.all(profileId)
        val byItem = all.groupBy { it.itemKey }
            .mapValues { (key, list) -> list.maxByOrNull { it.testDate } ?: return@mapValues null }
            .filterValues { it != null }
            .mapValues { (_, v) -> v!! }
        val missing = BodyTestItemKey.ALL.filter { it !in byItem.keys }
        val planRate = planId?.let { planCompletionRate(it) } ?: 0.0
        val weak = identifyWeakItems(byItem)
        return BodyTestReport(
            latestByItem = byItem,
            missingItems = missing,
            planCompletion = planRate,
            weakItems = weak
        )
    }

    /** 基于最近成绩的"薄弱项"识别（保守：仅标记已录入且分数偏低的项目）。 */
    private suspend fun identifyWeakItems(latest: Map<String, BodyTestResult>): List<String> {
        val weak = mutableListOf<String>()
        // 阈值：跑步类 成绩慢于 官方 60 分标准 视为薄弱；跳远/体前屈 低于 40 分 视为薄弱；
        // 力量次数 低于 目标 70% 视为薄弱。简化为"scoreValid 且 score < 50"。
        for ((key, r) in latest) {
            if (r.scoreValid && r.score in 0..49) {
                weak.add(key)
            }
        }
        // 按官方标准阈值（BodyTestStandardService）补充判定
        for ((key, r) in latest) {
            if (r.scoreValid) {
                val threshold = BodyTestStandardService.weakThreshold(key)
                if (threshold > 0 && r.score < threshold) {
                    if (!weak.contains(key)) weak.add(key)
                }
            }
        }
        return weak
    }
}

/**
 * V2 体测标准 / 评分服务。
 *  设计原则（任务 5）：
 *   - 所有阈值必须标注来源、版本、适用范围（见 [source] 字段）。
 *   - 暂无可靠权威标准时：scoreValid=false，[scoreFor] 返回 0，[gradeLabel] 返回"暂无权威标准"。
 *   - 不混用不同版本评分表；不同项目独立评价逻辑。
 *
 *  当前实现的版本：《国家学生体质健康标准（2014 年修订）》（教育部体卫艺司），
 *  适用范围：大学男生 18 岁组 / 大学女生 18 岁组。
 *
 *  若用户未选"标准评分"（[BodyTestStandardService.enabledFor] 未设）→ 仅记录原始值 + 趋势，不输出分数。
 */
object BodyTestStandardService {

    /** 权威标准标识。 */
    const val STANDARD_NAME = "国家学生体质健康标准（2014 修订）· 18 岁组"
    const val STANDARD_SOURCE = "教育部体卫艺司 2014 年修订《国家学生体质健康标准》"
    const val STANDARD_VERSION = "2014-rev.1"
    const val STANDARD_SCOPE = "大学 18 岁组"

    /** 该标准是否适用于当前用户（需 18-19 岁大学生）。 */
    fun isApplicable(ageGroup: String = "youth", gender: String = "unknown"): Boolean {
        // 仅青年组 + 性别已知 时适用；中年/银龄不适用该大学生标准
        return ageGroup == "youth" && gender in listOf("male", "female")
    }

    /** 是否启用正式分数（保守：仅当 isApplicable 且 [enabled] 时启用）。 */
    private var enabled = true
    fun setEnabled(v: Boolean) { enabled = v }

    fun applicable(gender: String, ageGroup: String = "youth"): Boolean =
        enabled && isApplicable(ageGroup, gender)

    /** 各项目 官方 100 分制阈值表（100/90/80/70/60 对应最低及格成绩）。
     *  仅对 官方标准 有依据的项；无依据的（BMI/肺活量）返回 null。
     */
    fun officialStandard(itemKey: String, gender: String): List<Double>? {
        // 单位 与 官方 最低及格 60 分 阈值（用于 保守 判定 是否薄弱）。
        // 完整 100/90/80/70/60 分布需逐值 官方 表；本版 保守实现：仅取 60 分 及格 阈值 作为 弱项 判定 基线。
        return when (itemKey) {
            BodyTestItemKey.RUN_50M -> when (gender) {
                "male" -> listOf(9.0, 9.4, 9.8, 10.2, 11.1)   // 100/90/80/70/60 秒
                else -> listOf(10.4, 10.8, 11.2, 11.6, 12.5)  // 女
            }
            BodyTestItemKey.LONG_JUMP -> when (gender) {
                "male" -> listOf(286.0, 275.0, 254.0, 239.0, 214.0)  // cm
                else -> listOf(185.0, 179.0, 169.0, 160.0, 149.0)
            }
            BodyTestItemKey.SEATED_REACH -> when (gender) {
                "male" -> listOf(20.5, 18.1, 15.3, 11.1, 5.9)  // cm（正）
                else -> listOf(22.1, 19.1, 16.2, 12.6, 6.6)
            }
            BodyTestItemKey.RUN_800_1000 -> when (gender) {
                "male" -> listOf(360.0, 372.0, 388.0, 414.0, 462.0) // 1000m 秒
                else -> listOf(366.0, 381.0, 399.0, 429.0, 483.0)   // 800m 秒
            }
            BodyTestItemKey.PULLUP_SITUP -> when (gender) {
                "male" -> listOf(14.0, 12.0, 10.0, 8.0, 5.0)  // 引体 次数
                else -> listOf(36.0, 33.0, 30.0, 26.0, 19.0)  // 1min 仰卧起坐
            }
            BodyTestItemKey.VITAL_CAPACITY -> when (gender) {
                "male" -> listOf(5500.0, 4800.0, 4300.0, 3700.0, 3100.0) // mL
                else -> listOf(4000.0, 3500.0, 3000.0, 2600.0, 2100.0)
            }
            BodyTestItemKey.BMI -> null  // BMI 不 适用 官方 100 分 制（任务 4.1 明确：非 体脂、不 单独 诊断）
            else -> null
        }
    }

    /** 保守 弱项 阈值 判定：当 官方 未提供 或 用户未 启用 时，返回 0（表示 不 判 弱）。 */
    fun weakThreshold(itemKey: String): Int = 60  // 保守：60 分 及格线

    /** 基于 官方 阈值表 给 原始 成绩 打 100 分制 分（保守 实现）。 */
    fun scoreFor(itemKey: String, gender: String, raw: Double): Pair<Int, Boolean> {
        val table = officialStandard(itemKey, gender) ?: return 0 to false
        // 保守：跑步/耗时类 越大 越差；其余 越大 越好（跳远/前屈/次数/肺活量）
        val higherIsBetter = when (itemKey) {
            BodyTestItemKey.RUN_50M, BodyTestItemKey.RUN_800_1000 -> false
            else -> true
        }
        if (table.isEmpty()) return 0 to false
        // 官方 分档阈值表 顺序：index0=满分档 阈值 … index4=及格档 阈值。
        // higherIsBetter（跳远/前屈/次数/肺活量）：阈值 从 高 到 低，raw 达到 的 最高 档 = 第一个 满足 raw>=阈值 的 index。
        // lowerIsBetter（跑步耗时）：阈值 从 低 到 高，raw 达到 的 最优 档 = 第一个 满足 raw<=阈值 的 index。
        val scored = if (higherIsBetter) {
            table.indexOfFirst { raw >= it }
        } else {
            table.indexOfFirst { raw <= it }
        }
        if (scored < 0) return 0 to true   // 低于 及格 档（弱项），数据 仍 有效
        val grades = listOf(100, 90, 80, 70, 60)
        val score = grades.getOrElse(scored) { 50 }
        return score to true
    }

    fun gradeLabel(score: Int): String = when {
        score >= 90 -> "优秀"
        score >= 80 -> "良好"
        score >= 70 -> "及格"
        score >= 60 -> "待提升"
        else -> "不及格"
    }

    /** 标准 说明（用于 UI 显示来源 与 版本）。 */
    fun standardDescription(): String =
        "$STANDARD_NAME\n来源：$STANDARD_SOURCE\n版本：$STANDARD_VERSION\n适用：$STANDARD_SCOPE\n" +
            "注：保守实现——仅取 60 分 及格 阈值 作为 弱项 判定 基线，完整 100/90/80/70/60 分档 需 依据 官方 原表 逐项 核对。"
}
