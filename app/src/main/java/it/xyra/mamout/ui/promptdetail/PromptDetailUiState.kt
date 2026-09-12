package it.xyra.mamout.ui.promptdetail

import androidx.compose.ui.text.input.TextFieldValue

/**
 * Represents the UI state for the Prompt Detail screen.
 *
 * @property promptId The unique identifier of the displayed prompt.
 * @property isLoading Indicates whether the prompt details are currently being loaded.
 * @property title The title of the prompt.
 * @property description The description of the prompt.
 * @property templateTextValue The raw template text, encapsulated in a [TextFieldValue] to manage text selection and cursor state.
 * @property inputValues A map storing temporary user inputs for dynamic template fields, keyed by field identifier.
 * @property isRawMode Flag indicating whether the viewer is displaying raw text mode (`true`) or structured preview mode (`false`).
 * @property isEditingHeader Flag indicating whether the header editing dialog (title and description) is currently visible.
 * @property errorMessage An optional error message to display if an operation fails, or `null` if no error occurred.
 */
data class PromptDetailUiState(
    val promptId: Long = 0L,
    val isLoading: Boolean = true,
    val title: String = "",
    val description: String = "",
    val templateTextValue: TextFieldValue = TextFieldValue(""),
    val inputValues: Map<String, String> = emptyMap(),
    val isRawMode: Boolean = false,
    val isEditingHeader: Boolean = false,
    val errorMessage: String? = null
)
