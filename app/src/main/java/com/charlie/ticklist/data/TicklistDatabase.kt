package com.charlie.ticklist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RouteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TicklistDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: TicklistDatabase? = null

        fun getDatabase(context: Context): TicklistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TicklistDatabase::class.java,
                    "ticklist_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
