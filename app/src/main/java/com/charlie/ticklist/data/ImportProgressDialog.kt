package com.charlie.ticklist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charlie.ticklist.data.ImportState

@Composable
fun ImportProgressDialog(
    state: ImportState.Running
) {
    AlertDialog(
        onDismissRequest = {
            // Der Import soll während des Vorgangs
            // nicht versehentlich geschlossen werden.
        },
        title = {
            Text("Import läuft")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Bitte die App geöffnet lassen."
                )

                LinearProgressIndicator(
                    progress = {
                        state.progress
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                Text(
                    text = if (state.totalFiles > 0) {
                        "${state.currentFile} von " +
                                "${state.totalFiles} Dateien"
                    } else {
                        "Dateien werden vorbereitet ..."
                    }
                )

                Text(
                    text = state.currentName
                )
            }
        },
        confirmButton = {
            // Kein Bestätigungsbutton während des Imports.
        }
    )
}
