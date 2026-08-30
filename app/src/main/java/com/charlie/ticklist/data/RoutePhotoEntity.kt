package com.charlie.ticklist.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_photos",
    indices = [
        Index(value = ["routeId"])
    ]
)
data class RoutePhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val routeId: Long,

    val filePath: String,

    val isMainPhoto: Boolean = false,

    val cropLeft: Float? = null,

    val cropTop: Float? = null,

    val cropRight: Float? = null,

    val cropBottom: Float? = null,

    val createdAt: Long
)
