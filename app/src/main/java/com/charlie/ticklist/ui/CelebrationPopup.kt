package com.charlie.ticklist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CelebrationPopup(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(
                horizontal = 24.dp,
                vertical = 14.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

fun randomCelebrationMessage(): String {
    return listOf(
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
        "Endlich!",
        "Lassen wir gelten!"
    ).random()
}
