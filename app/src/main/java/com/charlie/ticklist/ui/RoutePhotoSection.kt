package com.charlie.ticklist.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.charlie.ticklist.data.RouteEntity
import com.charlie.ticklist.data.RoutePhotoDao
import com.charlie.ticklist.data.RoutePhotoEntity
import com.charlie.ticklist.data.RoutePhotoRepository
import com.charlie.ticklist.data.rememberPhotoPicker
import kotlinx.coroutines.launch

@Composable
fun RoutePhotoSection(
    route: RouteEntity,
    photoDao: RoutePhotoDao,
    photoRepository: RoutePhotoRepository,
    onLongPressPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val photos by photoDao
        .observePhotosForRoute(route.id)
        .collectAsState(initial = emptyList())

    val openPhotoPicker = rememberPhotoPicker { uri: Uri ->
        scope.launch {
            val isFirstPhoto = photos.isEmpty()

            photoRepository.addPhoto(
                sourceUri = uri,
                route = route,
                isMainPhoto = isFirstPhoto
            )
        }
    }

    RoutePhotoGallery(
        photos = photos,
        onAddPhoto = openPhotoPicker,
        onDeletePhoto = { photo ->
            scope.launch {
                photoRepository.deletePhoto(photo)
            }
        },
        onSetMainPhoto = { photo ->
            scope.launch {
                photoRepository.setMainPhoto(photo)
            }
        },
        onLongPressPhoto = onLongPressPhoto,
        modifier = modifier
    )
}
