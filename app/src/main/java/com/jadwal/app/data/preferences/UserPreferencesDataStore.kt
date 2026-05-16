package com.jadwal.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension للـ DataStore على Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jadwal_prefs"
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ─── Keys ────────────────────────────────────────────────

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_LANGUAGE = stringPreferencesKey("language")         // "ar" | "en"
        val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val KEY_IS_ONBOARDING_DONE = booleanPreferencesKey("is_onboarding_done")
        val KEY_IS_SETUP_DONE = booleanPreferencesKey("is_setup_done")
        val KEY_DAILY_HOURS = intPreferencesKey("daily_hours")
        val KEY_PREFERRED_TIME = stringPreferencesKey("preferred_time") // "MORNING" | "EVENING" | "NIGHT"
        val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val KEY_NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val KEY_NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val KEY_STREAK_DAYS = intPreferencesKey("streak_days")
        val KEY_LAST_ACTIVE_DATE = stringPreferencesKey("last_active_date") // "2025-01-15"
    }

    // ─── Flows (للقراءة التفاعلية) ───────────────────────────

    val userName: Flow<String> = context.dataStore.data
        .map { it[KEY_USER_NAME] ?: "" }

    val language: Flow<String> = context.dataStore.data
        .map { it[KEY_LANGUAGE] ?: "ar" }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_IS_DARK_MODE] ?: false }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_IS_ONBOARDING_DONE] ?: false }

    val isSetupDone: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_IS_SETUP_DONE] ?: false }

    val dailyHours: Flow<Int> = context.dataStore.data
        .map { it[KEY_DAILY_HOURS] ?: 2 }

    val preferredTime: Flow<String> = context.dataStore.data
        .map { it[KEY_PREFERRED_TIME] ?: "MORNING" }

    val notificationEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_NOTIFICATION_ENABLED] ?: true }

    val notificationHour: Flow<Int> = context.dataStore.data
        .map { it[KEY_NOTIFICATION_HOUR] ?: 8 }

    val notificationMinute: Flow<Int> = context.dataStore.data
        .map { it[KEY_NOTIFICATION_MINUTE] ?: 0 }

    // ─── Suspend Getters (للقراءة لمرة واحدة) ───────────────

    suspend fun getUserName(): String =
        context.dataStore.data.map { it[KEY_USER_NAME] ?: "" }
            .let { flow -> var result = ""; flow.collect { result = it; }; result }

    // ─── Setters ─────────────────────────────────────────────

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_IS_DARK_MODE] = enabled }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_IS_ONBOARDING_DONE] = true }
    }

    suspend fun setSetupDone() {
        context.dataStore.edit { it[KEY_IS_SETUP_DONE] = true }
    }

    suspend fun setDailyHours(hours: Int) {
        context.dataStore.edit { it[KEY_DAILY_HOURS] = hours }
    }

    suspend fun setPreferredTime(time: String) {
        context.dataStore.edit { it[KEY_PREFERRED_TIME] = time }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[KEY_NOTIFICATION_HOUR] = hour
            it[KEY_NOTIFICATION_MINUTE] = minute
        }
    }

    suspend fun setLastActiveDate(date: String) {
        context.dataStore.edit { it[KEY_LAST_ACTIVE_DATE] = date }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
