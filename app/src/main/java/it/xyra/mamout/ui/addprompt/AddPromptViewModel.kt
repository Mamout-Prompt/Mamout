package it.xyra.mamout.ui.addprompt

import androidx.lifecycle.ViewModel
import it.xyra.mamout.domain.usecase.TemplatizePromptUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import it.xyra.mamout.domain.parser.ParsedPromptTemplate

sealed class AddPromptStep {
    data object Input : AddPromptStep()
    data class Visualization(val metaPrompt: String) : AddPromptStep()
    data class Preview(val template: ParsedPromptTemplate) : AddPromptStep()
}

data class AddPromptUiState(
    val currentStep: AddPromptStep = AddPromptStep.Input,
    val rawText: String = "",
    val jsonResponse: String = "",
    val templateTextValue: TextFieldValue = TextFieldValue(""),
    val isRawMode: Boolean = false
)

class AddPromptViewModel(
    private val templatizePromptUseCase: TemplatizePromptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPromptUiState())
    val uiState: StateFlow<AddPromptUiState> = _uiState.asStateFlow()

    fun onRawTextChange(text: String) {
        _uiState.update { it.copy(rawText = text) }
    }

    fun onJsonResponseChange(text: String) {
        _uiState.update { it.copy(jsonResponse = text) }
    }

    fun onNextClick() {
        val currentState = _uiState.value
        if (currentState.rawText.isNotBlank()) {
            val metaPrompt = templatizePromptUseCase.preparePromptForLlm(currentState.rawText)
            _uiState.update {
                it.copy(currentStep = AddPromptStep.Visualization(metaPrompt))
            }
        }
    }

    fun onBackClick() {
        when (_uiState.value.currentStep) {
            is AddPromptStep.Visualization -> {
                _uiState.update { it.copy(currentStep = AddPromptStep.Input) }
            }
            is AddPromptStep.Preview -> {
                _uiState.update { it.copy(currentStep = AddPromptStep.Visualization(_uiState.value.jsonResponse)) }
            }
            else -> {}
        }
    }

    fun onFinishClick() {
        val currentState = _uiState.value
        if (currentState.jsonResponse.isNotBlank()) {
            val template = templatizePromptUseCase.templatize(
                currentState.rawText,
                currentState.jsonResponse
            )
            _uiState.update { 
                it.copy(
                    currentStep = AddPromptStep.Preview(template),
                    templateTextValue = TextFieldValue(template.rawTemplate)
                )
            }
        }
    }

    fun onTemplateTextValueChange(newValue: TextFieldValue) {
        _uiState.update { it.copy(templateTextValue = newValue) }
    }

    fun goToNextInput() {
        val currentState = _uiState.value
        val text = currentState.templateTextValue.text
        val cursorPosition = currentState.templateTextValue.selection.end
        
        // Use the same regex as the viewer to find valid inputs
        val regex = Regex("<INPUT\\s+[^>]*type=\"(text|smallText|options)\"[^>]*>.*?</INPUT>", 
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        
        val matches = regex.findAll(text).toList()
        if (matches.isEmpty()) return

        val nextMatch = matches.find { it.range.first > cursorPosition } ?: matches.first()
        
        _uiState.update { 
            it.copy(
                templateTextValue = currentState.templateTextValue.copy(
                    selection = TextRange(nextMatch.range.first, nextMatch.range.last + 1)
                )
            )
        }
    }

    fun goToPreviousInput() {
        val currentState = _uiState.value
        val text = currentState.templateTextValue.text
        val cursorPosition = currentState.templateTextValue.selection.start
        
        val regex = Regex("<INPUT\\s+[^>]*type=\"(text|smallText|options)\"[^>]*>.*?</INPUT>", 
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        
        val matches = regex.findAll(text).toList()
        if (matches.isEmpty()) return

        val prevMatch = matches.findLast { it.range.last < cursorPosition } ?: matches.last()
        
        _uiState.update { 
            it.copy(
                templateTextValue = currentState.templateTextValue.copy(
                    selection = TextRange(prevMatch.range.first, prevMatch.range.last + 1)
                )
            )
        }
    }

    fun toggleRawMode() {
        _uiState.update { it.copy(isRawMode = !it.isRawMode) }
    }
}
