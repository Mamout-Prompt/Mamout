package it.xyra.mamout.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The main database for the application, providing access to local data storage.
 *
 * This database includes tables defined by [PromptEntity] and [PromptContentEntity].
 */
@Database(
    entities = [PromptEntity::class, PromptContentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Provides access to the [PromptDao] for database operations.
     * @return The Data Access Object for prompts.
     */
    abstract fun promptDao(): PromptDao
}