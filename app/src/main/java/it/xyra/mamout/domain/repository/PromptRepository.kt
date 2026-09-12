package it.xyra.mamout.domain.repository

import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.model.PromptSearchable
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface providing an abstraction layer for prompt data operations.
 */
interface PromptRepository {

    /**
     * Retrieves all prompt headers as a reactive stream.
     *
     * @return A [Flow] emitting the list of all [Prompt] domain models.
     */
    fun getPrompts(): Flow<List<Prompt>>

    /**
     * Retrieves all prompts combined with their associated template content as a reactive stream.
     *
     * @return A [Flow] emitting a list of [PromptSearchable] domain models for search processing.
     */
    fun getSearchablePrompts(): Flow<List<PromptSearchable>>

    /**
     * Retrieves a single prompt and its template content by its unique identifier.
     *
     * @param promptId The unique identifier of the prompt to retrieve.
     * @return A [Flow] emitting the matching [PromptSearchable] domain model, or `null` if not found.
     */
    fun getPromptById(promptId: Long): Flow<PromptSearchable?>

    /**
     * Updates an existing prompt header and its associated raw template content.
     *
     * @param promptId The unique identifier of the prompt to update.
     * @param title The updated title of the prompt.
     * @param description The updated description of the prompt.
     * @param templateText The updated raw template text.
     */
    suspend fun updatePrompt(
        promptId: Long,
        title: String,
        description: String,
        templateText: String
    )

    /**
     * Deletes a prompt and its associated content by its unique identifier.
     *
     * @param promptId The unique identifier of the prompt to delete.
     */
    suspend fun deletePrompt(promptId: Long)

    /**
     * Saves a new prompt with its header and content.
     *
     * @param title The title of the prompt.
     * @param description The description of the prompt.
     * @param templateText The raw template text.
     */
    suspend fun savePrompt(title: String, description: String, templateText: String)
}
