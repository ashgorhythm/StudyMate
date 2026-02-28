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
}

class UserPreferences(private val context: Context) {

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    val studentName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.STUDENT_NAME] ?: "Student"
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
}
