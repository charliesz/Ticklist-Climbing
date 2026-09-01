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
        CollectionEntity::class,
        RoutePhotoEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class TicklistDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    abstract fun collectionDao(): CollectionDao

    abstract fun routePhotoDao(): RoutePhotoDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {
                db.execSQL(
                    """
                    CREATE TABLE routes_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        number INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        status TEXT,
                        statusChangedAt INTEGER,
                        completedDate INTEGER,
                        collectionId INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO routes_new (
                        number,
                        name,
                        difficulty,
                        status,
                        statusChangedAt,
                        completedDate,
                        collectionId
                    )
                    SELECT
                        number,
                        name,
                        difficulty,
                        status,
                        statusChangedAt,
                        completedDate,
                        collectionId
                    FROM routes
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE routes")

                db.execSQL(
                    "ALTER TABLE routes_new RENAME TO routes"
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_routes_collectionId_number
                    ON routes(collectionId, number)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {
                db.execSQL(
                    """
                    ALTER TABLE routes
                    ADD COLUMN notes TEXT
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS route_photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        routeId INTEGER NOT NULL,
                        filePath TEXT NOT NULL,
                        isMainPhoto INTEGER NOT NULL DEFAULT 0,
                        cropLeft REAL,
                        cropTop REAL,
                        cropRight REAL,
                        cropBottom REAL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_route_photos_routeId
                    ON route_photos(routeId)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {
                db.execSQL(
                    """
                    ALTER TABLE route_photos
                    ADD COLUMN thumbnailPath TEXT
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: TicklistDatabase? = null

        fun getDatabase(
            context: Context
        ): TicklistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TicklistDatabase::class.java,
                    "ticklist_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
