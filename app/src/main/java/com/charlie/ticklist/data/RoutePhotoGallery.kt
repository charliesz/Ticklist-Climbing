package com.charlie.ticklist.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.charlie.ticklist.data.RoutePhotoEntity
import java.io.File

@Composable
fun RoutePhotoGallery(
    photos: List<RoutePhotoEntity>,
    onAddPhoto: () -> Unit,
    onDeletePhoto: (RoutePhotoEntity) -> Unit,
    onSetMainPhoto: (RoutePhotoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPhoto by remember {
        mutableStateOf<RoutePhotoEntity?>(null)
    }

    var showGallery by remember {
        mutableStateOf(false)
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
                            selectedPhoto = photo
                            showGallery = true
                        },
                        onLongClick = {
                            selectedPhoto = photo
                            showGallery = true
                        }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = onAddPhoto,
                        modifier = Modifier
                            .size(96.dp)
                            .height(96.dp)
                    ) {
                        Text("+")
                    }
                }
            }
        }
    }

    if (showGallery && selectedPhoto != null) {
        PhotoViewerDialog(
            photo = selectedPhoto!!,
            photos = photos,
            onDismiss = {
                showGallery = false
                selectedPhoto = null
            },
            onSelectPhoto = {
                selectedPhoto = it
            },
            onDeletePhoto = {
                onDeletePhoto(it)

                if (photos.size <= 1) {
                    showGallery = false
                    selectedPhoto = null
                } else {
                    selectedPhoto = photos.firstOrNull {
                        it.id != photoIdOrNull(selectedPhoto)
                    }
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
    onLongClick: () -> Unit
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
                        onLongClick()
                    }
                )
            }
    ) {
        AsyncImage(
            model = File(photo.filePath),
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
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(3.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun PhotoViewerDialog(
    photo: RoutePhotoEntity,
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
                AsyncImage(
                    model = File(photo.filePath),
                    contentDescription = "Routenfoto vergrößert",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentScale = ContentScale.Fit
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = photos,
                        key = { it.id }
                    ) { galleryPhoto ->
                        PhotoSelectorThumbnail(
                            photo = galleryPhoto,
                            selected = galleryPhoto.id == photo.id,
                            onClick = {
                                onSelectPhoto(galleryPhoto)
                            }
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        onSetMainPhoto(photo)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (photo.isMainPhoto) {
                            "Hauptfoto"
                        } else {
                            "Als Hauptfoto verwenden"
                        }
                    )
                }

                TextButton(
                    onClick = {
                        onDeletePhoto(photo)
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
private fun PhotoSelectorThumbnail(
    photo: RoutePhotoEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(56.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(photo.filePath),
            contentDescription = "Foto auswählen",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

private fun photoIdOrNull(
    photo: RoutePhotoEntity?
): Long? {
    return photo?.id
}
