package it.xyra.mamout.domain.repository

import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.model.PromptSearchable
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface providing an abstraction layer for prompt data operations.
 */
interface PromptRepository {

    /**
     * Retrieves all saved prompts as a reactive stream.
     *
     * @return A [Flow] emitting a list of lightweight [Prompt] domain models.
     */
    fun getPrompts(): Flow<List<Prompt>>

    /**
     * Retrieves all prompts wrapped with their template content for search processing.
     *
     * @return A [Flow] emitting a list of [PromptSearchable] domain models.
     */
    fun getSearchablePrompts(): Flow<List<PromptSearchable>>

    /**
     * Deletes a prompt and its associated content by its ID.
     *
     * @param promptId The unique identifier of the prompt to delete.
     */
    suspend fun deletePrompt(promptId: Long)
}
