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
 * Represents the UI state for the search screen.
 *
 * @property query The current search query text entered by the user.
 * @property results The list of prompts that match the active search query.
 * @property isSearching Indicates whether a search operation is currently in progress.
 * @property selectedPromptId The identifier of the currently selected prompt item, or null if none is selected.
 * @property promptToDeleteId The identifier of the prompt marked for deletion, or null if no deletion is pending.
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
 * ViewModel responsible for managing search logic, item selection, and prompt deletion states on the search screen.
 *
 * @property repository The repository providing access to prompt data and deletion operations.
 * @property searchPromptsUseCase The use case responsible for filtering prompts based on the search query.
 */
class SearchViewModel(
    private val repository: PromptRepository,
    private val searchPromptsUseCase: SearchPromptsUseCase = SearchPromptsUseCase()
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())

    /**
     * StateFlow exposing the current state of the search screen.
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
     * Updates the current search query and triggers filtered prompt computation.
     *
     * @param newQuery The new search query string.
     */
    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(isSearching = true) }
        _query.value = newQuery
    }

    /**
     * Handles single click interactions on prompt items.
     *
     * Toggles selection mode if an item is already selected.
     *
     * @param prompt The prompt item that was clicked.
     */
    fun onPromptClick(prompt: Prompt) {
        if (_uiState.value.selectedPromptId != null) {
            onPromptLongClick(prompt.id)
        }
    }

    /**
     * Toggles the selection state of a prompt item on long click.
     *
     * @param promptId The unique identifier of the target prompt.
     */
    fun onPromptLongClick(promptId: Long) {
        _uiState.update {
            it.copy(selectedPromptId = if (it.selectedPromptId == promptId) null else promptId)
        }
    }

    /**
     * Displays the confirmation dialog to delete a specific prompt.
     *
     * @param promptId The unique identifier of the prompt requested for deletion.
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
     * Confirms and performs prompt deletion via repository, then resets selection states.
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
     * Dismisses the prompt deletion confirmation dialog without performing deletion.
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
