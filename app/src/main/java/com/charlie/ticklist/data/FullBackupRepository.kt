package com.charlie.ticklist.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.charlie.ticklist.settings.AppSettingsRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream



class FullBackupRepository(
    private val context: Context,
    private val database: TicklistDatabase,
    private val settingsRepository: AppSettingsRepository
) {

    suspend fun exportFullBackup(
        destinationUri: Uri,
        onProgress: suspend (
            currentFile: Int,
            totalFiles: Int,
            currentName: String
        ) -> Unit
    ) {
        database.withTransaction {
            database.routeDao().deleteOrphanedRoutes()
            database.routePhotoDao().deleteOrphanedPhotos()
        }

        val collections = database
            .collectionDao()
            .observeAllCollectionsOnce()

        val routes = collections.flatMap { collection ->
            database.routeDao().getRoutesForCollection(collection.id)
        }

        val exportedRouteIds = routes
            .map { it.id }
            .toSet()

        val photos = database
            .routePhotoDao()
            .getAllPhotos()
            .filter { photo ->
                photo.routeId in exportedRouteIds &&
                        File(photo.filePath).exists()
            }

        deleteUnreferencedRouteFiles(
            photos = photos
        )


        val settings = settingsRepository
            .readBackupSettings()

        val files = mutableListOf<BackupFile>()

        collections.forEach { collection ->
            addBackupFile(
                files = files,
                sourcePath = collection.coverPhotoPath,
                zipPath = "photos/collections/${collection.id}/cover.jpg"
            )

            addBackupFile(
                files = files,
                sourcePath = collection.coverThumbnailPath,
                zipPath =
                    "photos/collections/${collection.id}/cover-thumbnail.webp"
            )
        }

        photos.forEach { photo ->
            addBackupFile(
                files = files,
                sourcePath = photo.filePath,
                zipPath =
                    "photos/routes/${photo.routeId}/" +
                            "photo_${photo.id}.jpg"
            )

            addBackupFile(
                files = files,
                sourcePath = photo.thumbnailPath,
                zipPath =
                    "photos/routes/${photo.routeId}/" +
                            "photo_${photo.id}-thumbnail.webp"
            )
        }

        val temporaryZip = File(
            context.cacheDir,
            "ticklist_full_backup_${UUID.randomUUID()}.zip"
        )

        try {
            ZipOutputStream(
                BufferedOutputStream(
                    FileOutputStream(temporaryZip)
                )
            ).use { zip ->

                writeManifest(zip)

                writeCollections(
                    zip = zip,
                    collections = collections
                )

                writeRoutes(
                    zip = zip,
                    routes = routes
                )

                writePhotos(
                    zip = zip,
                    photos = photos
                )

                writeSettings(
                    zip = zip,
                    settings = settings
                )

                var current = 0

                files.forEach { backupFile ->
                    copyFileToZip(
                        zip = zip,
                        backupFile = backupFile
                    )

                    current++

                    onProgress(
                        current,
                        files.size,
                        backupFile.zipPath
                    )

                }
            }

            context.contentResolver
                .openOutputStream(destinationUri)
                .use { output ->
                    requireNotNull(output) {
                        "Die Zieldatei konnte nicht geöffnet werden."
                    }

                    temporaryZip.inputStream().use { input ->
                        input.copyTo(output)
                    }

                    output.flush()
                }
        } finally {
            temporaryZip.delete()
        }
    }

    suspend fun restoreFullBackup(
        sourceUri: Uri,
        onProgress: suspend (
            currentFile: Int,
            totalFiles: Int,
            currentName: String
        ) -> Unit
    ) {
        val temporaryDirectory = File(
            context.cacheDir,
            "ticklist_full_restore_${UUID.randomUUID()}"
        )

        temporaryDirectory.mkdirs()

        try {
            extractZip(
                sourceUri = sourceUri,
                targetDirectory = temporaryDirectory,
                onProgress = onProgress
            )

            val manifestFile = File(
                temporaryDirectory,
                "manifest.json"
            )

            require(manifestFile.exists()) {
                "manifest.json fehlt im Backup."
            }

            val manifest = JSONObject(
                manifestFile.readText()
            )

            require(
                manifest.optString("exportType") == "FULL_BACKUP"
            ) {
                "Das ZIP-Archiv ist kein vollständiges Ticklist-Backup."
            }

            require(
                manifest.optInt("formatVersion") == 1
            ) {
                "Die Backup-Version wird nicht unterstützt."
            }

            val collectionsJson = JSONArray(
                File(
                    temporaryDirectory,
                    "collections.json"
                ).readText()
            )

            val routesJson = JSONArray(
                File(
                    temporaryDirectory,
                    "routes.json"
                ).readText()
            )

            val photosJson = JSONArray(
                File(
                    temporaryDirectory,
                    "route_photos.json"
                ).readText()
            )

            val settingsJson = JSONObject(
                File(
                    temporaryDirectory,
                    "app_settings.json"
                ).readText()
            )

            val restoredCollections =
                parseCollections(
                    array = collectionsJson,
                    temporaryDirectory = temporaryDirectory
                )


            val restoredRoutes =
                parseRoutes(routesJson)

            File(
                context.filesDir,
                "route_photos"
            ).deleteRecursively()

            File(
                context.filesDir,
                "collection_photos"
            ).deleteRecursively()

            val restoredPhotos =
                restorePhotoFiles(
                    temporaryDirectory = temporaryDirectory,
                    photosJson = photosJson
                )

            database.withTransaction {
                database.routePhotoDao().deleteAllPhotos()
                database.routeDao().deleteAllRoutes()
                database.collectionDao().deleteAllCollections()

                database.collectionDao().insertCollections(
                    restoredCollections
                )

                database.routeDao().insertRoutes(
                    restoredRoutes
                )

                database.routePhotoDao().insertPhotos(
                    restoredPhotos
                )
            }

            settingsRepository.restoreBackupSettings(
                settingsJson
            )
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun writeManifest(
        zip: ZipOutputStream
    ) {
        val manifest = JSONObject()
            .put("formatVersion", 1)
            .put("exportType", "FULL_BACKUP")
            .put("containsProgress", true)
            .put("containsPhotos", true)
            .put("containsSettings", true)
            .put("createdAt", System.currentTimeMillis())

        writeTextEntry(
            zip = zip,
            path = "manifest.json",
            content = manifest.toString(2)
        )
    }
    private fun deleteUnreferencedRouteFiles(
        photos: List<RoutePhotoEntity>
    ) {
        val referencedFiles = photos
            .flatMap { photo ->
                listOfNotNull(
                    photo.filePath,
                    photo.thumbnailPath
                )
            }
            .map { path ->
                File(path).canonicalPath
            }
            .toSet()

        val routePhotosDirectory = File(
            context.filesDir,
            "route_photos"
        )

        if (!routePhotosDirectory.exists()) {
            return
        }

        routePhotosDirectory
            .walkTopDown()
            .filter { file ->
                file.isFile
            }
            .forEach { file ->
                if (file.canonicalPath !in referencedFiles) {
                    file.delete()
                }
            }

        routePhotosDirectory
            .walkBottomUp()
            .filter { directory ->
                directory.isDirectory &&
                        directory != routePhotosDirectory
            }
            .forEach { directory ->
                if (directory.listFiles().isNullOrEmpty()) {
                    directory.delete()
                }
            }
    }

    private fun writeCollections(
        zip: ZipOutputStream,
        collections: List<CollectionEntity>
    ) {
        val array = JSONArray()

        collections.forEach { collection ->
            array.put(
                JSONObject()
                    .put("id", collection.id)
                    .put("name", collection.name)
                    .put("discipline", collection.discipline)
                    .put("createdAt", collection.createdAt)
                    .put("notes", collection.notes ?: JSONObject.NULL)
                    .put(
                        "coverPhotoPath",
                        collection.coverPhotoPath ?: JSONObject.NULL
                    )
                    .put(
                        "coverThumbnailPath",
                        collection.coverThumbnailPath
                            ?: JSONObject.NULL
                    )
            )
        }

        writeTextEntry(
            zip = zip,
            path = "collections.json",
            content = array.toString(2)
        )
    }

    private fun writeRoutes(
        zip: ZipOutputStream,
        routes: List<RouteEntity>
    ) {
        val array = JSONArray()

        routes.forEach { route ->
            array.put(
                JSONObject()
                    .put("id", route.id)
                    .put("number", route.number)
                    .put("name", route.name)
                    .put("difficulty", route.difficulty)
                    .put("status", route.status ?: JSONObject.NULL)
                    .put(
                        "statusChangedAt",
                        route.statusChangedAt ?: JSONObject.NULL
                    )
                    .put(
                        "completedDate",
                        route.completedDate ?: JSONObject.NULL
                    )
                    .put("collectionId", route.collectionId)
                    .put("notes", route.notes ?: JSONObject.NULL)
            )
        }

        writeTextEntry(
            zip = zip,
            path = "routes.json",
            content = array.toString(2)
        )
    }

    private fun writePhotos(
        zip: ZipOutputStream,
        photos: List<RoutePhotoEntity>
    ) {
        val array = JSONArray()

        photos.forEach { photo ->
            array.put(
                JSONObject()
                    .put("id", photo.id)
                    .put("routeId", photo.routeId)
                    .put("filePath", photo.filePath)
                    .put(
                        "thumbnailPath",
                        photo.thumbnailPath ?: JSONObject.NULL
                    )
                    .put("isMainPhoto", photo.isMainPhoto)
                    .put(
                        "cropLeft",
                        photo.cropLeft ?: JSONObject.NULL
                    )
                    .put(
                        "cropTop",
                        photo.cropTop ?: JSONObject.NULL
                    )
                    .put(
                        "cropRight",
                        photo.cropRight ?: JSONObject.NULL
                    )
                    .put(
                        "cropBottom",
                        photo.cropBottom ?: JSONObject.NULL
                    )
                    .put("createdAt", photo.createdAt)
            )
        }

        writeTextEntry(
            zip = zip,
            path = "route_photos.json",
            content = array.toString(2)
        )
    }

    private fun writeSettings(
        zip: ZipOutputStream,
        settings: JSONObject
    ) {
        writeTextEntry(
            zip = zip,
            path = "app_settings.json",
            content = settings.toString(2)
        )
    }

    private fun parseCollections(
        array: JSONArray,
        temporaryDirectory: File
    ): List<CollectionEntity> {
        return buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)

                val collectionId = json.getInt("id")

                add(
                    CollectionEntity(
                        id = collectionId,
                        name = json.getString("name"),
                        discipline = json.getString("discipline"),
                        createdAt = json.getLong("createdAt"),
                        notes = json.optStringOrNull("notes"),
                        coverPhotoPath = restoreCollectionPath(
                            temporaryDirectory = temporaryDirectory,
                            collectionId = collectionId,
                            thumbnail = false
                        ),
                        coverThumbnailPath = restoreCollectionPath(
                            temporaryDirectory = temporaryDirectory,
                            collectionId = collectionId,
                            thumbnail = true
                        )
                    )
                )
            }
        }
    }


    private fun parseRoutes(
        array: JSONArray
    ): List<RouteEntity> {
        return buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)

                add(
                    RouteEntity(
                        id = json.getLong("id"),
                        number = json.getInt("number"),
                        name = json.getString("name"),
                        difficulty = json.getString("difficulty"),
                        status = json.optStringOrNull("status"),
                        statusChangedAt =
                            json.optLongOrNull("statusChangedAt"),
                        completedDate =
                            json.optLongOrNull("completedDate"),
                        collectionId = json.getInt("collectionId"),
                        notes = json.optStringOrNull("notes")
                    )
                )
            }
        }
    }

    private fun restorePhotoFiles(
        temporaryDirectory: File,
        photosJson: JSONArray
    ): List<RoutePhotoEntity> {
        return buildList {
            for (index in 0 until photosJson.length()) {
                val json = photosJson.getJSONObject(index)

                val photoId = json.getLong("id")
                val routeId = json.getLong("routeId")

                val sourcePhoto = File(
                    temporaryDirectory,
                    "photos/routes/$routeId/photo_$photoId.jpg"
                )

                val destinationDirectory = File(
                    context.filesDir,
                    "route_photos/restored_route_$routeId"
                )

                destinationDirectory.mkdirs()

                val destinationPhoto = File(
                    destinationDirectory,
                    "photo_$photoId.jpg"
                )

                if (sourcePhoto.exists()) {
                    sourcePhoto.copyTo(
                        target = destinationPhoto,
                        overwrite = true
                    )
                }

                val sourceThumbnail = File(
                    temporaryDirectory,
                    "photos/routes/$routeId/" +
                            "photo_$photoId-thumbnail.webp"
                )

                val destinationThumbnail = File(
                    destinationDirectory,
                    "photo_$photoId-thumbnail.webp"
                )

                val thumbnailPath =
                    if (sourceThumbnail.exists()) {
                        sourceThumbnail.copyTo(
                            target = destinationThumbnail,
                            overwrite = true
                        )

                        destinationThumbnail.absolutePath
                    } else {
                        null
                    }

                add(
                    RoutePhotoEntity(
                        id = photoId,
                        routeId = routeId,
                        filePath = destinationPhoto.absolutePath,
                        thumbnailPath = thumbnailPath,
                        isMainPhoto = json.optBoolean(
                            "isMainPhoto",
                            false
                        ),
                        cropLeft = json.optFloatOrNull("cropLeft"),
                        cropTop = json.optFloatOrNull("cropTop"),
                        cropRight = json.optFloatOrNull("cropRight"),
                        cropBottom = json.optFloatOrNull("cropBottom"),
                        createdAt = json.getLong("createdAt")
                    )
                )
            }
        }
    }

    private fun restoreCollectionPath(
        temporaryDirectory: File,
        collectionId: Int,
        thumbnail: Boolean
    ): String? {
        val fileName = if (thumbnail) {
            "cover-thumbnail.webp"
        } else {
            "cover.jpg"
        }

        val source = File(
            temporaryDirectory,
            "photos/collections/$collectionId/$fileName"
        )

        if (!source.exists()) {
            return null
        }

        val destinationDirectory = File(
            context.filesDir,
            "collection_photos/restored_collection_$collectionId"
        )

        destinationDirectory.mkdirs()

        val destination = File(
            destinationDirectory,
            fileName
        )

        source.copyTo(
            target = destination,
            overwrite = true
        )

        return destination.absolutePath
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
                requireNotNull(input)

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
                requireNotNull(input)

                ZipInputStream(
                    BufferedInputStream(input)
                ).use { zip ->
                    var entry = zip.nextEntry

                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val outputFile = safeTargetFile(
                                targetDirectory,
                                entry.name
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

    private fun addBackupFile(
        files: MutableList<BackupFile>,
        sourcePath: String?,
        zipPath: String
    ) {
        if (sourcePath.isNullOrBlank()) {
            return
        }

        val sourceFile = File(sourcePath)

        if (sourceFile.exists() && sourceFile.isFile) {
            files += BackupFile(
                sourceFile = sourceFile,
                zipPath = zipPath
            )
        }
    }

    private fun copyFileToZip(
        zip: ZipOutputStream,
        backupFile: BackupFile
    ) {
        zip.putNextEntry(
            ZipEntry(backupFile.zipPath)
        )

        BufferedInputStream(
            FileInputStream(backupFile.sourceFile)
        ).use { input ->
            input.copyTo(zip)
        }

        zip.closeEntry()
    }

    private fun writeTextEntry(
        zip: ZipOutputStream,
        path: String,
        content: String
    ) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
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
                    targetPath.startsWith(
                        "$rootPath${File.separator}"
                    )
        ) {
            "Ungültiger ZIP-Pfad."
        }

        return target
    }

    private data class BackupFile(
        val sourceFile: File,
        val zipPath: String
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

private fun JSONObject.optLongOrNull(
    key: String
): Long? {
    if (!has(key) || isNull(key)) {
        return null
    }

    return optLong(key)
}

private fun JSONObject.optFloatOrNull(
    key: String
): Float? {
    if (!has(key) || isNull(key)) {
        return null
    }

    return optDouble(key).toFloat()
}
