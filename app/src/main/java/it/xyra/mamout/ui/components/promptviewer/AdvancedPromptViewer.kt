package it.xyra.mamout.ui.components.promptviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.xyra.mamout.domain.parser.InputType
import it.xyra.mamout.domain.parser.TagPromptParser
import it.xyra.mamout.ui.theme.MamoutTheme

private val VALID_INPUT_REGEX = Regex(
    "<INPUT\\s+[^>]*type=\"(${InputType.allKeys.joinToString("|")})\"[^>]*>(?:(?!<INPUT).)*?</INPUT>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)

/**
 * A wrapper for [InteractivePromptViewer] that adds:
 * - A toggle between Raw (editable) and Preview (interactive) modes.
 * - An info button to display the InputTypeGuide (visible only in Raw Mode).
 * - A floating navigation arrow to jump between input fields.
 * - Syntax highlighting for <INPUT> tags in raw mode.
 */
@Composable
fun AdvancedPromptViewer(
    templateTextValue: TextFieldValue,
    onTemplateTextValueChange: (TextFieldValue) -> Unit,
    isRawMode: Boolean,
    onToggleRawMode: () -> Unit,
    onNextInput: () -> Unit,
    onPreviousInput: () -> Unit,
    inputValues: Map<String, String>,
    onInputValueChange: (id: String, newValue: String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val matches = remember(templateTextValue.text) { VALID_INPUT_REGEX.findAll(templateTextValue.text).toList() }
    val inputTagCount = matches.size

    var isNavigatingUp by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }

    val lastIndex = remember(matches) { matches.lastOrNull()?.range?.first ?: -1 }
    val firstIndex = remember(matches) { matches.firstOrNull()?.range?.first ?: -1 }

    LaunchedEffect(templateTextValue.selection.start, lastIndex, firstIndex) {
        if (lastIndex != -1 && templateTextValue.selection.start >= lastIndex) {
            isNavigatingUp = true
        } else if (firstIndex != -1 && templateTextValue.selection.start <= firstIndex) {
            isNavigatingUp = false
        }
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isNavigatingUp) 180f else 0f,
        label = "ArrowRotation"
    )

    val targetedInputIndex = remember(templateTextValue.selection, matches) {
        matches.indexOfFirst { match ->
            templateTextValue.selection.start >= match.range.first &&
                    templateTextValue.selection.end <= match.range.last + 1
        }
    }

    val shouldShowArrow = remember(inputTagCount, isNavigatingUp, templateTextValue.selection, matches) {
        if (inputTagCount == 0) return@remember false
        if (inputTagCount == 1) {
            val tagRange = matches.firstOrNull()?.range
            tagRange == null || !(templateTextValue.selection.start >= tagRange.first &&
                    templateTextValue.selection.end <= tagRange.last + 1)
        } else {
            true
        }
    }

    // Input guide dialog
    if (showGuideDialog) {
        InputTypeGuide(
            onDismiss = { showGuideDialog = false }
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header with controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleRawMode) {
                    Icon(
                        imageVector = if (isRawMode) Icons.Default.Visibility else Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRawMode) "View Preview" else "Edit Raw")
                }

                // The Info button appears ONLY in Raw mode
                if (isRawMode) {
                    InputTypeGuideButton(
                        onClick = { showGuideDialog = true }
                    )
                }
            }

            // Content area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isRawMode) {
                    RawModeEditor(
                        value = templateTextValue,
                        onValueChange = onTemplateTextValueChange,
                        inputMatches = matches,
                        enabled = enabled,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        InteractivePromptViewer(
                            promptText = templateTextValue.text,
                            inputValues = inputValues,
                            onValueChange = onInputValueChange,
                            targetedInputIndex = targetedInputIndex,
                            enabled = enabled
                        )
                    }
                }

                // Navigation arrow
                if (shouldShowArrow) {
                    SmallFloatingActionButton(
                        onClick = {
                            if (isNavigatingUp) onPreviousInput() else onNextInput()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Navigate",
                            modifier = Modifier.rotate(arrowRotation)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RawModeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    inputMatches: List<MatchResult>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val showSuggestions = remember(value.text, value.selection) {
        val beforeCursor = value.text.take(value.selection.start)
        beforeCursor.endsWith("<INP", ignoreCase = true)
    }

    LaunchedEffect(value.selection, textLayoutResult) {
        if (!value.selection.collapsed) {
            textLayoutResult?.let { layout ->
                try {
                    val layoutLength = layout.layoutInput.text.length
                    val startIndex = value.selection.start.coerceIn(0, (layoutLength - 1).coerceAtLeast(0))
                    val endIndex = (value.selection.end - 1).coerceIn(0, (layoutLength - 1).coerceAtLeast(0))

                    val startRect = layout.getBoundingBox(startIndex)
                    val endRect = layout.getBoundingBox(endIndex)

                    bringIntoViewRequester.bringIntoView(
                        rect = Rect(
                            left = startRect.left,
                            top = startRect.top - 500f,
                            right = endRect.right,
                            bottom = endRect.bottom + 500f
                        )
                    )
                } catch (_: Exception) { }
            }
        }
    }

    val visualTransformation = remember(value.selection, inputMatches) {
        VisualTransformation { text ->
            TransformedText(
                text = highlightInputTags(text.text, value.selection, inputMatches),
                offsetMapping = OffsetMapping.Identity
            )
        }
    }

    // Shared insertion: used by chips below
    fun insertInputType(type: InputType) {
        val insertion = when (type) {
            InputType.OPTIONS -> "<INPUT type=\"options\" values=\"true,false\">true</INPUT>"
            else -> "<INPUT type=\"${type.key}\">default</INPUT>"
        }
        val beforeTag = value.text.take(value.selection.start).lowercase().lastIndexOf("<inp")
        if (beforeTag != -1) {
            val newText = value.text.take(beforeTag) + insertion + value.text.drop(value.selection.start)

            val contentStart = beforeTag + insertion.indexOf(">") + 1
            val contentEnd = beforeTag + insertion.lastIndexOf("</")

            onValueChange(
                value.copy(
                    text = newText,
                    selection = TextRange(contentStart, contentEnd)
                )
            )
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .bringIntoViewRequester(bringIntoViewRequester),
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                onTextLayout = { textLayoutResult = it },
                visualTransformation = visualTransformation,
                decorationBox = { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = value.text,
                        innerTextField = innerTextField,
                        enabled = enabled,
                        singleLine = false,
                        visualTransformation = visualTransformation,
                        interactionSource = remember { MutableInteractionSource() },
                        placeholder = { Text("Use <INPUT type=\"...\">default</INPUT> for dynamic fields") },
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = enabled,
                                isError = false,
                                interactionSource = remember { MutableInteractionSource() },
                                colors = OutlinedTextFieldDefaults.colors(),
                                shape = OutlinedTextFieldDefaults.shape
                            )
                        }
                    )
                }
            )
        }

        // Horizontal chip bar, anchored above the keyboard
        AnimatedVisibility(visible = showSuggestions) {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InputType.entries.forEach { type ->
                        val icon = when (type) {
                            InputType.TEXT -> Icons.AutoMirrored.Filled.Notes
                            InputType.SMALL_TEXT -> Icons.Default.Title
                            InputType.OPTIONS -> Icons.AutoMirrored.Filled.List
                        }
                        val label = when (type) {
                            InputType.TEXT -> "Long text"
                            InputType.SMALL_TEXT -> "Short text"
                            InputType.OPTIONS -> "Options"
                        }

                        AssistChip(
                            onClick = { insertInputType(type) },
                            label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp) // min touch target
                        )
                    }
                }
            }
        }
    }
}

private fun highlightInputTags(text: String, selection: TextRange, matches: List<MatchResult>): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        matches.forEach { match ->
            val isTargeted = !selection.collapsed &&
                    selection.start >= match.range.first &&
                    selection.end <= match.range.last + 1

            if (isTargeted) {
                addStyle(
                    style = SpanStyle(
                        color = Color(0xFFE36209),
                        fontWeight = FontWeight.Bold,
                        background = Color(0xFFFFF5EC)
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            } else {
                addStyle(
                    style = SpanStyle(
                        color = Color.Gray.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdvancedPromptViewerPreview() {
    val template = """
        # Long Template Test
        
        This is a long template to test scrolling and navigation.
        
        First input is here: <INPUT type="smallText">John</INPUT>
        
        ${"\n".repeat(10)}
        
        Middle 1 input is way down here: <INPUT type="options" values="Red,Blue,Green">Blue</INPUT>
        
        ${"\n".repeat(10)}
        
        Final input is at the bottom: <INPUT type="text">Life story...</INPUT>
        
        End of template.
    """.trimIndent()

    var textValue by remember { mutableStateOf(TextFieldValue(template)) }
    var isRawMode by remember { mutableStateOf(true) }
    var inputValues by remember { mutableStateOf(mapOf<String, String>()) }

    val onNavigate = { direction: Int ->
        val text = textValue.text
        val matches = VALID_INPUT_REGEX.findAll(text).toList()

        if (matches.isNotEmpty()) {
            val nextMatch = if (direction > 0) {
                matches.find { it.range.first > textValue.selection.end } ?: matches.first()
            } else {
                matches.findLast { it.range.last < textValue.selection.start } ?: matches.last()
            }
            textValue = textValue.copy(selection = TextRange(nextMatch.range.first, nextMatch.range.last + 1))
        }
    }

    MamoutTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                AdvancedPromptViewer(
                    templateTextValue = textValue,
                    onTemplateTextValueChange = { textValue = it },
                    isRawMode = isRawMode,
                    onToggleRawMode = { isRawMode = !isRawMode },
                    onNextInput = { onNavigate(1) },
                    onPreviousInput = { onNavigate(-1) },
                    inputValues = inputValues,
                    onInputValueChange = { id, newValue ->
                        inputValues = inputValues + (id to newValue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                )
            }
        }
    }
}
