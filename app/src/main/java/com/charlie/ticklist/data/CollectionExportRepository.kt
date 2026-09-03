package com.charlie.ticklist.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
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
        destinationUri: Uri
    ) {
        val collection = collectionDao.getCollection(collectionId)
            ?: error("Sammlung nicht gefunden.")

        val routes = routeDao
            .observeRoutesForCollectionOnce(collectionId)

        val collectionPhotos = routes
            .flatMap { route ->
                routePhotoDao.getPhotosForRoute(route.id)
            }

        context.contentResolver.openOutputStream(destinationUri).use {
                outputStream ->
            requireNotNull(outputStream) {
                "Die Zieldatei konnte nicht geöffnet werden."
            }

            ZipOutputStream(
                BufferedOutputStream(outputStream)
            ).use { zip ->
                writeManifest(zip)
                writeCollection(zip, collection)
                writeRoutes(zip, routes)

                writeCollectionCover(
                    zip = zip,
                    collection = collection
                )

                routes.forEach { route ->
                    val photos = collectionPhotos.filter {
                        it.routeId == route.id
                    }

                    photos.forEach { photo ->
                        writePhoto(
                            zip = zip,
                            route = route,
                            photo = photo
                        )
                    }
                }
            }
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

        routes.forEach { route ->
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

    private fun writeCollectionCover(
        zip: ZipOutputStream,
        collection: CollectionEntity
    ) {
        copyFileEntry(
            zip = zip,
            sourcePath = collection.coverPhotoPath,
            targetPath = "photos/collection/cover.jpg"
        )

        copyFileEntry(
            zip = zip,
            sourcePath = collection.coverThumbnailPath,
            targetPath = "photos/collection/cover-thumbnail.webp"
        )
    }

    private fun writePhoto(
        zip: ZipOutputStream,
        route: RouteEntity,
        photo: RoutePhotoEntity
    ) {
        val folder = "photos/route_${route.number}"

        val originalName =
            "${folder}/photo_${photo.id}.jpg"

        val thumbnailName =
            "${folder}/photo_${photo.id}-thumbnail.webp"

        copyFileEntry(
            zip = zip,
            sourcePath = photo.filePath,
            targetPath = originalName
        )

        copyFileEntry(
            zip = zip,
            sourcePath = photo.thumbnailPath,
            targetPath = thumbnailName
        )
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

    private fun copyFileEntry(
        zip: ZipOutputStream,
        sourcePath: String?,
        targetPath: String
    ) {
        if (sourcePath.isNullOrBlank()) {
            return
        }

        val sourceFile = File(sourcePath)

        if (!sourceFile.exists()) {
            return
        }

        zip.putNextEntry(ZipEntry(targetPath))
        sourceFile.inputStream().use { input ->
            input.copyTo(zip)
        }
        zip.closeEntry()
    }
}
