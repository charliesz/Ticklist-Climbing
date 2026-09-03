package com.charlie.ticklist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY createdAt ASC")
    fun observeAllCollections(): Flow<List<CollectionEntity>>

    @Query(
        """
        SELECT * FROM collections
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getCollection(
        id: Int
    ): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(
        collection: CollectionEntity
    ): Long

    @Query(
        """
        UPDATE collections
        SET name = :name
        WHERE id = :id
        """
    )
    suspend fun updateCollectionName(
        id: Int,
        name: String
    )

    @Query(
        """
        UPDATE collections
        SET name = :name,
            notes = :notes,
            coverPhotoPath = :coverPhotoPath,
            coverThumbnailPath = :coverThumbnailPath
        WHERE id = :id
        """
    )
    suspend fun updateCollectionDetails(
        id: Int,
        name: String,
        notes: String?,
        coverPhotoPath: String?,
        coverThumbnailPath: String?
    )

    @Query(
        """
        UPDATE collections
        SET notes = :notes
        WHERE id = :id
        """
    )
    suspend fun updateCollectionNotes(
        id: Int,
        notes: String?
    )

    @Query(
        """
        UPDATE collections
        SET coverPhotoPath = :coverPhotoPath,
            coverThumbnailPath = :coverThumbnailPath
        WHERE id = :id
        """
    )
    suspend fun updateCollectionCoverPhoto(
        id: Int,
        coverPhotoPath: String?,
        coverThumbnailPath: String?
    )

    @Query(
        """
        DELETE FROM collections
        WHERE id = :id
        """
    )
    suspend fun deleteCollectionById(
        id: Int
    ): Int

    @Query("SELECT COUNT(*) FROM collections")
    suspend fun countCollections(): Int

    @Delete
    suspend fun deleteCollection(
        collection: CollectionEntity
    )
}
