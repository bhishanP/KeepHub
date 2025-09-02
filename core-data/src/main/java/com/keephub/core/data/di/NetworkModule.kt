package com.keephub.core.data.di

import com.keephub.core.data.BuildConfig
import com.keephub.core.data.net.*
import com.keephub.core.data.repo.DictionaryService
import com.keephub.core.data.repo.TranslateService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun okHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .apply {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
            .build()

    @Provides @Singleton
    fun dictionaryService(client: OkHttpClient): DictionaryService =
        FreeDictionaryApiService(client, baseUrl = BuildConfig.DICT_BASE_URL)

    @Provides @Singleton
    fun translateService(client: OkHttpClient): TranslateService =
        if (!BuildConfig.ENABLE_TRANSLATION) {
            // No-op implementation: never called by repo when disabled, but safe fallback.
            object : TranslateService {
                override suspend fun translate(text: String, target: String, source: String) = text
            }
        } else run {
            LibreTranslateService(
                client,
                baseUrl = BuildConfig.TRANSLATE_BASE_URL,
                apiKey = BuildConfig.TRANSLATE_API_KEY.ifBlank { null }
            )
        }

}
