package it.xyra.mamout.data.repository

import it.xyra.mamout.data.local.PromptContentEntity
import it.xyra.mamout.data.local.PromptDao
import it.xyra.mamout.data.local.PromptEntity
import it.xyra.mamout.data.local.PromptSearchableDb
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.model.PromptSearchable
import it.xyra.mamout.domain.repository.PromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of [PromptRepository] interfacing with the local Room database.
 *
 * @property promptDao The Data Access Object for local database operations.
 */
class PromptRepositoryImpl(
    private val promptDao: PromptDao
) : PromptRepository {

    /**
     * Retrieves all saved prompts from the local database and maps them to domain models.
     *
     * @return A [Flow] emitting a list of [Prompt] domain models.
     */
    override fun getPrompts(): Flow<List<Prompt>> {
        return promptDao.getPrompts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Retrieves searchable prompt projections from the local database and maps them to domain models.
     *
     * @return A [Flow] emitting a list of [PromptSearchable] domain models.
     */
    override fun getSearchablePrompts(): Flow<List<PromptSearchable>> {
        return promptDao.getSearchablePrompts().map { dbProjections ->
            dbProjections.map { it.toDomainModel() }
        }
    }

    /**
     * Saves a new prompt with its header and content.
     */
    override suspend fun savePrompt(title: String, description: String, templateText: String) {
        val promptId = promptDao.insertPrompt(
            PromptEntity(title = title, description = description)
        )
        promptDao.insertContent(
            PromptContentEntity(promptId = promptId, templateText = templateText)
        )
    }

    /**
     * Extension function to map a [PromptEntity] database model to a [Prompt] domain model.
     */
    private fun PromptEntity.toDomainModel(): Prompt {
        return Prompt(
            id = id,
            title = title,
            description = description
        )
    }

    /**
     * Extension function to map a [PromptSearchableDb] database projection to a [PromptSearchable] domain model.
     */
    private fun PromptSearchableDb.toDomainModel(): PromptSearchable {
        return PromptSearchable(
            prompt = Prompt(
                id = id,
                title = title,
                description = description
            ),
            templateText = templateText
        )
    }

    /**
     * Deletes a prompt from the local database by its ID.
     *
     * @param promptId The unique identifier of the prompt to delete.
     */
    override suspend fun deletePrompt(promptId: Long) {
        promptDao.deletePromptById(promptId)
    }
}
