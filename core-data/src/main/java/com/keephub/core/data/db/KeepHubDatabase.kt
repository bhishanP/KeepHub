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
    version = 3,
    exportSchema = false,
    entities = [
        WordEntity::class, SenseEntity::class, TranslationEntity::class,
        SrsStateEntity::class, QuizResultEntity::class, ReviewSessionEntity::class
    ]
)
@TypeConverters(Converters::class)
abstract class KeepHubDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun senseDao(): SenseDao
    abstract fun translationDao(): TranslationDao
    abstract fun srsDao(): SrsDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun reviewSessionDao(): ReviewSessionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE senses ADD COLUMN audioUrls TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS review_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        completedAt INTEGER NOT NULL,
                        totalQuestions INTEGER NOT NULL,
                        correctCount INTEGER NOT NULL,
                        elapsedMs INTEGER NOT NULL,
                        isPractice INTEGER NOT NULL,
                        story TEXT,
                        storyWordsJson TEXT NOT NULL DEFAULT '[]',
                        itemsJson TEXT NOT NULL DEFAULT '[]'
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_sessions_completedAt ON review_sessions (completedAt)")
            }
        }
    }
}
