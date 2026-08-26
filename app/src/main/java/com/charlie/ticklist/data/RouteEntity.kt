package com.charlie.ticklist.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey
    val number: Int,

    val name: String,

    val difficulty: String,

    val status: String? = null,

    @ColumnInfo(name = "statusChangedAt")
    val statusChangedAt: Long? = null,

    @ColumnInfo(name = "completedDate")
    val completedDate: Long? = null
)
