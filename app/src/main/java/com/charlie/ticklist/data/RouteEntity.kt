package com.charlie.ticklist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey
    val number: Int,
    val name: String,
    val difficulty: String,
    val status: String? = null
)
