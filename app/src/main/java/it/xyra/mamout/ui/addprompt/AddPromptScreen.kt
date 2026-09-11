package it.xyra.mamout.ui.addprompt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import it.xyra.mamout.ui.components.promptviewer.AdvancedPromptViewer
import it.xyra.mamout.ui.components.promptviewer.InteractivePromptViewer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPromptScreen(
    viewModel: AddPromptViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Template") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentStep is AddPromptStep.Input) {
                            onBack()
                        } else {
                            viewModel.onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Toggle moved to AdvancedPromptViewer
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (val step = uiState.currentStep) {
                is AddPromptStep.Input -> {
                    InputStepContent(
                        rawText = uiState.rawText,
                        onTextChange = viewModel::onRawTextChange,
                        onNext = viewModel::onNextClick
                    )
                }
                is AddPromptStep.Visualization -> {
                    VisualizationStepContent(
                        metaPrompt = step.metaPrompt,
                        jsonResponse = uiState.jsonResponse,
                        onJsonResponseChange = viewModel::onJsonResponseChange,
                        onFinish = viewModel::onFinishClick
                    )
                }
                is AddPromptStep.Preview -> {
                    PreviewStepContent(
                        templateTextValue = uiState.templateTextValue,
                        isRawMode = uiState.isRawMode,
                        onTemplateTextValueChange = viewModel::onTemplateTextValueChange,
                        onNextAction = viewModel::goToNextInput,
                        onPreviousAction = viewModel::goToPreviousInput,
                        onToggleRawMode = viewModel::toggleRawMode
                    )
                }
            }
        }
    }
}

@Composable
fun InputStepContent(
    rawText: String,
    onTextChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = rawText,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("Paste your raw prompt here") },
            placeholder = { Text("Example: Write a blog post about...") }
        )
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = rawText.isNotBlank()
        ) {
            Text("Next")
        }
    }
}

@Composable
fun VisualizationStepContent(
    metaPrompt: String,
    jsonResponse: String,
    onJsonResponseChange: (String) -> Unit,
    onFinish: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var isCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Reset state after 2 seconds
    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step 2: Analysis & Extraction",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "1. Copy the Meta-Prompt using the button below.\n2. Send it to your favorite LLM.\n3. Paste the JSON response in the field below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = { 
                clipboardManager.setText(AnnotatedString(metaPrompt))
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isCopied = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = if (isCopied) 
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary) 
                else ButtonDefaults.buttonColors()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(if (isCopied) "Copied!" else "Copy Meta-Prompt")
            }
        }

        OutlinedTextField(
            value = jsonResponse,
            onValueChange = onJsonResponseChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("Paste LLM JSON Response here") },
            placeholder = { Text("{\"matches\": [...]}") }
        )

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            enabled = jsonResponse.isNotBlank()
        ) {
            Text("Finish")
        }
    }
}

@Composable
fun PreviewStepContent(
    templateTextValue: TextFieldValue,
    isRawMode: Boolean,
    onTemplateTextValueChange: (TextFieldValue) -> Unit,
    onNextAction: () -> Unit,
    onPreviousAction: () -> Unit,
    onToggleRawMode: () -> Unit
) {
    var inputValues by remember { mutableStateOf(mapOf<String, String>()) }
    
    AdvancedPromptViewer(
        templateTextValue = templateTextValue,
        onTemplateTextValueChange = onTemplateTextValueChange,
        isRawMode = isRawMode,
        onToggleRawMode = onToggleRawMode,
        onNextInput = onNextAction,
        onPreviousInput = onPreviousAction,
        inputValues = inputValues,
        onInputValueChange = { id, newValue ->
            inputValues = inputValues + (id to newValue)
        },
        modifier = Modifier.fillMaxSize()
    )
}
