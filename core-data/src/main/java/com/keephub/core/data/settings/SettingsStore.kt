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
}

class SettingsStore(private val context: Context) {
    val translationLang: Flow<String> =
        context.dataStore.data.map { it[Keys.TRANSLATION_LANG] ?: "en" }
    val dailyGoal: Flow<Int> =
        context.dataStore.data.map { it[Keys.DAILY_GOAL] ?: 20 }
    val quizModes: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.QUIZ_MODES] ?: setOf("MCQ","TYPE","CLOZE") }
    val notifyHour: Flow<Int> =
        context.dataStore.data.map { it[Keys.NOTIFY_HOUR] ?: 19 }

    suspend fun setTranslationLang(code: String) =
        context.dataStore.edit { it[Keys.TRANSLATION_LANG] = code }

    suspend fun setDailyGoal(goal: Int) =
        context.dataStore.edit { it[Keys.DAILY_GOAL] = goal.coerceIn(1, 200) }

    suspend fun setQuizModes(modes: Set<String>) =
        context.dataStore.edit { it[Keys.QUIZ_MODES] = modes }

    suspend fun setNotifyHour(hour24: Int) =
        context.dataStore.edit { it[Keys.NOTIFY_HOUR] = hour24.coerceIn(0, 23) }
}
