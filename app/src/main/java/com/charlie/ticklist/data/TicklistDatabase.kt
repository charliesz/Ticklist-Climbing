package com.charlie.ticklist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RouteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TicklistDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {
                db.execSQL(
                    "ALTER TABLE routes ADD COLUMN statusChangedAt INTEGER"
                )

                db.execSQL(
                    "ALTER TABLE routes ADD COLUMN completedDate INTEGER"
                )
            }
        }

        @Volatile
        private var INSTANCE: TicklistDatabase? = null

        fun getDatabase(context: Context): TicklistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TicklistDatabase::class.java,
                    "ticklist_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
