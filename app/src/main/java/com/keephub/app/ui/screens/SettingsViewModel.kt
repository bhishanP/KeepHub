package com.keephub.app.ui.screens

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.keephub.app.notifications.ReviewNotification
import com.keephub.app.work.DueReviewWorker
import com.keephub.core.data.net.GeminiApiService
import com.keephub.core.data.net.GeminiTestResult
import com.keephub.core.data.repo.WordRepository
import com.keephub.core.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val repo: WordRepository,
    private val gemini: GeminiApiService,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    // Expose flows to the UI
    val translationLang = store.translationLang
    val dailyGoal = store.dailyGoal
    val quizModes = store.quizModes
    val notifyHour = store.notifyHour
    val themeMode = store.themeMode
    val dynamicColor = store.dynamicColor
    val geminiApiKey = store.geminiApiKey
    val dailyWordsLevel = store.dailyWordsLevel
    val dailyWordsAutoAdd = store.dailyWordsAutoAdd

    val totalWordsCount: StateFlow<Int> =
        repo.observeAll().map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val difficultWordsCount: StateFlow<Int> =
        repo.observeDifficultWordIds().map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isReminderActive: StateFlow<Boolean> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(DueReviewWorker.UNIQUE_NAME)
            .map { list ->
                list.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // Mutators
    fun setLang(code: String) = launch { store.setTranslationLang(code.lowercase()) }
    fun setGoal(goal: Int)   = launch { store.setDailyGoal(goal) }
    fun setHour(h: Int)      = launch {
        store.setNotifyHour(h)
        DueReviewWorker.scheduleDailyReminder(context, h, ExistingWorkPolicy.REPLACE)
    }
    fun setThemeMode(mode: String) = launch { store.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launch { store.setDynamicColor(enabled) }
    fun setGeminiApiKey(key: String) = launch { store.setGeminiApiKey(key) }
    fun setDailyWordsLevel(level: String) = launch { store.setDailyWordsLevel(level) }
    fun setDailyWordsAutoAdd(enabled: Boolean) = launch { store.setDailyWordsAutoAdd(enabled) }

    suspend fun testGeminiConnection(key: String): Boolean = gemini.testConnection(key)
    suspend fun testGeminiConnectionDetailed(key: String): GeminiTestResult = gemini.testConnectionDetailed(key)
    fun sanitizeApiKey(key: String): String = gemini.sanitizeApiKey(key)

    fun sendTestNotification(): Boolean {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (enabled) {
            ReviewNotification.showTest(context)
        }
        return enabled
    }

    fun toggleMode(label: String, enabled: Boolean) = launch {
        val cur = quizModes.first().toMutableSet()
        if (enabled) cur += label else cur -= label
        if (cur.isEmpty()) cur += "MCQ"          // always keep at least one mode
        store.setQuizModes(cur)
    }

    fun resetReviewProgress(onDone: () -> Unit) = launch {
        repo.resetAllReviews()
        DueReviewWorker.scheduleDailyReminder(context, store.notifyHour.first(), ExistingWorkPolicy.REPLACE)
        onDone()
    }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }
}
