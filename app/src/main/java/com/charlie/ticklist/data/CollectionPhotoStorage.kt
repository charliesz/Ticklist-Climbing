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

object CollectionPhotoStorage {

    private const val DIRECTORY_NAME = "collection_photos"
    private const val THUMBNAIL_SIZE = 128

    fun saveCoverPhoto(
        context: Context,
        sourceUri: Uri,
        collectionId: Int
    ): String {
        val directory = collectionDirectory(
            context = context,
            collectionId = collectionId
        )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val target = File(
            directory,
            "cover_${UUID.randomUUID()}.jpg"
        )

        context.contentResolver
            .openInputStream(sourceUri)
            .use { input ->
                requireNotNull(input) {
                    "Das Sammlungsfoto konnte nicht geöffnet werden."
                }

                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

        return target.absolutePath
    }

    fun createCoverThumbnail(
        originalPath: String
    ): String? {
        val original = File(originalPath)

        if (!original.exists()) {
            return null
        }

        val parentDirectory = original.parentFile
            ?: return null

        val thumbnail = File(
            parentDirectory,
            "thumb_${original.nameWithoutExtension}.webp"
        )

        if (thumbnail.exists()) {
            return thumbnail.absolutePath
        }

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(
            original.absolutePath,
            bounds
        )

        if (
            bounds.outWidth <= 0 ||
            bounds.outHeight <= 0
        ) {
            return null
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                targetSize = THUMBNAIL_SIZE * 2
            )
        }

        val decodedBitmap =
            BitmapFactory.decodeFile(
                original.absolutePath,
                options
            ) ?: return null

        val squareSize = min(
            decodedBitmap.width,
            decodedBitmap.height
        )

        if (squareSize <= 0) {
            decodedBitmap.recycle()
            return null
        }

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

        FileOutputStream(thumbnail).use { output ->
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

        return thumbnail.absolutePath
    }

    /**
     * Löscht das Originalbild.
     */
    fun deleteCoverPhoto(
        filePath: String?
    ) {
        deleteFile(filePath)
    }

    /**
     * Löscht Originalbild und Thumbnail.
     */
    fun deleteCoverPhoto(
        filePath: String?,
        thumbnailPath: String?
    ) {
        deleteFile(filePath)
        deleteFile(thumbnailPath)
        deleteEmptyParentDirectories(filePath)
    }

    /**
     * Alternative, sprechende Methode zum Löschen
     * von Originalbild und Thumbnail.
     */
    fun deleteCoverPhotoWithThumbnail(
        filePath: String?,
        thumbnailPath: String?
    ) {
        deleteCoverPhoto(
            filePath = filePath,
            thumbnailPath = thumbnailPath
        )
    }

    fun deleteCoverThumbnail(
        thumbnailPath: String?
    ) {
        deleteFile(thumbnailPath)
        deleteEmptyParentDirectories(thumbnailPath)
    }

    fun coverPhotoExists(
        filePath: String?
    ): Boolean {
        return !filePath.isNullOrBlank() &&
                File(filePath).isFile
    }

    fun coverThumbnailExists(
        thumbnailPath: String?
    ): Boolean {
        return !thumbnailPath.isNullOrBlank() &&
                File(thumbnailPath).isFile
    }

    fun deleteAllPhotosForCollection(
        context: Context,
        collectionId: Int
    ) {
        val directory = collectionDirectory(
            context = context,
            collectionId = collectionId
        )

        if (directory.exists()) {
            directory.deleteRecursively()
        }
    }

    /**
     * Löscht alle Sammlungsbilder, die nicht mehr
     * durch die Datenbank referenziert werden.
     *
     * Übergeben werden die aktuell verwendeten Pfade
     * der Coverbilder und Thumbnails.
     */
    fun deleteUnreferencedFiles(
        context: Context,
        referencedPaths: Set<String>
    ) {
        val rootDirectory = File(
            context.filesDir,
            DIRECTORY_NAME
        )

        if (!rootDirectory.exists()) {
            return
        }

        val canonicalReferencedPaths = referencedPaths
            .filter { it.isNotBlank() }
            .mapNotNull { path ->
                runCatching {
                    File(path).canonicalPath
                }.getOrNull()
            }
            .toSet()

        rootDirectory
            .walkTopDown()
            .filter { file ->
                file.isFile
            }
            .forEach { file ->
                val canonicalPath = runCatching {
                    file.canonicalPath
                }.getOrNull()

                if (
                    canonicalPath == null ||
                    canonicalPath !in canonicalReferencedPaths
                ) {
                    file.delete()
                }
            }

        rootDirectory
            .walkBottomUp()
            .filter { directory ->
                directory.isDirectory &&
                        directory != rootDirectory
            }
            .forEach { directory ->
                if (
                    directory.listFiles()
                        .isNullOrEmpty()
                ) {
                    directory.delete()
                }
            }
    }

    private fun collectionDirectory(
        context: Context,
        collectionId: Int
    ): File {
        return File(
            context.filesDir,
            "$DIRECTORY_NAME/collection_$collectionId"
        )
    }

    private fun deleteFile(
        path: String?
    ) {
        if (path.isNullOrBlank()) {
            return
        }

        val file = File(path)

        if (file.exists()) {
            file.delete()
        }
    }

    private fun deleteEmptyParentDirectories(
        path: String?
    ) {
        if (path.isNullOrBlank()) {
            return
        }

        var directory = File(path).parentFile

        while (
            directory != null &&
            directory.name != DIRECTORY_NAME &&
            directory.exists() &&
            directory.isDirectory &&
            directory.listFiles().isNullOrEmpty()
        ) {
            val parent = directory.parentFile
            directory.delete()
            directory = parent
        }
    }

    private fun calculateSampleSize(
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
