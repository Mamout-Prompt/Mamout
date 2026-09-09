package it.xyra.mamout.ui.promptlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.xyra.mamout.domain.repository.PromptRepository
import it.xyra.mamout.domain.usecase.SearchPromptsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel responsible for managing the UI state of the prompt list screen.
 *
 * Handles fetching prompts from the repository and applying search query filtering
 * via [SearchPromptsUseCase].
 *
 * @property repository The repository used to retrieve prompt data.
 * @property searchPromptsUseCase The use case used to filter prompts according to search criteria.
 */
class PromptListViewModel(
    private val repository: PromptRepository,
    private val searchPromptsUseCase: SearchPromptsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    /**
     * The current search query string entered by the user.
     */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * The current state of the UI, combining repository stream updates and search filtering.
     */
    val uiState: StateFlow<PromptListUiState> = combine(
        repository.getSearchablePrompts(),
        _searchQuery
    ) { searchable, query ->
        val filtered = searchPromptsUseCase(searchable, query)
        if (filtered.isEmpty()) {
            PromptListUiState.Empty
        } else {
            PromptListUiState.Success(filtered)
        }
    }
        .catch { emit(PromptListUiState.Error(it.localizedMessage ?: "An unexpected error occurred")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PromptListUiState.Loading
        )

    /**
     * Updates the search query filter.
     *
     * @param newQuery The new search query string.
     */
    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }
}
