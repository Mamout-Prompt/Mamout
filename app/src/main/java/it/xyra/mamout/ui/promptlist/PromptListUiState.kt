package it.xyra.mamout.ui.promptlist

import it.xyra.mamout.domain.model.Prompt

/**
 * Represents the various UI states for the prompt list screen.
 */
sealed interface PromptListUiState {

    /**
     * Indicates that the prompt list is currently being loaded.
     */
    data object Loading : PromptListUiState

    /**
     * Indicates that the prompt list was loaded successfully and contains items.
     *
     * @property prompts The list of [Prompt] domain models to be displayed.
     * @property selectedPromptId The ID of the currently selected prompt, or null if none selected.
     * @property isDeleteDialogVisible Controls the visibility of the delete confirmation dialog.
     */
    data class Success(
        val prompts: List<Prompt>,
        val selectedPromptId: Long? = null,
        val isDeleteDialogVisible: Boolean = false
    ) : PromptListUiState

    /**
     * Indicates that no prompts have ever been saved (the underlying list is empty
     * regardless of the search query).
     */
    data object Empty : PromptListUiState

    /**
     * Indicates that prompts exist but none match the active search [query].
     *
     * Distinct from [Empty] so the UI can show "no results for this search" instead of
     * the misleading "no prompts saved" message when the user has simply typed a query
     * that doesn't match anything.
     *
     * @property query The search query that produced no matches.
     */
    data class NoResults(val query: String) : PromptListUiState

    /**
     * Indicates that an error occurred while loading the prompt list.
     *
     * @property message A human-readable error message describing the failure.
     */
    data class Error(val message: String) : PromptListUiState
}