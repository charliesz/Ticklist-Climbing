package com.charlie.ticklist.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.charlie.ticklist.ui.QrCodeThumbnail
import com.charlie.ticklist.ui.QrCodeViewerDialog

private const val GITHUB_URL =
    "https://github.com/charliesz/Ticklist-Climbing"

private const val RELEASES_URL =
    "https://github.com/charliesz/Ticklist-Climbing/releases"

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var showReleasesQr by remember {
        mutableStateOf(false)
    }

    val versionName = runCatching {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            0
        )

        packageInfo.versionName ?: "unbekannt"
    }.getOrDefault("unbekannt")

    fun openUrl(url: String) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }

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
                    text = "Über Ticklist Climbing",
                    modifier = Modifier.padding(
                        start = 8.dp,
                        top = 10.dp
                    ),
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ticklist Climbing",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text =
                            "Offline Climbing and Bouldering Ticklist",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Open Source",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text =
                            "Quellcode, Entwicklung und Beiträge " +
                                    "sind auf GitHub verfügbar.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = {
                            openUrl(GITHUB_URL)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("GitHub öffnen")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        QrCodeThumbnail(
                            onClick = {
                                showReleasesQr = true
                            }
                        )

                        Button(
                            onClick = {
                                openUrl(RELEASES_URL)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Releases öffnen")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Lizenz",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text =
                            "GNU General Public License " +
                                    "version 3 or later",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Credits",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text =
                            "Danke an Matze und Lotti fürs Testen.\n" +
                                    "Danke an Cato für alles!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showReleasesQr) {
        QrCodeViewerDialog(
            onDismiss = {
                showReleasesQr = false
            }
        )
    }
}
