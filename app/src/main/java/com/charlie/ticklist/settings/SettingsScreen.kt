package com.charlie.ticklist.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onHapticFeedbackChanged: (Boolean) -> Unit,
    onDurationChanged: (Int) -> Unit,
    onDarkModeChanged: (Boolean) -> Unit,
    onCelebrationMessagesChanged: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack
                ) {
                    Text("Zurück")
                }

                Text(
                    text = "Einstellungen",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Bedienung",
                        style = MaterialTheme.typography.titleMedium
                    )

                    SettingsCheckboxRow(
                        label = "Haptisches Feedback",
                        checked = settings.hapticFeedbackEnabled,
                        onCheckedChange = onHapticFeedbackChanged
                    )

                    Text(
                        text = "Status-Bestätigungsdauer: ${
                            formatDuration(
                                settings.statusConfirmationDurationMs
                            )
                        }"
                    )

                    Slider(
                        value = settings.statusConfirmationDurationMs
                            .toFloat(),

                        onValueChange = { value ->
                            val rounded =
                                (value / 250f)
                                    .toInt()
                                    .coerceIn(4, 12) * 250

                            onDurationChanged(rounded)
                        },

                        valueRange = 1000f..3000f,
                        steps = 7
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Darstellung",
                        style = MaterialTheme.typography.titleMedium
                    )

                    SettingsCheckboxRow(
                        label = "Dunkles Theme",
                        checked = settings.darkModeEnabled,
                        onCheckedChange = onDarkModeChanged
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Debug-Information",
                        style = MaterialTheme.typography.titleMedium
                    )

                    SettingsCheckboxRow(
                        label = "Debug-Informationen bei Absturz sichern",
                        checked = settings.celebrationMessagesEnabled,
                        onCheckedChange =
                            onCelebrationMessagesChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = checked,
                onClick = {
                    onCheckedChange(!checked)
                },
                role = Role.Checkbox
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun formatDuration(
    durationMs: Int
): String {
    val seconds = durationMs / 1000f
    return if (seconds % 1f == 0f) {
        "${seconds.toInt()},0 Sekunden"
    } else {
        "${seconds.toString().replace('.', ',')} Sekunden"
    }
}
