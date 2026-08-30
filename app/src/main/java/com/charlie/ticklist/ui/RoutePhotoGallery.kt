package com.charlie.ticklist.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.charlie.ticklist.data.RoutePhotoEntity
import java.io.File

@Composable
fun RoutePhotoGallery(
    photos: List<RoutePhotoEntity>,
    onAddPhoto: () -> Unit,
    onDeletePhoto: (RoutePhotoEntity) -> Unit,
    onSetMainPhoto: (RoutePhotoEntity) -> Unit,
    onLongPressPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var openedPhoto by remember {
        mutableStateOf<RoutePhotoEntity?>(null)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Fotos",
            style = MaterialTheme.typography.titleMedium
        )

        if (photos.isEmpty()) {
            OutlinedButton(
                onClick = onAddPhoto,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Foto hinzufügen")
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = photos,
                    key = { it.id }
                ) { photo ->
                    PhotoThumbnail(
                        photo = photo,
                        onClick = {
                            openedPhoto = photo
                        },
                        onLongPress = onLongPressPhoto
                    )
                }

                item {
                    OutlinedButton(
                        onClick = onAddPhoto,
                        modifier = Modifier.size(96.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("+")
                    }
                }
            }
        }
    }

    openedPhoto?.let { selectedPhoto ->
        PhotoViewerDialog(
            selectedPhoto = selectedPhoto,
            photos = photos,
            onDismiss = {
                openedPhoto = null
            },
            onSelectPhoto = {
                openedPhoto = it
            },
            onDeletePhoto = {
                onDeletePhoto(it)

                openedPhoto = photos.firstOrNull { photo ->
                    photo.id != selectedPhoto.id
                }
            },
            onSetMainPhoto = onSetMainPhoto
        )
    }
}

@Composable
private fun PhotoThumbnail(
    photo: RoutePhotoEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .pointerInput(photo.id) {
                detectTapGestures(
                    onTap = {
                        onClick()
                    },
                    onLongPress = {
                        onLongPress()
                    }
                )
            }
    ) {
        LocalPhotoImage(
            filePath = photo.filePath,
            contentDescription = "Routenfoto",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (photo.isMainPhoto) {
            Text(
                text = "Hauptfoto",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(3.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun PhotoViewerDialog(
    selectedPhoto: RoutePhotoEntity,
    photos: List<RoutePhotoEntity>,
    onDismiss: () -> Unit,
    onSelectPhoto: (RoutePhotoEntity) -> Unit,
    onDeletePhoto: (RoutePhotoEntity) -> Unit,
    onSetMainPhoto: (RoutePhotoEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Routenfotos")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LocalPhotoImage(
                    filePath = selectedPhoto.filePath,
                    contentDescription = "Routenfoto vergrößert",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentScale = ContentScale.Fit
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    photos.forEach { photo ->
                        Card(
                            modifier = Modifier
                                .size(56.dp)
                                .pointerInput(photo.id) {
                                    detectTapGestures(
                                        onTap = {
                                            onSelectPhoto(photo)
                                        }
                                    )
                                }
                        ) {
                            LocalPhotoImage(
                                filePath = photo.filePath,
                                contentDescription = "Foto auswählen",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onSetMainPhoto(selectedPhoto)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selectedPhoto.isMainPhoto) {
                            "Hauptfoto"
                        } else {
                            "Als Hauptfoto verwenden"
                        }
                    )
                }

                TextButton(
                    onClick = {
                        onDeletePhoto(selectedPhoto)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Foto löschen")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Schließen")
            }
        }
    )
}

@Composable
private fun LocalPhotoImage(
    filePath: String,
    contentDescription: String,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val context = LocalContext.current

    val imageBitmap = remember(filePath) {
        val file = File(filePath)

        if (!file.exists()) {
            null
        } else {
            runCatching {
                android.graphics.BitmapFactory
                    .decodeFile(file.absolutePath)
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(
                MaterialTheme.colorScheme.surfaceVariant
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Foto nicht verfügbar",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
