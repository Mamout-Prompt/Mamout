package it.xyra.mamout.ui.components.promptviewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.xyra.mamout.domain.parser.InputType
import it.xyra.mamout.domain.parser.ParsedPromptTemplate
import it.xyra.mamout.domain.parser.PromptSegment
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
 * Renders a dropdown menu for fields with predefined options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptOptionsField(
    field: PromptSegment.InputField,
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentValue,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Option") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            field.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
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
