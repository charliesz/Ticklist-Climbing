package com.charlie.ticklist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Query("SELECT * FROM routes ORDER BY number ASC")
    fun observeAllRoutes(): Flow<List<RouteEntity>>

    @Query(
        """
        SELECT * FROM routes
        WHERE number = :number
          AND collectionId = :collectionId
        LIMIT 1
        """
    )
    suspend fun getRoute(
        number: Int,
        collectionId: Int = 1
    ): RouteEntity?

    @Query(
        """
        SELECT * FROM routes
        WHERE collectionId = :collectionId
        ORDER BY number ASC
        """
    )
    fun observeRoutesForCollection(
        collectionId: Int
    ): Flow<List<RouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<RouteEntity>)

    @Update
    suspend fun updateRoute(route: RouteEntity)

    @Query(
        """
        UPDATE routes
        SET name = :name,
            difficulty = :difficulty,
            status = :status,
            statusChangedAt = :statusChangedAt,
            completedDate = :completedDate
        WHERE number = :number
          AND collectionId = :collectionId
        """
    )
    suspend fun updateRouteWithDates(
        number: Int,
        name: String,
        difficulty: String,
        status: String?,
        statusChangedAt: Long?,
        completedDate: Long?,
        collectionId: Int
    )

    @Query(
        """
        DELETE FROM routes
        WHERE collectionId = :collectionId
        """
    )
    suspend fun deleteRoutesForCollection(
        collectionId: Int
    )

    @Delete
    suspend fun deleteRoute(route: RouteEntity)

    @Query("DELETE FROM routes")
    suspend fun deleteAllRoutes()

    @Query("SELECT COUNT(*) FROM routes")
    suspend fun countRoutes(): Int

    @Query(
        """
    SELECT * FROM routes
    WHERE collectionId = :collectionId
    ORDER BY number ASC
    """
    )
    suspend fun observeRoutesForCollectionOnce(
        collectionId: Int
    ): List<RouteEntity>

    @Query(
        """
        SELECT COUNT(*) FROM routes
        WHERE collectionId = :collectionId
        """
    )
    suspend fun countRoutesForCollection(
        collectionId: Int
    ): Int
}
