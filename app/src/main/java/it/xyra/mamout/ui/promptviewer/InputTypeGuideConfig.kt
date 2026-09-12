package it.xyra.mamout.ui.promptviewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.ui.graphics.vector.ImageVector
import it.xyra.mamout.domain.parser.InputType

/**
 * UI configuration for a single input type in the guide.
 */
data class InputTypeGuideData(
    val type: InputType,
    val icon: ImageVector,
    val description: String,
    val exampleCode: String
)

/**
 * Centralized configuration for all input types.
 *
 * Derived from [InputType.entries] with an exhaustive `when`: adding a new
 * InputType forces the compiler to require an entry here too, preventing the
 * enum and this guide from drifting out of sync.
 */
val inputTypeGuideConfigs: List<InputTypeGuideData> = InputType.entries.map { type ->
    when (type) {
        InputType.TEXT -> InputTypeGuideData(
            type = type,
            icon = Icons.AutoMirrored.Filled.Notes,
            description = "Multiline text field, ideal for longer content such as descriptions, detailed instructions, or paragraphs.",
            exampleCode = "<INPUT type=\"text\">Your story...</INPUT>"
        )
        InputType.SMALL_TEXT -> InputTypeGuideData(
            type = type,
            icon = Icons.Default.ShortText,
            description = "Single-line text field, for short values like names, titles, or keywords.",
            exampleCode = "<INPUT type=\"smallText\">John</INPUT>"
        )
        InputType.OPTIONS -> InputTypeGuideData(
            type = type,
            icon = Icons.AutoMirrored.Filled.List,
            description = "Dropdown menu with predefined comma-separated values: the user picks instead of typing.",
            exampleCode = "<INPUT type=\"options\" values=\"Red,Blue,Green\">Blue</INPUT>"
        )
    }
}