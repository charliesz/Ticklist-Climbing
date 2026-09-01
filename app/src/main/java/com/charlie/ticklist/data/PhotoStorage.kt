package com.charlie.ticklist.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

object PhotoStorage {

    private const val PHOTO_DIRECTORY = "route_photos"

    private const val THUMBNAIL_SIZE = 128

    fun savePhoto(
        context: Context,
        sourceUri: Uri,
        collectionId: Int,
        routeId: Long
    ): String {
        val routeDirectory = getRouteDirectory(
            context = context,
            collectionId = collectionId,
            routeId = routeId
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

    fun createThumbnail(
        originalFilePath: String
    ): String? {
        val originalFile = File(originalFilePath)

        if (!originalFile.exists()) {
            return null
        }

        val thumbnailFile = File(
            originalFile.parentFile,
            "thumb_${originalFile.nameWithoutExtension}.webp"
        )

        if (thumbnailFile.exists()) {
            return thumbnailFile.absolutePath
        }

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(
            originalFile.absolutePath,
            boundsOptions
        )

        if (
            boundsOptions.outWidth <= 0 ||
            boundsOptions.outHeight <= 0
        ) {
            return null
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = boundsOptions.outWidth,
                height = boundsOptions.outHeight,
                targetSize = THUMBNAIL_SIZE * 2
            )
        }

        val decodedBitmap = BitmapFactory.decodeFile(
            originalFile.absolutePath,
            decodeOptions
        ) ?: return null

        val squareSize = min(
            decodedBitmap.width,
            decodedBitmap.height
        )

        val cropLeft = max(
            0,
            (decodedBitmap.width - squareSize) / 2
        )

        val cropTop = max(
            0,
            (decodedBitmap.height - squareSize) / 2
        )

        val squareBitmap = Bitmap.createBitmap(
            decodedBitmap,
            cropLeft,
            cropTop,
            squareSize,
            squareSize
        )

        val thumbnailBitmap = Bitmap.createScaledBitmap(
            squareBitmap,
            THUMBNAIL_SIZE,
            THUMBNAIL_SIZE,
            true
        )

        FileOutputStream(thumbnailFile).use { output ->
            thumbnailBitmap.compress(
                Bitmap.CompressFormat.WEBP_LOSSY,
                85,
                output
            )
        }

        if (thumbnailBitmap !== squareBitmap) {
            thumbnailBitmap.recycle()
        }

        if (squareBitmap !== decodedBitmap) {
            squareBitmap.recycle()
        }

        decodedBitmap.recycle()

        return thumbnailFile.absolutePath
    }

    fun deletePhoto(
        filePath: String
    ) {
        val file = File(filePath)

        if (file.exists()) {
            file.delete()
        }
    }

    fun deleteThumbnail(
        thumbnailPath: String?
    ) {
        if (thumbnailPath != null) {
            deletePhoto(thumbnailPath)
        }
    }

    fun deleteAllPhotosForRoute(
        context: Context,
        collectionId: Int,
        routeId: Long
    ) {
        val routeDirectory = getRouteDirectory(
            context = context,
            collectionId = collectionId,
            routeId = routeId
        )

        if (routeDirectory.exists()) {
            routeDirectory.deleteRecursively()
        }
    }

    fun fileExists(
        filePath: String?
    ): Boolean {
        return filePath != null && File(filePath).exists()
    }

    private fun getRouteDirectory(
        context: Context,
        collectionId: Int,
        routeId: Long
    ): File {
        return File(
            context.filesDir,
            "$PHOTO_DIRECTORY/collection_$collectionId/route_$routeId"
        )
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        targetSize: Int
    ): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height

        while (
            currentWidth / 2 >= targetSize &&
            currentHeight / 2 >= targetSize
        ) {
            currentWidth /= 2
            currentHeight /= 2
            sampleSize *= 2
        }

        return sampleSize
    }
}
