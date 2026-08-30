package com.charlie.ticklist.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object PhotoStorage {

    private const val PHOTO_DIRECTORY = "route_photos"

    fun savePhoto(
        context: Context,
        sourceUri: Uri,
        collectionId: Int,
        routeId: Long
    ): String {
        val routeDirectory = File(
            context.filesDir,
            "$PHOTO_DIRECTORY/collection_$collectionId/route_$routeId"
        )

        if (!routeDirectory.exists()) {
            routeDirectory.mkdirs()
        }

        val fileName = "${UUID.randomUUID()}.jpg"
        val destinationFile = File(routeDirectory, fileName)

        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) {
                "Das ausgewählte Bild konnte nicht geöffnet werden."
            }

            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return destinationFile.absolutePath
    }

    fun deletePhoto(filePath: String) {
        val file = File(filePath)

        if (file.exists()) {
            file.delete()
        }
    }

    fun deleteAllPhotosForRoute(
        context: Context,
        collectionId: Int,
        routeId: Long
    ) {
        val routeDirectory = File(
            context.filesDir,
            "$PHOTO_DIRECTORY/collection_$collectionId/route_$routeId"
        )

        if (routeDirectory.exists()) {
            routeDirectory.deleteRecursively()
        }
    }

    fun fileExists(filePath: String?): Boolean {
        return filePath != null && File(filePath).exists()
    }
}
