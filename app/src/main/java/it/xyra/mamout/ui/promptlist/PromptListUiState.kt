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
     */
    data class Success(val prompts: List<Prompt>) : PromptListUiState

    /**
     * Indicates that the prompt list was loaded successfully but contains no items.
     */
    data object Empty : PromptListUiState

    /**
     * Indicates that an error occurred while loading the prompt list.
     *
     * @property message A human-readable error message describing the failure.
     */
    data class Error(val message: String) : PromptListUiState
}
