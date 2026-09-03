package com.charlie.ticklist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val discipline: String,

    val createdAt: Long,

    val notes: String? = null,

    val coverPhotoPath: String? = null
)
