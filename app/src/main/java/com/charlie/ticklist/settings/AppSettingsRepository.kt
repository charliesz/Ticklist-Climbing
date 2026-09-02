package com.charlie.ticklist.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    val celebrationMessagesEnabled: Boolean = true,
    val manualSuccessCount: Int = 0
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

        val manualSuccessCount =
            intPreferencesKey("manual_success_count")

        val remainingCelebrationMessages =
            stringPreferencesKey("remaining_celebration_messages")
    }

    private val celebrationMessages = listOf(
        "Endlich mal festgehalten!",
        "Nicht Kletteräffchen - Klättermätzchen!",
        "Ne Frauen-Route war das nicht!",
        "Ballarina!",
        "Können die auch etwas schwieriges schrauben!",
        "Auch du hast es jetzt geschafft!",
        "Mega Dude!",
        "Fast so elegant wie Lotti!",
        "Reichweite ist auch eine Technik!",
        "Schön klettern kann jeder!",
        "Einfach war das nicht!",
        "Easy Peasy!",
        "Manche leute haben es einfach drauf!",
        "Lass es nächstes Mal schwieriger ausseh",
        "Ali Cato!",
        "Es kommt nicht immer auf die Größe an!",
        "Stabil!",
        "Das merkst du noch morgen!",
        "Endlich!",
        "Schön ist anders!",
        "Lassen wir gelten!"
    )

    private val messageSeparator = "\u001F"

    val settings: Flow<AppSettings> =
        context.appSettingsDataStore.data.map { preferences ->
            AppSettings(
                hapticFeedbackEnabled =
                    preferences[Keys.hapticFeedbackEnabled] ?: true,

                statusConfirmationDurationMs =
                    preferences[
                        Keys.statusConfirmationDurationMs
                    ] ?: 1500,

                darkModeEnabled =
                    preferences[Keys.darkModeEnabled] ?: false,

                celebrationMessagesEnabled =
                    preferences[
                        Keys.celebrationMessagesEnabled
                    ] ?: true,

                manualSuccessCount =
                    preferences[Keys.manualSuccessCount] ?: 0
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
            preferences[
                Keys.statusConfirmationDurationMs
            ] = durationMs.coerceIn(1000, 3000)
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
            preferences[
                Keys.celebrationMessagesEnabled
            ] = enabled
        }
    }

    suspend fun incrementManualSuccessCount(): Int {
        var newCount = 0

        context.appSettingsDataStore.edit { preferences ->
            newCount =
                (preferences[Keys.manualSuccessCount] ?: 0) + 1

            preferences[Keys.manualSuccessCount] = newCount
        }

        return newCount
    }

    suspend fun resetManualSuccessCount() {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.manualSuccessCount] = 0
        }
    }

    suspend fun nextCelebrationMessage(): String {
        var selectedMessage = celebrationMessages.first()

        context.appSettingsDataStore.edit { preferences ->
            val storedMessages =
                preferences[Keys.remainingCelebrationMessages]
                    .orEmpty()

            val remainingMessages =
                storedMessages
                    .split(messageSeparator)
                    .filter { it.isNotBlank() }
                    .filter { it in celebrationMessages }
                    .toMutableList()

            if (remainingMessages.isEmpty()) {
                remainingMessages.addAll(
                    celebrationMessages.shuffled()
                )
            }

            selectedMessage = remainingMessages.removeAt(0)

            preferences[
                Keys.remainingCelebrationMessages
            ] = remainingMessages.joinToString(messageSeparator)
        }

        return selectedMessage
    }

    suspend fun resetCelebrationMessages() {
        context.appSettingsDataStore.edit { preferences ->
            preferences.remove(
                Keys.remainingCelebrationMessages
            )
        }
    }
}
