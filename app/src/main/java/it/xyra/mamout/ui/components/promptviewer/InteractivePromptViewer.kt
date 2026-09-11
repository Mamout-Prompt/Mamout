package it.xyra.mamout.ui.components.promptviewer

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
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
 * @param targetedInputIndex The index of the input field that should be scrolled into view/focused.
 * @param modifier The modifier to be applied to the root container.
 */
@Composable
fun InteractivePromptViewer(
    promptText: String,
    inputValues: Map<String, String>,
    onValueChange: (id: String, newValue: String) -> Unit,
    targetedInputIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    val parsedTemplate = remember(promptText) { TagPromptParser.parse(promptText) }

    InteractivePromptViewer(
        template = parsedTemplate,
        inputValues = inputValues,
        onValueChange = onValueChange,
        targetedInputIndex = targetedInputIndex,
        modifier = modifier
    )
}

/**
 * A shared stateless component that renders a prompt template with interactive input fields.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InteractivePromptViewer(
    template: ParsedPromptTemplate,
    inputValues: Map<String, String>,
    onValueChange: (id: String, newValue: String) -> Unit,
    targetedInputIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    var inputCounter = 0

    // JSON-block context is a fact about the *whole* template's brace balance, so it
    // must be computed once across all StaticText segments in order — not per
    // segment in isolation — otherwise a `{` opened before an <INPUT> field would be
    // invisible to the fragment that follows it. See PromptVisualizerEngine docs.
    val jsonBlockContext = remember(template) {
        PromptVisualizerEngine.computeJsonBlockContext(
            template.segments.filterIsInstance<PromptSegment.StaticText>().map { it.text }
        )
    }
    var staticTextIndex = 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        template.segments.forEach { segment ->
            when (segment) {
                is PromptSegment.StaticText -> {
                    if (segment.text.isNotBlank()) {
                        val forceJsonContext = jsonBlockContext.getOrElse(staticTextIndex) { false }
                        Text(
                            text = remember(segment.text, forceJsonContext) {
                                PromptVisualizerEngine.highlight(segment.text, forceJsonContext)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    staticTextIndex++
                }
                is PromptSegment.InputField -> {
                    val currentIndex = inputCounter++
                    val isTargeted = currentIndex == targetedInputIndex
                    val bringIntoViewRequester = remember { BringIntoViewRequester() }
                    val focusRequester = remember { FocusRequester() }

                    LaunchedEffect(isTargeted) {
                        if (isTargeted) {
                            // Request focus to show IME if needed
                            focusRequester.requestFocus()

                            // Bring into view with "comfort" padding (e.g. 200px above and below)
                            // This effectively centers the item if the container is large enough
                            bringIntoViewRequester.bringIntoView(
                                rect = Rect(
                                    left = 0f,
                                    top = -400f,
                                    right = 0f,
                                    bottom = 800f
                                )
                            )
                        }
                    }

                    PromptInputField(
                        field = segment,
                        currentValue = inputValues[segment.id] ?: segment.defaultValue,
                        onValueChange = { newValue -> onValueChange(segment.id, newValue) },
                        modifier = Modifier
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .focusRequester(focusRequester)
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
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
 * A popup dialog containing an iOS-style wheel picker.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPickerPopup(
    options: List<String>,
    initialValue: String,
    onDismiss: () -> Unit,
    onValueChange: (String) -> Unit
) {
    val itemHeight = 48.dp
    val visibleItems = 3
    val initialIndex = options.indexOf(initialValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snappingLayout = rememberSnapFlingBehavior(lazyListState = listState)

    // Rather than deriving the centered item from firstVisibleItemIndex/ScrollOffset
    // and contentPadding (whose exact interaction is easy to get subtly wrong — this
    // file's previous version had exactly that bug), we ask the layout directly: once
    // scrolling has settled, layoutInfo.visibleItemsInfo gives each visible item's
    // actual on-screen bounds, so we can find whichever item's center is closest to
    // the viewport's center. This is correct regardless of padding/offset conventions.
    val viewportCenterIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter =
                (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
            }?.index ?: initialIndex
        }
    }

    // Fire onValueChange once scrolling settles, using the same index that drives
    // the visual highlight below — the two can never disagree with each other.
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && viewportCenterIndex in options.indices) {
            onValueChange(options[viewportCenterIndex])
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(280.dp)
                .height(itemHeight * visibleItems + 32.dp) // Height for 3 items + padding
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Selection Indicator (the "center" highlight)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {}

                LazyColumn(
                    state = listState,
                    flingBehavior = snappingLayout,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = itemHeight), // One item height padding top/bottom to center first/last
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(options.size) { index ->
                        val isSelected = viewportCenterIndex == index
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .clickable {
                                    // Clicking an item centers it and closes the popup
                                    onValueChange(options[index])
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = options[index],
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
}

@Preview(showBackground = true)
@Composable
fun InteractivePromptViewerPreview() {
    val examples = listOf(
        "Plain Text Example" to ParsedPromptTemplate(
            "", listOf(
                PromptSegment.StaticText("Hello, my name is "),
                PromptSegment.InputField("p1", InputType.SMALL_TEXT, "John"),
                PromptSegment.StaticText(" and I want to talk about "),
                PromptSegment.InputField("p2", InputType.TEXT, "Weather")
            )
        ),
        "Markdown Example" to ParsedPromptTemplate(
            "", listOf(
                PromptSegment.StaticText("# Planning\n\n- **Objective**: "),
                PromptSegment.InputField("m1", InputType.SMALL_TEXT, "Finish project"),
                PromptSegment.StaticText("\n- *Priority*: "),
                PromptSegment.InputField("m2", InputType.OPTIONS, "Normal", listOf("Low", "Normal", "Urgent"))
            )
        ),
        "JSON/XML Hybrid" to ParsedPromptTemplate(
            "", listOf(
                PromptSegment.StaticText("{\n  \"api_key\": \""),
                PromptSegment.InputField("j1", InputType.SMALL_TEXT, "XYZ-123"),
                PromptSegment.StaticText("\",\n  \"config\": <SETTING>"),
                PromptSegment.InputField("j2", InputType.OPTIONS, "ENABLED", listOf("ENABLED", "DISABLED")),
                PromptSegment.StaticText("</SETTING>\n}")
            )
        )
    )

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
                examples.forEach { (title, template) ->
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
                                template = template,
                                inputValues = emptyMap(),
                                onValueChange = { _, _ -> }
                            )
                        }
                    }
                }
            }
        }
    }
}