package it.xyra.mamout.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.repository.PromptRepository
import it.xyra.mamout.domain.usecase.SearchPromptsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the search feature.
 *
 * @property query The active search query string.
 * @property results The list of [Prompt] domain models matching the active search query.
 * @property isSearching Indicates whether a search query operation is currently being processed.
 * @property selectedPromptId The ID of the currently selected prompt item, or `null` if no item is selected.
 * @property promptToDeleteId The ID of the prompt targeted for deletion, or `null` if no deletion is pending.
 * @property isDeleteDialogVisible Indicates whether the delete confirmation dialog is currently displayed.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<Prompt> = emptyList(),
    val isSearching: Boolean = false,
    val selectedPromptId: Long? = null,
    val promptToDeleteId: Long? = null,
    val isDeleteDialogVisible: Boolean = false
)

/**
 * ViewModel responsible for managing search execution and UI state transitions.
 *
 * Combines search queries with searchable prompt streams from the repository, applying
 * filtering logic via [SearchPromptsUseCase] and handling item actions such as deletion.
 *
 * @property repository Repository providing prompt data operations.
 * @property searchPromptsUseCase Use case executing search filter algorithms against prompt items.
 */
class SearchViewModel(
    private val repository: PromptRepository,
    private val searchPromptsUseCase: SearchPromptsUseCase = SearchPromptsUseCase()
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())

    /**
     * The observable UI state stream for the search feature.
     */
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
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
     * Updates the active search query string and sets search execution status.
     *
     * @param newQuery The updated search query string.
     */
    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(isSearching = true) }
        _query.value = newQuery
    }

    /**
     * Clears current selection state if a prompt is selected.
     */
    fun onPromptClick() {
        if (_uiState.value.selectedPromptId != null) {
            _uiState.update { it.copy(selectedPromptId = null) }
        }
    }

    /**
     * Handles long-click interactions on a prompt item to toggle its selection state.
     *
     * @param promptId The unique identifier of the target prompt.
     */
    fun onPromptLongClick(promptId: Long) {
        _uiState.update {
            it.copy(selectedPromptId = if (it.selectedPromptId == promptId) null else promptId)
        }
    }

    /**
     * Prepares a prompt for deletion and displays the confirmation dialog.
     *
     * @param promptId The unique identifier of the prompt to delete.
     */
    fun onDeleteRequested(promptId: Long) {
        _uiState.update {
            it.copy(
                promptToDeleteId = promptId,
                isDeleteDialogVisible = true
            )
        }
    }

    /**
     * Confirms and executes prompt deletion using the repository, resetting deletion state upon completion.
     */
    fun onDeleteConfirmed() {
        val idToDelete = _uiState.value.promptToDeleteId ?: return
        viewModelScope.launch {
            repository.deletePrompt(idToDelete)
            _uiState.update {
                it.copy(
                    selectedPromptId = null,
                    promptToDeleteId = null,
                    isDeleteDialogVisible = false
                )
            }
        }
    }

    /**
     * Dismisses the prompt deletion confirmation dialog without modifying data.
     */
    fun onDeleteDialogDismissed() {
        _uiState.update {
            it.copy(
                promptToDeleteId = null,
                isDeleteDialogVisible = false
            )
        }
    }
}
