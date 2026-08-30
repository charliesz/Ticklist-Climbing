package com.charlie.ticklist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutePhotoDao {

    @Query(
        """
        SELECT * FROM route_photos
        WHERE routeId = :routeId
        ORDER BY isMainPhoto DESC, createdAt ASC
        """
    )
    fun observePhotosForRoute(
        routeId: Long
    ): Flow<List<RoutePhotoEntity>>

    @Query(
        """
        SELECT * FROM route_photos
        WHERE routeId = :routeId
          AND isMainPhoto = 1
        LIMIT 1
        """
    )
    suspend fun getMainPhoto(
        routeId: Long
    ): RoutePhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(
        photo: RoutePhotoEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(
        photos: List<RoutePhotoEntity>
    )

    @Update
    suspend fun updatePhoto(
        photo: RoutePhotoEntity
    )

    @Delete
    suspend fun deletePhoto(
        photo: RoutePhotoEntity
    )

    @Query(
        """
        DELETE FROM route_photos
        WHERE routeId = :routeId
        """
    )
    suspend fun deletePhotosForRoute(
        routeId: Long
    )

    @Query(
        """
        UPDATE route_photos
        SET isMainPhoto = 0
        WHERE routeId = :routeId
        """
    )
    suspend fun clearMainPhoto(
        routeId: Long
    )
}
