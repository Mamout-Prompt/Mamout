package it.xyra.mamout.ui.components.promptviewer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import it.xyra.mamout.domain.parser.InputType
import it.xyra.mamout.domain.parser.ParsedPromptTemplate
import it.xyra.mamout.domain.parser.PromptSegment
import it.xyra.mamout.domain.parser.TagPromptParser
import it.xyra.mamout.ui.theme.MamoutTheme

/**
 * A shared stateless component that renders a prompt template with interactive input fields.
 *
 * @param template The parsed prompt template containing static text and input fields.
 * @param inputValues A map of current values for each input field ID.
 * @param onValueChange Callback triggered when an input field's value is modified.
 * @param modifier The modifier to be applied to the root container.
 */
@Composable
fun InteractivePromptViewer(
    promptText: String,
    inputValues: Map<String, String>,
    onValueChange: (id: String, newValue: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedTemplate = remember(promptText) { TagPromptParser.parse(promptText) }
    
    InteractivePromptViewer(
        template = parsedTemplate,
        inputValues = inputValues,
        onValueChange = onValueChange,
        modifier = modifier
    )
}

/**
 * A shared stateless component that renders a prompt template with interactive input fields.
 *
 * @param template The parsed prompt template containing static text and input fields.
 * @param inputValues A map of current values for each input field ID.
 * @param onValueChange Callback triggered when an input field's value is modified.
 * @param modifier The modifier to be applied to the root container.
 */
@Composable
fun InteractivePromptViewer(
    template: ParsedPromptTemplate,
    inputValues: Map<String, String>,
    onValueChange: (id: String, newValue: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        template.segments.forEach { segment ->
            when (segment) {
                is PromptSegment.StaticText -> {
                    if (segment.text.isNotBlank()) {
                        Text(
                            text = PromptVisualizerEngine.highlight(segment.text),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                is PromptSegment.InputField -> {
                    PromptInputField(
                        field = segment,
                        currentValue = inputValues[segment.id] ?: segment.defaultValue,
                        onValueChange = { newValue -> onValueChange(segment.id, newValue) }
                    )
                }
            }
        }
    }
}

/**
 * Renders a specific input field based on its type (Text, Small Text, or Options).
 */
@Composable
private fun PromptInputField(
    field: PromptSegment.InputField,
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        when (field.type) {
            InputType.OPTIONS -> {
                PromptOptionsField(field, currentValue, onValueChange)
            }
            InputType.TEXT -> {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    label = { Text("Input Field") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
            InputType.SMALL_TEXT -> {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    label = { Text("Short Input") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }
        }
    }
}

/**
 * Renders a selector that looks like a transparent button and opens a wheel-style picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptOptionsField(
    field: PromptSegment.InputField,
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = "Select option",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDialog) {
        WheelPickerPopup(
            options = field.options,
            initialValue = currentValue,
            onDismiss = { showDialog = false },
            onValueChange = onValueChange
        )
    }
}

/**
 * A popup dialog containing an iOS-style wheel picker using VerticalPager for perfect centering.
 */
@Composable
private fun WheelPickerPopup(
    options: List<String>,
    initialValue: String,
    onDismiss: () -> Unit,
    onValueChange: (String) -> Unit
) {
    val itemHeight = 48.dp
    val initialIndex = options.indexOf(initialValue).coerceAtLeast(0)
    
    // PagerState handles the centering and snapping automatically
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { options.size })

    // Update the value when the pager settles on a new page
    LaunchedEffect(pagerState.currentPage) {
        onValueChange(options[pagerState.currentPage])
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(280.dp)
                .height(itemHeight * 3 + 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Selection Indicator (Fixed in the center)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {}

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    // This creates the "3 items visible" effect with the current one in the middle
                    contentPadding = PaddingValues(vertical = itemHeight),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { page ->
                    val isSelected = pagerState.currentPage == page
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clickable {
                                // If user clicks a neighbor, it will scroll to it and then we close
                                if (isSelected) onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = options[page],
                            style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InteractivePromptViewerPreview() {
    val examples = listOf(
        "Plain Text Example" to """
            Hello, my name is <INPUT type="smallText">John</INPUT> and I want to talk about <INPUT>Weather</INPUT>.
        """.trimIndent(),
        "Markdown Example" to """
            # Planning
            
            - **Objective**: <INPUT type="smallText">Finish project</INPUT>
            - *Priority*: <INPUT type="options" values="Low,Normal,Urgent">Normal</INPUT>
        """.trimIndent(),
        "JSON/XML Hybrid" to """
            {
              "api_key": "<INPUT type="smallText">XYZ-123</INPUT>",
              "config": <SETTING><INPUT type="options" values="ENABLED,DISABLED">ENABLED</INPUT></SETTING>
            }
        """.trimIndent()
    )

    // Using a map to track state for all examples in the preview
    var inputValues by remember { mutableStateOf(mapOf<String, String>()) }

    MamoutTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                examples.forEachIndexed { index, (title, promptText) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            InteractivePromptViewer(
                                promptText = promptText,
                                inputValues = inputValues,
                                onValueChange = { id, newValue ->
                                    inputValues = inputValues + (id to newValue)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
