package it.xyra.mamout.data.repository

import it.xyra.mamout.data.local.PromptDao
import it.xyra.mamout.data.local.PromptEntity
import it.xyra.mamout.data.local.PromptSearchableDb
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.model.PromptSearchable
import it.xyra.mamout.domain.repository.PromptRepository
import it.xyra.mamout.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of [PromptRepository] that manages prompt data operations
 * using [PromptDao] as the local data source.
 *
 * @property promptDao The Data Access Object providing database access operations.
 */
class PromptRepositoryImpl(
    private val promptDao: PromptDao
) : PromptRepository {

    /**
     * Retrieves all prompt headers from the database and maps them to domain models.
     *
     * @return A [Flow] emitting the list of all [Prompt] domain models.
     */
    override fun getPrompts(): Flow<List<Prompt>> {
        return promptDao.getPrompts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Retrieves all searchable prompts (headers combined with template content) from the database
     * and maps them to domain models.
     *
     * @return A [Flow] emitting a list of [PromptSearchable] domain models.
     */
    override fun getSearchablePrompts(): Flow<List<PromptSearchable>> {
        return promptDao.getSearchablePrompts().map { dbProjections ->
            dbProjections.map { it.toDomainModel() }
        }
    }

    /**
     * Retrieves a single searchable prompt by its unique identifier and maps it to a domain model.
     *
     * @param promptId The unique identifier of the prompt to retrieve.
     * @return A [Flow] emitting the matching [PromptSearchable] domain model, or `null` if not found.
     */
    override fun getPromptById(promptId: Long): Flow<PromptSearchable?> {
        return promptDao.getSearchablePromptById(promptId).map { it?.toDomainModel() }
    }

    /**
     * Updates an existing prompt header details and its raw template content in the database.
     *
     * @param promptId The unique identifier of the prompt to update.
     * @param title The updated title of the prompt.
     * @param description The updated description of the prompt.
     * @param templateText The updated raw template text.
     */
    override suspend fun updatePrompt(
        promptId: Long,
        title: String,
        description: String,
        templateText: String
    ) {
        promptDao.updatePromptWithContent(
            promptId = promptId,
            title = title,
            description = description,
            templateText = templateText
        )
        SyncManager.syncAll()
    }

    override suspend fun savePrompt(title: String, description: String, templateText: String) {
        promptDao.insertPromptWithContent(
            prompt = PromptEntity(title = title, description = description),
            templateText = templateText
        )
        SyncManager.syncAll()
    }

    override suspend fun getAllPromptsSync(): List<PromptSearchable> {
        return promptDao.getSearchablePromptsSync().map { it.toDomainModel() }
    }

    override suspend fun syncPrompts(prompts: List<PromptSearchable>) {
        prompts.forEach { incoming ->
            val existing = promptDao.getSearchablePromptByTitleAndDescription(
                title = incoming.prompt.title,
                description = incoming.prompt.description
            )
            
            if (existing != null) {
                if (incoming.prompt.lastModified > existing.lastModified) {
                    promptDao.updatePromptWithContent(
                        promptId = existing.id,
                        title = incoming.prompt.title,
                        description = incoming.prompt.description,
                        templateText = incoming.templateText,
                        lastModified = incoming.prompt.lastModified
                    )
                }
            } else {
                promptDao.insertPromptWithContent(
                    prompt = PromptEntity(
                        title = incoming.prompt.title,
                        description = incoming.prompt.description,
                        lastModified = incoming.prompt.lastModified
                    ),
                    templateText = incoming.templateText
                )
            }
        }
    }

    /**
     * Deletes a prompt and its associated content from the database by its unique identifier.
     *
     * @param promptId The unique identifier of the prompt to delete.
     */
    override suspend fun deletePrompt(promptId: Long) {
        promptDao.deletePromptById(promptId)
    }

    /**
     * Maps a database [PromptEntity] to a domain [Prompt] model.
     */
    private fun PromptEntity.toDomainModel(): Prompt {
        return Prompt(
            id = id,
            title = title,
            description = description,
            lastModified = lastModified
        )
    }

    /**
     * Maps a database projection [PromptSearchableDb] to a domain [PromptSearchable] model.
     */
    private fun PromptSearchableDb.toDomainModel(): PromptSearchable {
        return PromptSearchable(
            prompt = Prompt(
                id = id,
                title = title,
                description = description,
                lastModified = lastModified
            ),
            templateText = templateText
        )
    }
}