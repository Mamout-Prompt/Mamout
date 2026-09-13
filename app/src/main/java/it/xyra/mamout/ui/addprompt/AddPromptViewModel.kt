package it.xyra.mamout.ui.addprompt

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.model.PromptSearchable
import it.xyra.mamout.domain.repository.PromptRepository
import it.xyra.mamout.domain.usecase.SearchPromptsUseCase
import it.xyra.mamout.domain.usecase.TemplatizePromptUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddPromptUiState(
    val step: Int = 1,
    val title: String = "",
    val description: String = "",
    val templateText: TextFieldValue = TextFieldValue(""),
    val isRawMode: Boolean = true,
    val inputValues: Map<String, String> = emptyMap(),
    val llmPrompt: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
class AddPromptViewModel(
    private val repository: PromptRepository,
    private val searchPromptsUseCase: SearchPromptsUseCase = SearchPromptsUseCase(),
    private val templatizeUseCase: TemplatizePromptUseCase = TemplatizePromptUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPromptUiState())
    val uiState: StateFlow<AddPromptUiState> = _uiState.asStateFlow()

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onDescriptionChange(newDescription: String) {
        _uiState.update { it.copy(description = newDescription) }
    }

    fun onTemplateTextChange(newValue: TextFieldValue) {
        _uiState.update { it.copy(templateText = newValue, llmPrompt = null) }
    }

    fun prepareLlmPrompt() {
        val currentText = _uiState.value.templateText.text
        if (currentText.isBlank()) return

        val llmPrompt = templatizeUseCase.preparePromptForLlm(currentText)
        _uiState.update { it.copy(llmPrompt = llmPrompt) }
    }

    fun applyLlmResponse(jsonResponse: String) {
        if (jsonResponse.isBlank()) {
            _uiState.update { it.copy(error = "Paste the LLM's JSON response first") }
            return
        }
        try {
            val currentText = _uiState.value.templateText.text
            val parsed = templatizeUseCase.templatize(currentText, jsonResponse)
            _uiState.update {
                it.copy(
                    templateText = TextFieldValue(parsed.rawTemplate),
                    step = 3 // Move to preview/refine step
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = "Couldn't read that as JSON. Make sure you pasted the full response: ${e.message}")
            }
        }
    }

    fun onToggleRawMode() {
        _uiState.update { it.copy(isRawMode = !it.isRawMode) }
    }

    fun onInputValueChange(id: String, newValue: String) {
        _uiState.update {
            it.copy(inputValues = it.inputValues + (id to newValue))
        }
    }

    fun nextStep() {
        _uiState.update {
            if (it.step < 3) it.copy(step = it.step + 1) else it
        }
    }

    fun previousStep() {
        _uiState.update {
            if (it.step > 1) it.copy(step = it.step - 1) else it
        }
    }

    fun savePrompt() {
        val state = _uiState.value
        if (state.title.isBlank() || state.templateText.text.isBlank()) {
            _uiState.update { it.copy(error = "Title and prompt text cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                repository.savePrompt(
                    title = state.title,
                    description = state.description,
                    templateText = state.templateText.text
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetState() {
        _uiState.value = AddPromptUiState()
    }
}