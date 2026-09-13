package it.xyra.mamout.ui.addprompt

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.xyra.mamout.ui.promptviewer.AdvancedPromptViewer
import it.xyra.mamout.domain.parser.InputType
import it.xyra.mamout.domain.usecase.TemplatizePromptUseCase
import it.xyra.mamout.ui.theme.MamoutTheme
import kotlinx.coroutines.delay

private val VALID_INPUT_REGEX = Regex(
    "<INPUT\\s+[^>]*type=\"(${InputType.allKeys.joinToString("|")})\"[^>]*>(?:(?!<INPUT).)*?</INPUT>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPromptScreen(
    viewModel: AddPromptViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetState()
        }
    }

    AddPromptContent(
        uiState = uiState,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onTemplateTextChange = viewModel::onTemplateTextChange,
        onToggleRawMode = viewModel::onToggleRawMode,
        onInputValueChange = viewModel::onInputValueChange,
        onNextStep = viewModel::nextStep,
        onPreviousStep = viewModel::previousStep,
        onSavePrompt = viewModel::savePrompt,
        onPrepareLlmPrompt = viewModel::prepareLlmPrompt,
        onApplyLlmResponse = viewModel::applyLlmResponse,
        onClearError = viewModel::clearError,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPromptContent(
    uiState: AddPromptUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTemplateTextChange: (TextFieldValue) -> Unit,
    onToggleRawMode: () -> Unit,
    onInputValueChange: (id: String, newValue: String) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onSavePrompt: () -> Unit,
    onPrepareLlmPrompt: () -> Unit,
    onApplyLlmResponse: (String) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Add Prompt - Step ${uiState.step} of 3") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    if (uiState.step > 1) {
                        TextButton(onClick = onPreviousStep) {
                            Text("Previous")
                        }
                    }
                },
                floatingActionButton = {
                    if (uiState.step < 3) {
                        Button(
                            onClick = onNextStep,
                            enabled = when(uiState.step) {
                                1 -> uiState.title.isNotBlank()
                                2 -> uiState.templateText.text.isNotBlank()
                                else -> true
                            }
                        ) {
                            Text("Next")
                        }
                    } else {
                        ExtendedFloatingActionButton(
                            onClick = onSavePrompt,
                            icon = { Icon(Icons.Default.Check, null) },
                            text = { Text("Save Prompt") },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = uiState.step,
                label = "StepTransition"
            ) { step ->
                when (step) {
                    1 -> BasicInfoStep(
                        title = uiState.title,
                        description = uiState.description,
                        onTitleChange = onTitleChange,
                        onDescriptionChange = onDescriptionChange
                    )
                    2 -> AiTemplatizeStep(
                        templateText = uiState.templateText,
                        onTemplateTextChange = onTemplateTextChange,
                        llmPrompt = uiState.llmPrompt,
                        onPrepareLlmPrompt = onPrepareLlmPrompt,
                        onApplyLlmResponse = onApplyLlmResponse
                    )
                    3 -> TemplatizeStep(
                        uiState = uiState,
                        onTemplateTextChange = onTemplateTextChange,
                        onToggleRawMode = onToggleRawMode,
                        onInputValueChange = onInputValueChange
                    )
                }
            }

            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.error?.let { error ->
                AlertDialog(
                    onDismissRequest = onClearError,
                    confirmButton = {
                        TextButton(onClick = onClearError) { Text("OK") }
                    },
                    title = { Text("Error") },
                    text = { Text(error) }
                )
            }
        }
    }
}

@Composable
private fun BasicInfoStep(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Give your prompt a name and description.", style = MaterialTheme.typography.bodyLarge)
        
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
    }
}

@Composable
private fun AiTemplatizeStep(
    templateText: TextFieldValue,
    onTemplateTextChange: (TextFieldValue) -> Unit,
    llmPrompt: String?,
    onPrepareLlmPrompt: () -> Unit,
    onApplyLlmResponse: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var llmResponse by remember { mutableStateOf("") }
    var showCopiedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(llmPrompt) {
        if (llmPrompt != null) {
            clipboardManager.setText(AnnotatedString(llmPrompt))
            showCopiedFeedback = true
            delay(2000)
            showCopiedFeedback = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("Paste your raw prompt below.", style = MaterialTheme.typography.titleLarge)
            Text(
                "We will wrap it in a meta-prompt. You should send it to an LLM (Gemini, Claude, GPT, etc.) to detect dynamic fields.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = templateText,
            onValueChange = onTemplateTextChange,
            label = { Text("Raw Prompt") },
            modifier = Modifier
                .fillMaxWidth(),
            minLines = 6,
            placeholder = { Text("e.g. Write an email to [name] about [topic]...") }
        )

        Button(
            onClick = onPrepareLlmPrompt,
            modifier = Modifier.align(Alignment.End),
            enabled = templateText.text.isNotBlank(),
            colors = if (showCopiedFeedback) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Text(if (showCopiedFeedback) "Copied!" else "Copy Meta-Prompt")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Paste the JSON response from the LLM here:", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = llmResponse,
                onValueChange = { llmResponse = it },
                modifier = Modifier
                    .fillMaxWidth(),
                minLines = 5,
                placeholder = { Text("Paste JSON here...") }
            )

            Button(
                onClick = { onApplyLlmResponse(llmResponse) },
                modifier = Modifier.fillMaxWidth(),
                enabled = llmResponse.isNotBlank()
            ) {
                Text("Apply & Next")
            }
        }
    }
}

@Composable
private fun TemplatizeStep(
    uiState: AddPromptUiState,
    onTemplateTextChange: (TextFieldValue) -> Unit,
    onToggleRawMode: () -> Unit,
    onInputValueChange: (id: String, newValue: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Refine your prompt by adding <INPUT> tags.", style = MaterialTheme.typography.bodyLarge)
        
        val onNavigate = { direction: Int ->
            val text = uiState.templateText.text
            val matches = VALID_INPUT_REGEX.findAll(text).toList()

            if (matches.isNotEmpty()) {
                val nextMatch = if (direction > 0) {
                    matches.find { it.range.first > uiState.templateText.selection.end } ?: matches.first()
                } else {
                    matches.findLast { it.range.last < uiState.templateText.selection.start } ?: matches.last()
                }
                onTemplateTextChange(uiState.templateText.copy(selection = TextRange(nextMatch.range.first, nextMatch.range.last + 1)))
            }
        }

        AdvancedPromptViewer(
            templateTextValue = uiState.templateText,
            onTemplateTextValueChange = onTemplateTextChange,
            isRawMode = uiState.isRawMode,
            onToggleRawMode = onToggleRawMode,
            onNextInput = { onNavigate(1) },
            onPreviousInput = { onNavigate(-1) },
            inputValues = uiState.inputValues,
            onInputValueChange = onInputValueChange,
            enabled = uiState.isRawMode,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddPromptScreenPreview() {
    var uiState by remember { mutableStateOf(AddPromptUiState(
        step = 2,
        title = "Preview Prompt",
        description = "A description for testing",
        templateText = TextFieldValue("Write an email to [name] about [topic].")
    )) }

    MamoutTheme {
        AddPromptContent(
            uiState = uiState,
            onTitleChange = { uiState = uiState.copy(title = it) },
            onDescriptionChange = { uiState = uiState.copy(description = it) },
            onTemplateTextChange = { 
                uiState = uiState.copy(templateText = it)
            },
            onToggleRawMode = { uiState = uiState.copy(isRawMode = !uiState.isRawMode) },
            onInputValueChange = { id, newValue -> 
                uiState = uiState.copy(inputValues = uiState.inputValues + (id to newValue))
            },
            onNextStep = { uiState = uiState.copy(step = (uiState.step + 1).coerceAtMost(3)) },
            onPreviousStep = { uiState = uiState.copy(step = (uiState.step - 1).coerceAtLeast(1)) },
            onSavePrompt = { /* Save action */ },
            onPrepareLlmPrompt = {
                uiState = uiState.copy(
                    llmPrompt = TemplatizePromptUseCase().preparePromptForLlm(uiState.templateText.text)
                )
            },
            onApplyLlmResponse = { 
                uiState = uiState.copy(
                    templateText = TextFieldValue("Hello <INPUT type=\"smallText\">World</INPUT>!"),
                    step = 3
                )
            },
            onClearError = { uiState = uiState.copy(error = null) },
            onBack = {}
        )
    }
}
