package com.charlie.ticklist.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class CollectionImportRepository(
    private val context: Context,
    private val collectionDao: CollectionDao,
    private val routeDao: RouteDao,
    private val routePhotoDao: RoutePhotoDao
) {

    suspend fun importAsNewCollection(
        sourceUri: Uri,
        onProgress: suspend (
            currentFile: Int,
            totalFiles: Int,
            currentName: String
        ) -> Unit
    ): String {
        val temporaryDirectory = File(
            context.cacheDir,
            "ticklist_import_${UUID.randomUUID()}"
        )

        temporaryDirectory.mkdirs()

        try {
            extractZip(
                sourceUri = sourceUri,
                targetDirectory = temporaryDirectory,
                onProgress = onProgress
            )

            val collectionFile = File(
                temporaryDirectory,
                "collection.json"
            )

            val routesFile = File(
                temporaryDirectory,
                "routes.json"
            )

            require(collectionFile.exists()) {
                "collection.json fehlt im ZIP-Archiv."
            }

            require(routesFile.exists()) {
                "routes.json fehlt im ZIP-Archiv."
            }

            val collectionJson = JSONObject(
                collectionFile.readText()
            )

            val routesJson = JSONArray(
                routesFile.readText()
            )

            val originalName = collectionJson.optString(
                "name",
                "Importierte Sammlung"
            )

            val importedName = createImportName(originalName)

            val newCollectionId = collectionDao
                .insertCollection(
                    CollectionEntity(
                        name = importedName,
                        discipline = collectionJson.optString(
                            "discipline",
                            "BOULDER"
                        ),
                        createdAt = System.currentTimeMillis(),
                        notes = collectionJson
                            .optStringOrNull("notes"),
                        coverPhotoPath = null,
                        coverThumbnailPath = null
                    )
                )
                .toInt()

            val routeNumberToOriginalId =
                mutableMapOf<Int, Long>()

            for (index in 0 until routesJson.length()) {
                val routeJson = routesJson.getJSONObject(index)

                val number = routeJson.optInt(
                    "number",
                    index + 1
                )

                val originalRouteId = routeJson.optLong(
                    "id",
                    0L
                )

                routeNumberToOriginalId[number] =
                    originalRouteId

                routeDao.insertRoute(
                    RouteEntity(
                        number = number,
                        name = routeJson.optString(
                            "name",
                            "%02d".format(number)
                        ),
                        difficulty = routeJson.optString(
                            "difficulty",
                            ""
                        ),
                        status = null,
                        statusChangedAt = null,
                        completedDate = null,
                        collectionId = newCollectionId,
                        notes = routeJson.optStringOrNull(
                            "notes"
                        )
                    )
                )
            }

            importCollectionCover(
                temporaryDirectory = temporaryDirectory,
                collectionId = newCollectionId
            )?.let { coverPaths ->
                collectionDao.updateCollectionDetails(
                    id = newCollectionId,
                    name = importedName,
                    notes = collectionJson.optStringOrNull(
                        "notes"
                    ),
                    coverPhotoPath = coverPaths.originalPath,
                    coverThumbnailPath = coverPaths.thumbnailPath
                )
            }

            importRoutePhotos(
                temporaryDirectory = temporaryDirectory,
                collectionId = newCollectionId,
                routeNumberToOriginalId = routeNumberToOriginalId
            )

            return importedName
        } finally {
            if (temporaryDirectory.exists()) {
                temporaryDirectory.deleteRecursively()
            }
        }
    }

    private suspend fun extractZip(
        sourceUri: Uri,
        targetDirectory: File,
        onProgress: suspend (
            Int,
            Int,
            String
        ) -> Unit
    ) {
        val entries = mutableListOf<String>()

        context.contentResolver
            .openInputStream(sourceUri)
            .use { input ->
                requireNotNull(input) {
                    "Das ZIP-Archiv konnte nicht geöffnet werden."
                }

                ZipInputStream(
                    BufferedInputStream(input)
                ).use { zip ->
                    var entry = zip.nextEntry

                    while (entry != null) {
                        if (!entry.isDirectory) {
                            entries += entry.name
                        }

                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

        var current = 0

        context.contentResolver
            .openInputStream(sourceUri)
            .use { input ->
                requireNotNull(input) {
                    "Das ZIP-Archiv konnte nicht geöffnet werden."
                }

                ZipInputStream(
                    BufferedInputStream(input)
                ).use { zip ->
                    var entry = zip.nextEntry

                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val outputFile = safeTargetFile(
                                root = targetDirectory,
                                entryName = entry.name
                            )

                            outputFile.parentFile?.mkdirs()

                            outputFile.outputStream().use { output ->
                                zip.copyTo(output)
                            }

                            current++

                            onProgress(
                                current,
                                entries.size,
                                entry.name
                            )
                        }

                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
    }

    private suspend fun importCollectionCover(
        temporaryDirectory: File,
        collectionId: Int
    ): ImportedCover? {
        val original = File(
            temporaryDirectory,
            "photos/collection/cover.jpg"
        )

        if (!original.exists()) {
            return null
        }

        val destinationDirectory = File(
            context.filesDir,
            "collection_photos/collection_$collectionId"
        )

        destinationDirectory.mkdirs()

        val originalDestination = File(
            destinationDirectory,
            "cover_imported.jpg"
        )

        original.copyTo(
            target = originalDestination,
            overwrite = true
        )

        val thumbnail = File(
            temporaryDirectory,
            "photos/collection/cover-thumbnail.webp"
        )

        val thumbnailDestination = if (thumbnail.exists()) {
            val target = File(
                destinationDirectory,
                "cover_imported-thumbnail.webp"
            )

            thumbnail.copyTo(
                target = target,
                overwrite = true
            )

            target
        } else {
            null
        }

        return ImportedCover(
            originalPath = originalDestination.absolutePath,
            thumbnailPath = thumbnailDestination
                ?.absolutePath
        )
    }

    private suspend fun importRoutePhotos(
        temporaryDirectory: File,
        collectionId: Int,
        routeNumberToOriginalId: Map<Int, Long>
    ) {
        val photosRoot = File(
            temporaryDirectory,
            "photos"
        )

        if (!photosRoot.exists()) {
            return
        }

        val routeDirectories = photosRoot
            .listFiles()
            .orEmpty()
            .filter {
                it.isDirectory &&
                        it.name.startsWith("route_")
            }

        for (routeDirectory in routeDirectories) {
            val originalRouteId =
                routeDirectory.name
                    .removePrefix("route_")
                    .toLongOrNull()
                    ?: continue

            val number = routeNumberToOriginalId
                .entries
                .firstOrNull {
                    it.value == originalRouteId
                }
                ?.key
                ?: continue

            val route = routeDao.getRoute(
                number = number,
                collectionId = collectionId
            ) ?: continue

            val destinationDirectory = File(
                context.filesDir,
                "route_photos/collection_$collectionId" +
                        "/route_${route.id}"
            )

            destinationDirectory.mkdirs()

            val files = routeDirectory
                .listFiles()
                .orEmpty()
                .filter {
                    it.isFile
                }

            val importedPhotos = mutableListOf<RoutePhotoEntity>()

            for (file in files) {
                val isThumbnail =
                    file.name.endsWith(
                        "-thumbnail.webp",
                        ignoreCase = true
                    )

                if (isThumbnail) {
                    continue
                }

                val target = File(
                    destinationDirectory,
                    "imported_${file.name}"
                )

                file.copyTo(
                    target = target,
                    overwrite = true
                )

                val photoId = routePhotoDao.insertPhoto(
                    RoutePhotoEntity(
                        routeId = route.id,
                        filePath = target.absolutePath,
                        thumbnailPath = findMatchingThumbnail(
                            routeDirectory = routeDirectory,
                            originalFile = file,
                            destinationDirectory =
                                destinationDirectory
                        ),
                        isMainPhoto = importedPhotos.isEmpty(),
                        createdAt = System.currentTimeMillis()
                    )
                )

                importedPhotos += RoutePhotoEntity(
                    id = photoId,
                    routeId = route.id,
                    filePath = target.absolutePath,
                    thumbnailPath = findMatchingThumbnail(
                        routeDirectory = routeDirectory,
                        originalFile = file,
                        destinationDirectory =
                            destinationDirectory
                    ),
                    isMainPhoto = importedPhotos.isEmpty(),
                    createdAt = System.currentTimeMillis()
                )
            }
        }
    }

    private fun findMatchingThumbnail(
        routeDirectory: File,
        originalFile: File,
        destinationDirectory: File
    ): String? {
        val thumbnail = File(
            routeDirectory,
            "${originalFile.nameWithoutExtension}" +
                    "-thumbnail.webp"
        )

        if (!thumbnail.exists()) {
            return null
        }

        val target = File(
            destinationDirectory,
            "imported_${thumbnail.name}"
        )

        thumbnail.copyTo(
            target = target,
            overwrite = true
        )

        return target.absolutePath
    }

    private suspend fun createImportName(
        originalName: String
    ): String {
        val baseName =
            if (originalName.endsWith("_import")) {
                originalName
            } else {
                "${originalName}_import"
            }

        var candidate = baseName
        var counter = 2

        while (collectionNameExists(candidate)) {
            candidate = "${baseName}_$counter"
            counter++
        }

        return candidate
    }

    private suspend fun collectionNameExists(
        name: String
    ): Boolean {
        return collectionDao
            .observeAllCollectionsOnce()
            .any { collection ->
                collection.name == name
            }
    }


    private fun safeTargetFile(
        root: File,
        entryName: String
    ): File {
        val target = File(root, entryName)
        val rootPath = root.canonicalPath
        val targetPath = target.canonicalPath

        require(
            targetPath == rootPath ||
                    targetPath.startsWith("$rootPath${File.separator}")
        ) {
            "Ungültiger ZIP-Pfad."
        }

        return target
    }

    private data class ExportFile(
        val sourceFile: File,
        val zipPath: String
    )

    private data class ImportedCover(
        val originalPath: String,
        val thumbnailPath: String?
    )
}

private fun JSONObject.optStringOrNull(
    key: String
): String? {
    if (!has(key) || isNull(key)) {
        return null
    }

    return optString(key).takeIf {
        it.isNotBlank()
    }
}
