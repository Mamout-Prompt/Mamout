package it.xyra.mamout.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main database for the application, providing access to local data storage.
 *
 * This database includes tables defined by [PromptEntity] and [PromptContentEntity].
 */
@Database(
    entities = [PromptEntity::class, PromptContentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to the [PromptDao] for database operations.
     * @return The Data Access Object for prompts.
     */
    abstract fun promptDao(): PromptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of [AppDatabase], creating it if necessary.
         *
         * @param context The application context used to build the database.
         * @return The singleton [AppDatabase] instance.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mamout_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
