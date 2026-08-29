package com.charlie.ticklist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RouteEntity::class,
        CollectionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TicklistDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    abstract fun collectionDao(): CollectionDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS collections (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        discipline TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                val createdAt = System.currentTimeMillis()

                db.execSQL(
                    """
                    INSERT INTO collections (
                        id,
                        name,
                        discipline,
                        createdAt
                    )
                    VALUES (
                        1,
                        'Boulder 01–90',
                        'BOULDER',
                        $createdAt
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    ALTER TABLE routes
                    ADD COLUMN collectionId INTEGER NOT NULL DEFAULT 1
                    """.trimIndent()
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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
