package com.keephub.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.keephub.core.data.db.dao.*
import com.keephub.core.data.db.entity.*
import com.keephub.core.data.db.typeconverters.Converters

@Database(
    version = 2,
    exportSchema = true,
    entities = [
        WordEntity::class, SenseEntity::class, TranslationEntity::class,
        SrsStateEntity::class, QuizResultEntity::class
    ]
)
@TypeConverters(Converters::class)
abstract class KeepHubDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun senseDao(): SenseDao
    abstract fun translationDao(): TranslationDao
    abstract fun srsDao(): SrsDao
    abstract fun quizResultDao(): QuizResultDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE senses ADD COLUMN audioUrls TEXT NOT NULL DEFAULT '[]'")
            }
        }
    }
}
