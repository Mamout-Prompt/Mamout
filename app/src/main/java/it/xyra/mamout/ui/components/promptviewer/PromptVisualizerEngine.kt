package it.xyra.mamout.ui.components.promptviewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Engine responsible for applying syntax highlighting and formatting to prompt text.
 *
 * It uses a pattern-based approach to detect and style JSON keys/strings, XML tags,
 * and Markdown elements within static text segments.
 */
object PromptVisualizerEngine {

    // Regex patterns for different formats
    private val jsonKeyRegex = Regex("""(?<=")([^"]+)(?=":\s*)""")
    private val stringRegex = Regex(""""([^"]*)"""")
    private val xmlTagRegex = Regex("""<(/?[a-zA-Z0-9]+)([^>]*)>""")
    private val markdownBoldRegex = Regex("""\*\*([^*]+)\*\*""")
    private val markdownItalicRegex = Regex("""\*([^*]+)\*""")

    /**
     * Styles for highlighting
     */
    private val jsonKeyStyle = SpanStyle(color = Color(0xFF9C27B0), fontWeight = FontWeight.Bold) // Purple
    private val stringStyle = SpanStyle(color = Color(0xFF4CAF50)) // Green
    private val xmlTagStyle = SpanStyle(color = Color(0xFF2196F3)) // Blue
    private val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
    private val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)

    /**
     * Transforms raw text into an [AnnotatedString] with applied styles.
     *
     * @param text The static text segment to process.
     * @return An [AnnotatedString] with syntax highlighting.
     */
    fun highlight(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            
            // 1. Highlight JSON Keys
            jsonKeyRegex.findAll(text).forEach { match ->
                addStyle(jsonKeyStyle, match.range.first, match.range.last + 1)
            }

            // 2. Highlight Strings (if not already styled as keys)
            stringRegex.findAll(text).forEach { match ->
                // Check if this range overlaps with a key style
                // For simplicity, we apply string style only to non-key matches or let it be overridden
                addStyle(stringStyle, match.range.first, match.range.last + 1)
            }

            // 3. Highlight XML Tags (excluding our own INPUT tags which are handled by the parser)
            xmlTagRegex.findAll(text).forEach { match ->
                val tagName = match.groups[1]?.value ?: ""
                if (tagName.uppercase() != "INPUT") {
                    addStyle(xmlTagStyle, match.range.first, match.range.last + 1)
                }
            }

            // 4. Markdown Bold
            markdownBoldRegex.findAll(text).forEach { match ->
                addStyle(boldStyle, match.range.first, match.range.last + 1)
            }

            // 5. Markdown Italic
            markdownItalicRegex.findAll(text).forEach { match ->
                addStyle(italicStyle, match.range.first, match.range.last + 1)
            }
        }
    }
}
