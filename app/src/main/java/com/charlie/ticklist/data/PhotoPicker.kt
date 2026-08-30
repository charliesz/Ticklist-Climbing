package com.charlie.ticklist.data

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun rememberPhotoPicker(
    onPhotoSelected: (Uri) -> Unit
): () -> Unit {
    val currentOnPhotoSelected =
        rememberUpdatedState(onPhotoSelected)

    val pickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                currentOnPhotoSelected.value(uri)
            }
        }

    return {
        pickerLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }
}
