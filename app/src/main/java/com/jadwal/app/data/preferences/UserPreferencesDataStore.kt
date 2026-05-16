package com.jadwal.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jadwal_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_SETUP_DONE = booleanPreferencesKey("setup_done")
        val KEY_DAILY_HOURS = intPreferencesKey("daily_hours")
        val KEY_PREFERRED_TIME = stringPreferencesKey("preferred_time")
        val KEY_STREAK_DAYS = intPreferencesKey("streak_days")
        val KEY_LAST_STUDY_DATE = longPreferencesKey("last_study_date")
        val KEY_NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val KEY_NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_LANGUAGE_CODE = stringPreferencesKey("language_code")
        // إصلاح: حفظ حالة طلب إذن الإشعارات حتى لا يتكرر الطلب
        val KEY_NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
        // حفظ مسار صورة الملف الشخصي
        val KEY_PROFILE_PHOTO_PATH = stringPreferencesKey("profile_photo_path")
    }

    // ===== إذن الإشعارات =====

    val notificationPermissionAsked: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_NOTIFICATION_PERMISSION_ASKED] ?: false }

    suspend fun setNotificationPermissionAsked(asked: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATION_PERMISSION_ASKED] = asked }
    }

    // ===== صورة الملف الشخصي =====

    val profilePhotoPath: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PROFILE_PHOTO_PATH] ?: "" }

    suspend fun setProfilePhotoPath(path: String) {
        dataStore.edit { it[KEY_PROFILE_PHOTO_PATH] = path }
    }

    // ===== اللغة =====

    val languageCode: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LANGUAGE_CODE] ?: "" }

    suspend fun setLanguageCode(code: String) {
        dataStore.edit { it[KEY_LANGUAGE_CODE] = code }
    }

    // ===== الثيم =====

    val themeMode: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_THEME_MODE] ?: "SYSTEM" }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    // ===== Onboarding =====

    val onboardingDone: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    // ===== Setup =====

    val setupDone: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_SETUP_DONE] ?: false }

    suspend fun setSetupDone(done: Boolean) {
        dataStore.edit { it[KEY_SETUP_DONE] = done }
    }

    // ===== اسم المستخدم =====

    val userName: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_USER_NAME] ?: "" }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun getUserName(): String = userName.first()

    // ===== ساعات الدراسة =====

    val dailyHours: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DAILY_HOURS] ?: 2 }

    suspend fun setDailyHours(hours: Int) {
        dataStore.edit { it[KEY_DAILY_HOURS] = hours }
    }

    // ===== وقت الدراسة المفضل =====

    val preferredTime: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PREFERRED_TIME] ?: "MORNING" }

    suspend fun setPreferredTime(time: String) {
        dataStore.edit { it[KEY_PREFERRED_TIME] = time }
    }

    // ===== الإشعارات =====

    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }

    val notificationHour: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_NOTIFICATION_HOUR] ?: 8 }

    val notificationMinute: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_NOTIFICATION_MINUTE] ?: 0 }

    suspend fun setNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        dataStore.edit {
            it[KEY_NOTIFICATIONS_ENABLED] = enabled
            it[KEY_NOTIFICATION_HOUR] = hour
            it[KEY_NOTIFICATION_MINUTE] = minute
        }
    }

    // ===== Streak =====

    val streakDays: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_STREAK_DAYS] ?: 0 }

    suspend fun updateStreak() {
        val now = System.currentTimeMillis()
        val lastDate = dataStore.data.first()[KEY_LAST_STUDY_DATE] ?: 0L
        val dayMillis = 24 * 60 * 60 * 1000L
        val currentStreak = dataStore.data.first()[KEY_STREAK_DAYS] ?: 0

        dataStore.edit { prefs ->
            prefs[KEY_LAST_STUDY_DATE] = now
            prefs[KEY_STREAK_DAYS] = when {
                lastDate == 0L -> 1
                now - lastDate < dayMillis * 2 -> currentStreak + 1
                else -> 1
            }
        }
    }
}
