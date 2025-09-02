package com.keephub.core.data.di

import android.content.Context
import androidx.room.Room
import com.keephub.core.data.db.KeepHubDatabase
import com.keephub.core.data.db.typeconverters.Converters
import com.keephub.core.data.repo.*
import com.keephub.core.data.settings.SettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import com.keephub.core.data.util.WordNormalizer

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context, json: Json): KeepHubDatabase =
        Room.databaseBuilder(ctx, KeepHubDatabase::class.java, "keephub.db")
            .addTypeConverter(Converters(json))
            .addMigrations(KeepHubDatabase.MIGRATION_1_2)
            .build()

    @Provides fun provideWordDao(db: KeepHubDatabase) = db.wordDao()
    @Provides fun provideSenseDao(db: KeepHubDatabase) = db.senseDao()
    @Provides fun provideTranslationDao(db: KeepHubDatabase) = db.translationDao()
    @Provides fun provideSrsDao(db: KeepHubDatabase) = db.srsDao()
    @Provides fun provideQuizDao(db: KeepHubDatabase) = db.quizResultDao()

    @Provides @Singleton
    fun provideSettings(@ApplicationContext ctx: Context) = SettingsStore(ctx)

    @Provides @Singleton
    fun provideWordRepo(impl: WordRepositoryImpl): WordRepository = impl

    @Provides @Singleton
    fun provideNormalizer(): WordNormalizer = WordNormalizer()
}
