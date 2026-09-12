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
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the UI state and user interactions of the prompt list screen.
 *
 * Handles fetching prompts, applying real-time search filtering, card selection, and prompt deletion.
 *
 * @property repository The repository providing access to prompt data.
 * @property searchPromptsUseCase The use case responsible for filtering prompts based on search queries.
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

    private val _selectedPromptId = MutableStateFlow<Long?>(null)
    private val _isDeleteDialogVisible = MutableStateFlow(false)

    /**
     * StateFlow exposing the current UI state of the prompt list screen.
     *
     * Combines data from the repository, search query changes, selection state,
     * and deletion dialog visibility into a unified [PromptListUiState].
     */
    val uiState: StateFlow<PromptListUiState> = combine(
        repository.getSearchablePrompts(),
        _searchQuery,
        _selectedPromptId,
        _isDeleteDialogVisible
    ) { searchable, query, selectedId, isDialogVisible ->
        val filtered = searchPromptsUseCase(searchable, query)
        when {
            filtered.isNotEmpty() -> PromptListUiState.Success(
                prompts = filtered,
                selectedPromptId = selectedId,
                isDeleteDialogVisible = isDialogVisible
            )
            searchable.isEmpty() -> PromptListUiState.Empty
            else -> PromptListUiState.NoResults(query)
        }
    }
        .catch { emit(PromptListUiState.Error(it.localizedMessage ?: "An unexpected error occurred")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PromptListUiState.Loading
        )

    /**
     * Updates the active search query.
     *
     * @param newQuery The new text string to filter prompts by.
     */
    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    /**
     * Toggles the selection state for a specific prompt item on long click.
     *
     * @param promptId The unique identifier of the target prompt.
     */
    fun onPromptLongClick(promptId: Long) {
        _selectedPromptId.value = if (_selectedPromptId.value == promptId) null else promptId
    }

    /**
     * Clears any active prompt selection when a prompt is clicked.
     *
     * @param promptId The unique identifier of the clicked prompt.
     */
    fun onPromptClick(promptId: Long) {
        if (_selectedPromptId.value != null) {
            _selectedPromptId.value = null
        }
    }

    /**
     * Requests prompt deletion by displaying the confirmation dialog.
     *
     * If an explicit [promptId] is provided, it sets that prompt as selected before
     * showing the dialog.
     *
     * @param promptId Optional unique identifier of the prompt to delete.
     */
    fun onDeleteRequested(promptId: Long? = null) {
        if (promptId != null) {
            _selectedPromptId.value = promptId
        }
        if (_selectedPromptId.value != null) {
            _isDeleteDialogVisible.value = true
        }
    }

    /**
     * Dismisses the delete confirmation dialog without performing any deletion.
     */
    fun onDeleteDialogDismissed() {
        _isDeleteDialogVisible.value = false
    }

    /**
     * Confirms and executes the deletion of the currently selected prompt.
     *
     * Dismisses the dialog, calls the repository deletion method, and resets the selection.
     */
    fun onDeleteConfirmed() {
        val idToDelete = _selectedPromptId.value ?: return
        viewModelScope.launch {
            _isDeleteDialogVisible.value = false
            repository.deletePrompt(idToDelete)
            _selectedPromptId.value = null
        }
    }
}