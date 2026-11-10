package com.keephub.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private val Context.dataStore by preferencesDataStore(name = "keephub_settings")

object Keys {
    val TRANSLATION_LANG = stringPreferencesKey("translation_lang")     // e.g., "hi"
    val DAILY_GOAL       = intPreferencesKey("daily_goal")              // e.g., 20
    val QUIZ_MODES       = stringSetPreferencesKey("quiz_modes")        // MCQ/TYPE/CLOZE
    val NOTIFY_HOUR      = intPreferencesKey("notify_hour")             // 24h local
    val THEME_MODE       = stringPreferencesKey("theme_mode")           // system | light | dark
    val DYNAMIC_COLOR    = booleanPreferencesKey("dynamic_color")       // true | false
    val GEMINI_API_KEY   = stringPreferencesKey("gemini_api_key")       // BYOK key
    val DAILY_WORDS_LEVEL = stringPreferencesKey("daily_words_level")   // BEGINNER | INTERMEDIATE | ADVANCED
    val LAST_DAILY_WORDS_DATE = stringPreferencesKey("last_daily_words_date") // YYYY-MM-DD
    val LAST_DAILY_WORDS_REVIEWED_DATE = stringPreferencesKey("last_daily_words_reviewed_date") // YYYY-MM-DD
    val DAILY_WORDS_AUTO_ADD = booleanPreferencesKey("daily_words_auto_add")   // default: true
    val AI_DAILY_WORDS_CACHE = stringPreferencesKey("ai_daily_words_cache")
    val AI_DAILY_WORDS_DATE = stringPreferencesKey("ai_daily_words_date")
    val AI_DAILY_WORDS_LEVEL = stringPreferencesKey("ai_daily_words_level")
}

class SettingsStore(private val context: Context) {
    val translationLang: Flow<String> =
        context.dataStore.data.map { it[Keys.TRANSLATION_LANG] ?: "en" }
    val dailyGoal: Flow<Int> =
        context.dataStore.data.map { it[Keys.DAILY_GOAL] ?: 20 }
    val quizModes: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.QUIZ_MODES] ?: setOf("MCQ","TYPE","CLOZE","AUDIO") }
    val notifyHour: Flow<Int> =
        context.dataStore.data.map { it[Keys.NOTIFY_HOUR] ?: 19 }
    val themeMode: Flow<String> =
        context.dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }
    val dynamicColor: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }
    val geminiApiKey: Flow<String> =
        context.dataStore.data.map { it[Keys.GEMINI_API_KEY] ?: "" }
    val dailyWordsLevel: Flow<String> =
        context.dataStore.data.map { it[Keys.DAILY_WORDS_LEVEL] ?: "INTERMEDIATE" }
    val lastDailyWordsDate: Flow<String> =
        context.dataStore.data.map { it[Keys.LAST_DAILY_WORDS_DATE] ?: "" }
    val lastDailyWordsReviewedDate: Flow<String> =
        context.dataStore.data.map { it[Keys.LAST_DAILY_WORDS_REVIEWED_DATE] ?: "" }
    val dailyWordsAutoAdd: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DAILY_WORDS_AUTO_ADD] ?: true }
    val aiDailyWordsCache: Flow<String> =
        context.dataStore.data.map { it[Keys.AI_DAILY_WORDS_CACHE] ?: "" }
    val aiDailyWordsDate: Flow<String> =
        context.dataStore.data.map { it[Keys.AI_DAILY_WORDS_DATE] ?: "" }
    val aiDailyWordsLevel: Flow<String> =
        context.dataStore.data.map { it[Keys.AI_DAILY_WORDS_LEVEL] ?: "" }

    suspend fun setTranslationLang(code: String) =
        context.dataStore.edit { it[Keys.TRANSLATION_LANG] = code }

    suspend fun setDailyGoal(goal: Int) =
        context.dataStore.edit { it[Keys.DAILY_GOAL] = goal.coerceIn(1, 200) }

    suspend fun setQuizModes(modes: Set<String>) =
        context.dataStore.edit { it[Keys.QUIZ_MODES] = modes }

    suspend fun setNotifyHour(hour24: Int) =
        context.dataStore.edit { it[Keys.NOTIFY_HOUR] = hour24.coerceIn(0, 23) }

    suspend fun setThemeMode(mode: String) =
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }

    suspend fun setDynamicColor(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setGeminiApiKey(key: String) =
        context.dataStore.edit { it[Keys.GEMINI_API_KEY] = key.trim() }

    suspend fun setDailyWordsLevel(level: String) =
        context.dataStore.edit { it[Keys.DAILY_WORDS_LEVEL] = level.uppercase() }

    suspend fun setLastDailyWordsDate(dateStr: String) =
        context.dataStore.edit { it[Keys.LAST_DAILY_WORDS_DATE] = dateStr }

    suspend fun setLastDailyWordsReviewedDate(dateStr: String) =
        context.dataStore.edit { it[Keys.LAST_DAILY_WORDS_REVIEWED_DATE] = dateStr }

    suspend fun setDailyWordsAutoAdd(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DAILY_WORDS_AUTO_ADD] = enabled }

    suspend fun setAiDailyWords(cacheJson: String, dateStr: String, level: String) =
        context.dataStore.edit {
            it[Keys.AI_DAILY_WORDS_CACHE] = cacheJson
            it[Keys.AI_DAILY_WORDS_DATE] = dateStr
            it[Keys.AI_DAILY_WORDS_LEVEL] = level
        }

    suspend fun clearAiDailyWords() =
        context.dataStore.edit {
            it.remove(Keys.AI_DAILY_WORDS_CACHE)
            it.remove(Keys.AI_DAILY_WORDS_DATE)
            it.remove(Keys.AI_DAILY_WORDS_LEVEL)
        }
}
