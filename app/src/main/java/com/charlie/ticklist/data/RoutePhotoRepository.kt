package com.charlie.ticklist.data

import android.content.Context
import android.net.Uri

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

        val photo = RoutePhotoEntity(
            routeId = route.id,
            filePath = filePath,
            isMainPhoto = isMainPhoto,
            createdAt = System.currentTimeMillis()
        )

        val photoId = photoDao.insertPhoto(photo)

        return photo.copy(id = photoId)
    }

    suspend fun setMainPhoto(photo: RoutePhotoEntity) {
        photoDao.clearMainPhoto(photo.routeId)

        photoDao.updatePhoto(
            photo.copy(isMainPhoto = true)
        )
    }

    suspend fun deletePhoto(photo: RoutePhotoEntity) {
        photoDao.deletePhoto(photo)
        PhotoStorage.deletePhoto(photo.filePath)
    }

    suspend fun deleteAllPhotos(route: RouteEntity) {
        photoDao.deletePhotosForRoute(route.id)

        PhotoStorage.deleteAllPhotosForRoute(
            context = context,
            collectionId = route.collectionId,
            routeId = route.id
        )
    }
}
