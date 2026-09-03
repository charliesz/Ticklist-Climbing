package com.charlie.ticklist.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun CollectionCoverThumbnail(
    thumbnailPath: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val imageBitmap = remember(thumbnailPath) {
        if (thumbnailPath.isNullOrBlank()) {
            null
        } else {
            val file = File(thumbnailPath)

            if (file.exists()) {
                runCatching {
                    android.graphics.BitmapFactory
                        .decodeFile(file.absolutePath)
                        ?.asImageBitmap()
                }.getOrNull()
            } else {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(thumbnailPath) {
                detectTapGestures(
                    onTap = {
                        if (imageBitmap != null) {
                            onClick()
                        }
                    },
                    onLongPress = {
                        onLongClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Sammlungsfoto",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
