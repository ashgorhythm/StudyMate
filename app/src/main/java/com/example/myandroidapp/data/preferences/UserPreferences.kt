package com.example.myandroidapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studymate_prefs")

object PreferencesKeys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val STUDENT_NAME = stringPreferencesKey("student_name")
    val FOLDER_SETUP_SHOWN = booleanPreferencesKey("folder_setup_shown")

    // Interface & Personalization
    val THEME_MODE = stringPreferencesKey("theme_mode") // "System", "Light", "Dark"
    val ACCENT_COLOR_HEX = stringPreferencesKey("accent_color_hex")
    val TEXT_SCALE = floatPreferencesKey("text_scale")
    val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")

    // Security & Focus
    val REQUIRE_PIN = booleanPreferencesKey("require_pin")
    val LOCK_DURING_FOCUS = booleanPreferencesKey("lock_during_focus")
    val DND_DURING_FOCUS = booleanPreferencesKey("dnd_during_focus")
    val APP_PIN = stringPreferencesKey("app_pin")

    // Beta
    val BETA_ENROLLED = booleanPreferencesKey("beta_enrolled")

    // Backup history
    val BACKUP_HISTORY_JSON = stringPreferencesKey("backup_history_json")
}

class UserPreferences(private val context: Context) {

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    val studentName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.STUDENT_NAME] ?: "Student"
    }

    val folderSetupShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FOLDER_SETUP_SHOWN] ?: false
    }

    // Interface & Personalization
    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.THEME_MODE] ?: "System"
    }

    val accentColorHex: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACCENT_COLOR_HEX] ?: "#13ECEC"
    }

    val textScale: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TEXT_SCALE] ?: 1.0f
    }

    val reduceAnimations: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.REDUCE_ANIMATIONS] ?: false
    }

    // Security & Focus
    val requirePin: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.REQUIRE_PIN] ?: false
    }

    val lockDuringFocus: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LOCK_DURING_FOCUS] ?: true
    }

    val dndDuringFocus: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DND_DURING_FOCUS] ?: true
    }

    val appPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.APP_PIN] ?: ""
    }

    // Beta
    val betaEnrolled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.BETA_ENROLLED] ?: false
    }

    // Backup history
    val backupHistoryJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.BACKUP_HISTORY_JSON] ?: "[]"
    }

    suspend fun completeOnboarding(name: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ONBOARDING_COMPLETED] = true
            prefs[PreferencesKeys.STUDENT_NAME] = name
        }
    }

    suspend fun updateName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.STUDENT_NAME] = name
        }
    }

    suspend fun markFolderSetupShown() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOLDER_SETUP_SHOWN] = true
        }
    }

    // Interface settings
    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
    }

    suspend fun updateAccentColor(hex: String) {
        context.dataStore.edit { it[PreferencesKeys.ACCENT_COLOR_HEX] = hex }
    }

    suspend fun updateTextScale(scale: Float) {
        context.dataStore.edit { it[PreferencesKeys.TEXT_SCALE] = scale }
    }

    suspend fun updateReduceAnimations(reduce: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.REDUCE_ANIMATIONS] = reduce }
    }

    // Security settings
    suspend fun updateRequirePin(require: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.REQUIRE_PIN] = require }
    }

    suspend fun updateLockDuringFocus(lock: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.LOCK_DURING_FOCUS] = lock }
    }

    suspend fun updateDndDuringFocus(dnd: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DND_DURING_FOCUS] = dnd }
    }

    suspend fun updateAppPin(pin: String) {
        context.dataStore.edit { it[PreferencesKeys.APP_PIN] = pin }
    }

    // Beta
    suspend fun updateBetaEnrolled(enrolled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BETA_ENROLLED] = enrolled }
    }

    // Backup history
    suspend fun updateBackupHistory(json: String) {
        context.dataStore.edit { it[PreferencesKeys.BACKUP_HISTORY_JSON] = json }
    }

    // Clear all preferences
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
