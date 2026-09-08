package it.xyra.mamout.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing [PromptEntity] and [PromptContentEntity] operations.
 */
@Dao
interface PromptDao {

    /**
     * Inserts a new prompt header.
     * @param prompt The entity to insert.
     * @return The row ID of the newly inserted prompt.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: PromptEntity): Long

    /**
     * Inserts a new prompt content entry.
     * @param content The entity to insert.
     * @return The row ID of the newly inserted content.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(content: PromptContentEntity): Long

    /**
     * Retrieves all prompts as a stream of lists.
     * @return A [Flow] emitting the list of all [PromptEntity]s.
     */
    @Query("SELECT * FROM prompts")
    fun getPrompts(): Flow<List<PromptEntity>>

    /**
     * Retrieves the content for a specific prompt ID.
     * @param promptId The ID of the prompt to search for.
     * @return A [Flow] emitting the [PromptContentEntity] if found, or null.
     */
    @Query("SELECT * FROM prompt_contents WHERE promptId = :promptId LIMIT 1")
    fun getPromptContent(promptId: Long): Flow<PromptContentEntity?>

    /**
     * Updates an existing prompt header.
     * @param prompt The entity to update.
     */
    @Update
    suspend fun updatePrompt(prompt: PromptEntity)

    /**
     * Updates an existing prompt content entry.
     * @param content The entity to update.
     */
    @Update
    suspend fun updateContent(content: PromptContentEntity)

    /**
     * Deletes a prompt header. Due to cascade setup, this also deletes its content.
     * @param prompt The entity to delete.
     */
    @Delete
    suspend fun deletePrompt(prompt: PromptEntity)
}