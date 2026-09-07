package com.charlie.ticklist.data

import android.content.Context
import android.net.Uri
import java.io.File


class RoutePhotoRepository(
    private val context: Context,
    private val photoDao: RoutePhotoDao
) {

    suspend fun addPhoto(
        sourceUri: Uri,
        route: RouteEntity,
        isMainPhoto: Boolean = false
    ): RoutePhotoEntity {
        if (isMainPhoto) {
            photoDao.clearMainPhoto(route.id)
        }

        val filePath = PhotoStorage.savePhoto(
            context = context,
            sourceUri = sourceUri,
            collectionId = route.collectionId,
            routeId = route.id
        )

        val thumbnailPath = PhotoStorage.createThumbnail(
            originalFilePath = filePath
        )

        val photo = RoutePhotoEntity(
            routeId = route.id,
            filePath = filePath,
            thumbnailPath = thumbnailPath,
            isMainPhoto = isMainPhoto,
            createdAt = System.currentTimeMillis()
        )

        val photoId = photoDao.insertPhoto(photo)

        return photo.copy(id = photoId)
    }

    suspend fun ensureThumbnail(
        photo: RoutePhotoEntity
    ): RoutePhotoEntity {
        val existingThumbnail = photo.thumbnailPath

        if (PhotoStorage.fileExists(existingThumbnail)) {
            return photo
        }

        val thumbnailPath = PhotoStorage.createThumbnail(
            originalFilePath = photo.filePath
        )

        val updatedPhoto = photo.copy(
            thumbnailPath = thumbnailPath
        )

        photoDao.updatePhoto(updatedPhoto)

        return updatedPhoto
    }

    suspend fun setMainPhoto(
        photo: RoutePhotoEntity
    ) {
        photoDao.clearMainPhoto(photo.routeId)

        photoDao.updatePhoto(
            photo.copy(isMainPhoto = true)
        )
    }

    suspend fun deletePhoto(
        photo: RoutePhotoEntity
    ) {
        photoDao.deletePhoto(photo)

        PhotoStorage.deletePhoto(photo.filePath)

        PhotoStorage.deleteThumbnail(
            photo.thumbnailPath
        )
    }
    suspend fun deleteOrphanedPhotoFiles() {
        val referencedPhotos = photoDao.getAllPhotos()

        val referencedPaths = referencedPhotos
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
            if (routePhotosDirectory.exists()) {
                routePhotosDirectory.deleteRecursively()
            }
        }

        routePhotosDirectory
            .walkTopDown()
            .filter { file ->
                file.isFile
            }
            .forEach { file ->
                if (file.canonicalPath !in referencedPaths) {
                    file.delete()
                }
            }
    }

    suspend fun deleteAllPhotos(
        route: RouteEntity
    ) {
        photoDao.deletePhotosForRoute(route.id)

        PhotoStorage.deleteAllPhotosForRoute(
            context = context,
            collectionId = route.collectionId,
            routeId = route.id
        )
    }
}
