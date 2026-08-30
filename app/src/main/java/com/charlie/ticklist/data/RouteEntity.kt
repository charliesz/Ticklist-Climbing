package com.charlie.ticklist.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes",
    indices = [
        Index(
            value = ["collectionId", "number"],
            unique = true
        )
    ]
)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val number: Int,

    val name: String,

    val difficulty: String,

    val status: String? = null,

    @ColumnInfo(name = "statusChangedAt")
    val statusChangedAt: Long? = null,

    @ColumnInfo(name = "completedDate")
    val completedDate: Long? = null,

    @ColumnInfo(name = "collectionId")
    val collectionId: Int = 1,

    val notes: String? = null
)
