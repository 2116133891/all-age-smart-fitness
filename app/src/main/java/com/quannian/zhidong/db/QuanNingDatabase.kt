package com.quannian.zhidong.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 训练记录数据库（Room）。单例，全本地。
 *
 *  V2 升级（version 1 → 2）：新增
 *   - [BodyTestResult]  体测原始成绩（可追溯 + 标准版本 + 评分可选）
 *   - [TrainingPlanTask] 体测备考训练计划每日任务
 *  既有 [TrainingSession] 表**完整保留**（手工 [MIGRATION_1_2] 只建新表，不触碰旧表），
 *  满足任务 9"不得因 UI 更换删除旧训练记录"。
 *  使用手工 migration（CREATE TABLE IF NOT EXISTS），不依赖 AutoMigration / schema 导出。
 */
@Database(
    entities = [
        TrainingSession::class,
        BodyTestResult::class,
        TrainingPlanTask::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QuanNingDatabase : RoomDatabase() {
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun bodyTestDao(): BodyTestDao
    abstract fun trainingPlanDao(): TrainingPlanDao

    companion object {
        @Volatile
        private var INSTANCE: QuanNingDatabase? = null

        fun get(context: Context): QuanNingDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuanNingDatabase::class.java,
                    "quannian_zhidong.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }

        /**
         * 手工 migration（v1 → v2）：仅 建新表，不 触碰 旧 training_sessions 表。
         *  使用 CREATE TABLE IF NOT EXISTS，升级 已 装 旧 版 用户 时 幂等 安全。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `body_test_results` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`itemKey` TEXT NOT NULL, `ageGroup` TEXT NOT NULL, `gender` TEXT NOT NULL, " +
                        "`testDate` INTEGER NOT NULL, `rawValue` REAL NOT NULL, `rawValue2` REAL NOT NULL, " +
                        "`heightCm` REAL NOT NULL, `weightKg` REAL NOT NULL, `unit` TEXT NOT NULL, " +
                        "`source` TEXT NOT NULL, `manualEntry` INTEGER NOT NULL, " +
                        "`deviceVerified` INTEGER NOT NULL, `standardVersion` TEXT NOT NULL, " +
                        "`score` INTEGER NOT NULL, `scoreValid` INTEGER NOT NULL, `note` TEXT NOT NULL, " +
                        "`profileId` TEXT NOT NULL)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_body_test_results_itemKey` ON `body_test_results` (`itemKey`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_body_test_results_testDate` ON `body_test_results` (`testDate`)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `training_plan_tasks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `planId` TEXT NOT NULL, " +
                        "`dayIndex` INTEGER NOT NULL, `itemKey` TEXT NOT NULL, `weakItemKey` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `steps` TEXT NOT NULL, `videoAsset` TEXT NOT NULL, " +
                        "`durationMin` INTEGER NOT NULL, `caution` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                        "`completedAt` INTEGER NOT NULL, `profileId` TEXT NOT NULL)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_training_plan_tasks_planId` ON `training_plan_tasks` (`planId`)")
            }
        }
    }
}
