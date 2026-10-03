package com.quannian.zhidong.db

import com.quannian.zhidong.model.TrainingReport

/**
 * 训练记录数据访问 + 统计（Room）。把 [TrainingReport] 持久化到本地数据库，
 * 并提供"我的训练 / 成长曲线"所需的统计查询。全本地、无后端、无云同步。
 *
 *  放在 db 包内（与 [QuanNingDatabase] 同包），避免跨包 KSP 生成边界问题。
 */
class TrainingRepository(context: android.content.Context) {

    private val dao = QuanNingDatabase.get(context.applicationContext).trainingSessionDao()

    /** 保存一次训练结束的报告。 */
    suspend fun save(report: TrainingReport, exerciseId: String, exerciseName: String) {
        dao.insert(
            TrainingSession(
                ageGroup = report.ageGroup,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                dateTime = System.currentTimeMillis(),
                durationSec = report.durationSec,
                repetitions = report.repCount,
                targetReps = report.targetReps,
                score = report.overallScore,
                correctionCount = report.errorTypesSeen.size,
                averageScore = report.formScorePercent
            )
        )
    }

    /** 最近 [limit] 条记录（倒序）。 */
    suspend fun recent(limit: Int = 10): List<TrainingSession> = dao.recent(limit)

    /** 全部记录（累计统计用）。 */
    suspend fun all(): List<TrainingSession> = dao.all()

    /** 最近 [days] 天的按天统计（成长曲线）。 */
    suspend fun dailyStats(days: Int = 7): List<DailyStat> {
        val since = System.currentTimeMillis() - days * 86400_000L
        return dao.dailyStats(since)
    }

    /** 累计统计（总次数 / 总时长 / 平均得分 / 最近一次）。 */
    suspend fun summary(): Summary {
        val sessions = dao.all()
        if (sessions.isEmpty()) return Summary(0, 0, 0, 0, null)
        val totalReps = sessions.sumOf { it.repetitions }
        val totalDurSec = sessions.sumOf { it.durationSec }
        val avgScore = (sessions.map { it.score }.sum() / sessions.size).toInt()
        val last = sessions.maxByOrNull { it.dateTime }
        return Summary(
            sessionCount = sessions.size,
            totalRepetitions = totalReps,
            totalDurationSec = totalDurSec,
            avgScore = avgScore,
            lastSession = last
        )
    }

    data class Summary(
        val sessionCount: Int,
        val totalRepetitions: Int,
        val totalDurationSec: Int,
        val avgScore: Int,
        val lastSession: TrainingSession? = null
    )
}
