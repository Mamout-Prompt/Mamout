package it.xyra.mamout.ui.promptdetail

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.xyra.mamout.domain.parser.TagPromptParser
import it.xyra.mamout.domain.repository.PromptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the UI state and business logic of the Prompt Detail screen.
 *
 * Handles loading prompt data, editing header details, toggling between raw and preview modes,
 * navigating dynamic input tags, and persisting updates to the repository.
 *
 * @param savedStateHandle State handle containing arguments passed during navigation, including the prompt ID.
 * @property repository Repository providing data access operations for prompts.
 */
class PromptDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: PromptRepository
) : ViewModel() {

    private val promptId: Long = checkNotNull(savedStateHandle["promptId"])

    private val _uiState = MutableStateFlow(PromptDetailUiState(promptId = promptId))

    /**
     * The observable UI state stream for the prompt detail screen.
     */
    val uiState: StateFlow<PromptDetailUiState> = _uiState.asStateFlow()

    init {
        loadPrompt()
    }

    /**
     * Loads the prompt and its associated content from the repository.
     *
     * Initial emission initializes the template text value; subsequent emissions update title and description
     * to avoid overwriting ongoing raw text modifications.
     */
    private fun loadPrompt() {
        viewModelScope.launch {
            repository.getPromptById(promptId).collect { searchable ->
                if (searchable != null) {
                    _uiState.update { current ->
                        if (current.isLoading) {
                            current.copy(
                                isLoading = false,
                                title = searchable.prompt.title,
                                description = searchable.prompt.description,
                                templateTextValue = TextFieldValue(searchable.templateText)
                            )
                        } else {
                            current.copy(
                                title = searchable.prompt.title,
                                description = searchable.prompt.description
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Prompt not found")
                    }
                }
            }
        }
    }

    /**
     * Updates the prompt title in the current state.
     *
     * @param newTitle The updated title string.
     */
    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    /**
     * Updates the prompt description in the current state.
     *
     * @param newDescription The updated description string.
     */
    fun onDescriptionChange(newDescription: String) {
        _uiState.update { it.copy(description = newDescription) }
    }

    /**
     * Toggles the visibility of the header editing dialog.
     *
     * @param show `true` to display the edit dialog; `false` to hide it.
     */
    fun onToggleEditHeader(show: Boolean) {
        _uiState.update { it.copy(isEditingHeader = show) }
    }

    /**
     * Toggles between raw text editing mode and structured preview mode.
     */
    fun onToggleRawMode() {
        _uiState.update { it.copy(isRawMode = !it.isRawMode) }
    }

    /**
     * Updates the raw template text value in the current state.
     *
     * @param newValue The new [TextFieldValue] containing updated text and selection state.
     */
    fun onTemplateTextValueChange(newValue: TextFieldValue) {
        _uiState.update { it.copy(templateTextValue = newValue) }
    }

    /**
     * Updates a single dynamic input field value in preview mode.
     *
     * @param id The unique identifier of the target input tag.
     * @param newValue The updated text value for the target field.
     */
    fun onInputValueChange(id: String, newValue: String) {
        _uiState.update {
            val updatedMap = it.inputValues.toMutableMap()
            updatedMap[id] = newValue
            it.copy(inputValues = updatedMap)
        }
    }

    /**
     * Persists the current prompt title, description, and raw template text to the repository.
     */
    fun savePrompt() {
        viewModelScope.launch {
            val state = _uiState.value
            repository.updatePrompt(
                promptId = promptId,
                title = state.title,
                description = state.description,
                templateText = state.templateTextValue.text
            )
        }
    }

    /**
     * Generates the final compiled prompt string by parsing raw template tags and replacing
     * dynamic input placeholders with user-provided values.
     *
     * @return The fully compiled prompt text.
     */
    fun getCompiledPrompt(): String {
        val rawText = uiState.value.templateTextValue.text
        val parsed = TagPromptParser.parse(rawText)
        return TagPromptParser.buildFinalPrompt(parsed, uiState.value.inputValues)
    }

    /**
     * Navigates text selection focus between dynamic `<INPUT>` tags within the template text.
     *
     * @param direction A positive integer moves selection forward to the next input tag;
     * a negative integer moves backward to the previous input tag.
     */
    fun onNavigateInput(direction: Int) {
        val currentText = uiState.value.templateTextValue.text
        val regex = Regex("""<INPUT\b[^>]*>(?:(?!<INPUT).)*?</INPUT>""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(currentText).toList()

        if (matches.isEmpty()) return

        val currentSelection = uiState.value.templateTextValue.selection
        val nextMatch = if (direction > 0) {
            matches.find { it.range.first > currentSelection.end } ?: matches.first()
        } else {
            matches.findLast { it.range.last < currentSelection.start } ?: matches.last()
        }

        _uiState.update {
            it.copy(
                templateTextValue = it.templateTextValue.copy(
                    selection = TextRange(nextMatch.range.first, nextMatch.range.last + 1)
                )
            )
        }
    }
}
