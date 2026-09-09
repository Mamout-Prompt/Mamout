package it.xyra.mamout.domain.usecase

import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.model.PromptSearchable

/**
 * Use case responsible for filtering prompts based on a search query.
 */
class SearchPromptsUseCase {

    companion object {
        /** Minimum query length required to trigger text filtering. */
        const val MIN_QUERY_LENGTH = 2
    }

    /**
     * Filters a list of searchable prompts by matching the query against title, description, or template text.
     *
     * @param items The list of searchable prompt wrappers.
     * @param query The search query string entered by the user.
     * @return A list of lightweight [Prompt] domain objects matching the criteria,
     * or all prompts if the query is shorter than [MIN_QUERY_LENGTH].
     */
    operator fun invoke(items: List<PromptSearchable>, query: String): List<Prompt> {
        val trimmedQuery = query.trim()

        if (trimmedQuery.length < MIN_QUERY_LENGTH) {
            return items.map { it.prompt }
        }

        return items
            .filter { searchable ->
                searchable.prompt.title.contains(trimmedQuery, ignoreCase = true) ||
                        searchable.prompt.description.contains(trimmedQuery, ignoreCase = true) ||
                        searchable.templateText.contains(trimmedQuery, ignoreCase = true)
            }
            .map { it.prompt }
    }
}
