package com.charlie.ticklist.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CollectionExportRepository(
    private val context: Context,
    private val routeDao: RouteDao,
    private val collectionDao: CollectionDao,
    private val routePhotoDao: RoutePhotoDao
) {

    suspend fun exportCollection(
        collectionId: Int,
        destinationUri: Uri,
        onProgress: suspend (
            Int,
            Int,
            String
        ) -> Unit
    ) {
        val collection = collectionDao.getCollection(collectionId)
            ?: error("Sammlung nicht gefunden.")

        val routes = routeDao.getRoutesForCollection(collectionId)

        val routePhotos = mutableMapOf<
                Long,
                List<RoutePhotoEntity>
                >()

        for (route in routes) {
            routePhotos[route.id] =
                routePhotoDao.getPhotosForRoute(route.id)
        }

        val exportFiles = buildExportFiles(
            collection = collection,
            routes = routes,
            routePhotos = routePhotos
        )

        val temporaryZip = File(
            context.cacheDir,
            "ticklist_export_${System.currentTimeMillis()}.zip"
        )

        try {
            createTemporaryZip(
                temporaryZip = temporaryZip,
                collection = collection,
                routes = routes,
                exportFiles = exportFiles,
                onProgress = onProgress
            )

            copyCompletedZip(
                temporaryZip = temporaryZip,
                destinationUri = destinationUri
            )
        } finally {
            if (temporaryZip.exists()) {
                temporaryZip.delete()
            }
        }
    }

    private suspend fun createTemporaryZip(
        temporaryZip: File,
        collection: CollectionEntity,
        routes: List<RouteEntity>,
        exportFiles: List<ExportFile>,
        onProgress: suspend (
            Int,
            Int,
            String
        ) -> Unit
    ) {
        ZipOutputStream(
            BufferedOutputStream(
                FileOutputStream(temporaryZip)
            )
        ).use { zip ->

            writeManifest(zip)
            writeCollection(zip, collection)
            writeRoutes(zip, routes)

            var currentFile = 0

            for (exportFile in exportFiles) {
                copyFileToZip(
                    zip = zip,
                    exportFile = exportFile
                )

                currentFile++

                onProgress(
                    currentFile,
                    exportFiles.size,
                    exportFile.zipPath
                )
            }
        }
    }

    private fun copyCompletedZip(
        temporaryZip: File,
        destinationUri: Uri
    ) {
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
    }

    private fun writeManifest(
        zip: ZipOutputStream
    ) {
        val manifest = JSONObject()
            .put("formatVersion", 1)
            .put("exportType", "COLLECTION")
            .put("containsProgress", false)
            .put("createdAt", System.currentTimeMillis())

        writeTextEntry(
            zip = zip,
            path = "manifest.json",
            content = manifest.toString(2)
        )
    }

    private fun writeCollection(
        zip: ZipOutputStream,
        collection: CollectionEntity
    ) {
        val json = JSONObject()
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

        // Wichtig:
        // Hier werden bewusst keine Bilddateien geschrieben.
        // Cover und Cover-Thumbnail kommen ausschließlich
        // über buildExportFiles().
        writeTextEntry(
            zip = zip,
            path = "collection.json",
            content = json.toString(2)
        )
    }

    private fun writeRoutes(
        zip: ZipOutputStream,
        routes: List<RouteEntity>
    ) {
        val routeArray = JSONArray()

        for (route in routes) {
            routeArray.put(
                JSONObject()
                    .put("id", route.id)
                    .put("number", route.number)
                    .put("name", route.name)
                    .put("difficulty", route.difficulty)
                    .put("notes", route.notes ?: JSONObject.NULL)
            )
        }

        writeTextEntry(
            zip = zip,
            path = "routes.json",
            content = routeArray.toString(2)
        )
    }

    private fun buildExportFiles(
        collection: CollectionEntity,
        routes: List<RouteEntity>,
        routePhotos: Map<Long, List<RoutePhotoEntity>>
    ): List<ExportFile> {
        val result = mutableListOf<ExportFile>()

        addFileIfAvailable(
            result = result,
            sourcePath = collection.coverPhotoPath,
            zipPath = "photos/collection/cover.jpg"
        )

        addFileIfAvailable(
            result = result,
            sourcePath = collection.coverThumbnailPath,
            zipPath = "photos/collection/cover-thumbnail.webp"
        )

        for (route in routes) {
            val photos = routePhotos[route.id].orEmpty()
            val routeDirectory = "photos/route_${route.id}"

            for (photo in photos) {
                addFileIfAvailable(
                    result = result,
                    sourcePath = photo.filePath,
                    zipPath = "$routeDirectory/photo_${photo.id}.jpg"
                )

                addFileIfAvailable(
                    result = result,
                    sourcePath = photo.thumbnailPath,
                    zipPath =
                        "$routeDirectory/photo_" +
                                "${photo.id}-thumbnail.webp"
                )
            }
        }

        return result
    }

    private fun addFileIfAvailable(
        result: MutableList<ExportFile>,
        sourcePath: String?,
        zipPath: String
    ) {
        if (sourcePath.isNullOrBlank()) {
            return
        }

        val sourceFile = File(sourcePath)

        if (sourceFile.exists() && sourceFile.isFile) {
            result += ExportFile(
                sourceFile = sourceFile,
                zipPath = zipPath
            )
        }
    }

    private fun copyFileToZip(
        zip: ZipOutputStream,
        exportFile: ExportFile
    ) {
        zip.putNextEntry(
            ZipEntry(exportFile.zipPath)
        )

        BufferedInputStream(
            FileInputStream(exportFile.sourceFile)
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

    private data class ExportFile(
        val sourceFile: File,
        val zipPath: String
    )
}
