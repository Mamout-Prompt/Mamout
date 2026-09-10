package it.xyra.mamout.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.model.PromptSearchable
import it.xyra.mamout.domain.repository.PromptRepository
import it.xyra.mamout.domain.usecase.SearchPromptsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
 * This ViewModel interacts with [PromptRepository] to observe the latest data
 * and [SearchPromptsUseCase] to filter prompts based on the user's query.
 *
 * @property repository The repository providing access to prompt data.
 * @property searchPromptsUseCase The use case used for filtering results.
 */
class SearchViewModel(
    private val repository: PromptRepository,
    private val searchPromptsUseCase: SearchPromptsUseCase = SearchPromptsUseCase()
) : ViewModel() {

    private val _query = MutableStateFlow("")
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Observe both the search query and the repository data to update results reactively.
        combine(_query, repository.getSearchablePrompts()) { query, items ->
            val filtered = searchPromptsUseCase(items, query)
            _uiState.update { 
                it.copy(
                    query = query,
                    results = filtered,
                    isSearching = false
                )
            }
        }.launchIn(viewModelScope)
    }

    /**
     * Updates the search query and triggers a re-filtering of the data.
     *
     * @param newQuery The new search query string.
     */
    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(isSearching = true) }
        _query.value = newQuery
    }
}
