package it.xyra.mamout.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
     * Inserts a new prompt content entry, replacing any existing content for the same
     * [PromptContentEntity.promptId] (which is this table's primary key).
     * @param content The entity to insert.
     * @return The row ID of the inserted (or replaced) content row.
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
     * Retrieves all prompts joined with their template content for search query processing.
     *
     * @return A [Flow] emitting a list of [PromptSearchableDb] database projections.
     */
    @Query(
        """
        SELECT p.id AS id, p.title AS title, p.description AS description, c.templateText AS templateText
        FROM prompts p
        INNER JOIN prompt_contents c ON p.id = c.promptId
        """
    )
    fun getSearchablePrompts(): Flow<List<PromptSearchableDb>>

    /**
     * Retrieves a single searchable prompt database projection by its unique identifier.
     *
     * Joins the prompt header details with its associated content template.
     *
     * @param promptId The unique identifier of the prompt to retrieve.
     * @return A [Flow] emitting the matching [PromptSearchableDb] entry, or `null` if not found.
     */
    @Query(
        """
    SELECT p.id AS id, p.title AS title, p.description AS description, c.templateText AS templateText
    FROM prompts p
    INNER JOIN prompt_contents c ON p.id = c.promptId
    WHERE p.id = :promptId
    LIMIT 1
    """
    )
    fun getSearchablePromptById(promptId: Long): Flow<PromptSearchableDb?>

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
     * Updates the template text for a specific prompt content record.
     *
     * @param promptId The unique identifier of the prompt content to update.
     * @param templateText The new raw template text to be stored.
     */
    @Query("UPDATE prompt_contents SET templateText = :templateText WHERE promptId = :promptId")
    suspend fun updatePromptContent(promptId: Long, templateText: String)

    /**
     * Inserts a new prompt content entry or replaces the existing one if a conflict occurs.
     *
     * @param promptId The unique identifier of the prompt content.
     * @param templateText The raw template text to insert or replace.
     */
    @Query("INSERT OR REPLACE INTO prompt_contents (promptId, templateText) VALUES (:promptId, :templateText)")
    suspend fun upsertPromptContent(promptId: Long, templateText: String)

    /**
     * Inserts a new prompt header together with its content in a single atomic transaction.
     *
     * Wrapping both inserts in [Transaction] prevents a "ghost" prompt: without it, a crash
     * between the two inserts could leave a [PromptEntity] with no matching
     * [PromptContentEntity], which would show up in [getPrompts] but silently disappear from
     * [getSearchablePrompts] / [getSearchablePromptById] due to their INNER JOIN.
     *
     * @param prompt The prompt header to insert.
     * @param templateText The raw template text for the new prompt's content.
     * @return The row ID of the newly inserted prompt header.
     */
    @Transaction
    suspend fun insertPromptWithContent(prompt: PromptEntity, templateText: String): Long {
        val promptId = insertPrompt(prompt)
        insertContent(PromptContentEntity(promptId = promptId, templateText = templateText))
        return promptId
    }

    /**
     * Deletes a prompt header. Due to cascade setup, this also deletes its content.
     * @param prompt The entity to delete.
     */
    @Delete
    suspend fun deletePrompt(prompt: PromptEntity)

    /**
     * Deletes a prompt header by its ID. Due to foreign key cascade, this also deletes its content.
     * @param promptId The ID of the prompt to delete.
     */
    @Query("DELETE FROM prompts WHERE id = :promptId")
    suspend fun deletePromptById(promptId: Long)
}