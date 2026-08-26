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

    @Query("SELECT * FROM routes WHERE number = :number LIMIT 1")
    suspend fun getRoute(number: Int): RouteEntity?

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
        """
    )
    suspend fun updateRouteWithDates(
        number: Int,
        name: String,
        difficulty: String,
        status: String?,
        statusChangedAt: Long?,
        completedDate: Long?
    )

    @Delete
    suspend fun deleteRoute(route: RouteEntity)

    @Query("DELETE FROM routes")
    suspend fun deleteAllRoutes()

    @Query("SELECT COUNT(*) FROM routes")
    suspend fun countRoutes(): Int
}
