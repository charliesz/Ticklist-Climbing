package com.charlie.ticklist.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(
    name = "app_settings"
)

data class AppSettings(
    val hapticFeedbackEnabled: Boolean = true,
    val statusConfirmationDurationMs: Int = 1500,
    val darkModeEnabled: Boolean = false,
    val celebrationMessagesEnabled: Boolean = true
)

class AppSettingsRepository(
    private val context: Context
) {
    private object Keys {
        val hapticFeedbackEnabled =
            booleanPreferencesKey("haptic_feedback_enabled")

        val statusConfirmationDurationMs =
            intPreferencesKey("status_confirmation_duration_ms")

        val darkModeEnabled =
            booleanPreferencesKey("dark_mode_enabled")

        val celebrationMessagesEnabled =
            booleanPreferencesKey("celebration_messages_enabled")
    }

    val settings: Flow<AppSettings> =
        context.appSettingsDataStore.data.map { preferences ->
            AppSettings(
                hapticFeedbackEnabled =
                    preferences[Keys.hapticFeedbackEnabled] ?: true,

                statusConfirmationDurationMs =
                    preferences[Keys.statusConfirmationDurationMs] ?: 1500,

                darkModeEnabled =
                    preferences[Keys.darkModeEnabled] ?: false,

                celebrationMessagesEnabled =
                    preferences[Keys.celebrationMessagesEnabled] ?: true
            )
        }

    suspend fun setHapticFeedbackEnabled(
        enabled: Boolean
    ) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.hapticFeedbackEnabled] = enabled
        }
    }

    suspend fun setStatusConfirmationDurationMs(
        durationMs: Int
    ) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.statusConfirmationDurationMs] = durationMs
        }
    }

    suspend fun setDarkModeEnabled(
        enabled: Boolean
    ) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.darkModeEnabled] = enabled
        }
    }

    suspend fun setCelebrationMessagesEnabled(
        enabled: Boolean
    ) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.celebrationMessagesEnabled] = enabled
        }
    }
}
