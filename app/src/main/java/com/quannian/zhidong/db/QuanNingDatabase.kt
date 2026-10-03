package com.quannian.zhidong.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 训练记录数据库（Room）。单例，全本地。
 */
@Database(entities = [TrainingSession::class], version = 1, exportSchema = false)
abstract class QuanNingDatabase : RoomDatabase() {
    abstract fun trainingSessionDao(): TrainingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: QuanNingDatabase? = null

        fun get(context: Context): QuanNingDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuanNingDatabase::class.java,
                    "quannian_zhidong.db"
                ).build().also { INSTANCE = it }
            }
    }
}
