package it.xyra.mamout.ui.search

import androidx.lifecycle.ViewModel
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.model.PromptSearchable
import it.xyra.mamout.domain.usecase.SearchPromptsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state for the Search Screen.
 *
 * @property query The current search text entered by the user.
 * @property results The list of prompts that match the current query.
 * @property isSearching Indicates whether a search operation is currently active.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<Prompt> = emptyList(),
    val isSearching: Boolean = false
)

/**
 * ViewModel for the Search Screen, managing search logic and state.
 *
 * This ViewModel interacts with [SearchPromptsUseCase] to provide real-time
 * filtering of prompts as the user types.
 */
class SearchViewModel(
    private val searchPromptsUseCase: SearchPromptsUseCase = SearchPromptsUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * Internal list of all searchable prompts.
     * In a production environment, this would typically be observed from a repository.
     */
    private var allPrompts: List<PromptSearchable> = emptyList()

    /**
     * Initializes the ViewModel with a data set.
     *
     * @param items The list of prompts available for searching.
     */
    fun setInitialData(items: List<PromptSearchable>) {
        allPrompts = items
        performSearch(_uiState.value.query)
    }

    /**
     * Updates the search query and triggers a new search.
     *
     * @param newQuery The new query string.
     */
    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, isSearching = true) }
        performSearch(newQuery)
    }

    private fun performSearch(query: String) {
        val filteredResults = searchPromptsUseCase(allPrompts, query)
        _uiState.update { 
            it.copy(
                results = filteredResults,
                isSearching = false
            )
        }
    }
}
