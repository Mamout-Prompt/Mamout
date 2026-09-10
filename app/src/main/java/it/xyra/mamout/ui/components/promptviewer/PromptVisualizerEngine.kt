package it.xyra.mamout.ui.components.promptviewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import it.xyra.mamout.domain.parser.TagPromptParser

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
    
    // Markdown Inline Patterns
    private val inlineRegex = Regex(
        """(\*\*\*|___)(.*?)\1|""" +      // Bold Italic
        """(\*\*|__)(.*?)\3|""" +         // Bold
        """(\*|_)(.*?)\5|""" +            // Italic
        """(~~)(.*?)\7|""" +              // Strikethrough
        """(`{1,3})(.*?)\9|""" +          // Inline Code
        """(\[(.*?)\]\((.*?)\))"""        // Link: [text](url)
    )

    /**
     * Styles for highlighting
     */
    private val jsonKeyStyle = SpanStyle(fontWeight = FontWeight.Bold) 
    private val stringStyle = SpanStyle(color = Color(0xFF388E3C)) // Darker, subtle green
    private val xmlTagStyle = SpanStyle(color = Color(0xFF1976D2)) // Professional blue
    
    private val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
    private val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
    private val strikethroughStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
    private val codeStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = Color(0xFFF5F5F5),
        color = Color.Unspecified
    )
    private val linkStyle = SpanStyle(color = Color(0xFF1565C0), textDecoration = TextDecoration.Underline)
    private val quoteStyle = SpanStyle(color = Color.Gray, fontStyle = FontStyle.Italic)

    /**
     * Transforms raw text into an [AnnotatedString] with applied styles.
     * It parses Markdown elements (removing markers) and applies syntax highlighting.
     *
     * @param text The static text segment to process.
     * @return An [AnnotatedString] with syntax highlighting and Markdown formatting.
     */
    fun highlight(text: String): AnnotatedString {
        val markdownAnnotated = buildAnnotatedString {
            val lines = text.split("\n")
            var inCodeBlock = false
            
            lines.forEachIndexed { index, line ->
                if (line.trim().startsWith("```")) {
                    inCodeBlock = !inCodeBlock
                } else {
                    if (inCodeBlock) {
                        withStyle(codeStyle) {
                            append(line)
                        }
                    } else {
                        renderLine(this, line)
                    }
                    if (index < lines.lastIndex) append("\n")
                }
            }
        }

        // Apply Syntax Highlighting on top of the rendered Markdown text
        return buildAnnotatedString {
            append(markdownAnnotated)
            applySyntaxHighlighting(this, markdownAnnotated.text)
        }
    }

    /**
     * Processes a single line for block-level Markdown (headers, quotes, lists).
     */
    private fun renderLine(builder: AnnotatedString.Builder, line: String) {
        when {
            line.startsWith("#") -> {
                val level = line.takeWhile { it == '#' }.length.coerceAtMost(6)
                val content = line.drop(level).trim()
                builder.withStyle(getHeaderStyle(level)) {
                    append(content)
                }
            }
            line.startsWith(">") -> {
                val content = line.drop(1).trim()
                builder.withStyle(quoteStyle) {
                    append(content)
                }
            }
            line.startsWith("---") || (line.startsWith("***") && line.length >= 3 && !line.contains(" ")) -> {
                builder.withStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.5f))) {
                    append("────────────────────────────────")
                }
            }
            // List item (Bullet)
            line.trim().matches(Regex("""^[*+-]\s.*""")) -> {
                builder.append("  • ")
                renderInline(builder, line.trim().drop(2))
            }
            // List item (Numbered)
            line.trim().matches(Regex("""^\d+\.\s.*""")) -> {
                val dotIndex = line.trim().indexOf('.')
                builder.append("  ${line.trim().take(dotIndex + 2)}")
                renderInline(builder, line.trim().drop(dotIndex + 2))
            }
            else -> {
                renderInline(builder, line)
            }
        }
    }

    /**
     * Processes inline Markdown elements (bold, italic, code, links) and removes markers.
     */
    private fun renderInline(builder: AnnotatedString.Builder, text: String) {
        var lastIndex = 0
        inlineRegex.findAll(text).forEach { match ->
            // Append plain text before match
            builder.append(text.substring(lastIndex, match.range.first))
            
            // Process based on which group matched
            when {
                match.groups[1] != null -> { // Bold Italic
                    builder.withStyle(boldStyle.merge(italicStyle)) { append(match.groups[2]!!.value) }
                }
                match.groups[3] != null -> { // Bold
                    builder.withStyle(boldStyle) { append(match.groups[4]!!.value) }
                }
                match.groups[5] != null -> { // Italic
                    builder.withStyle(italicStyle) { append(match.groups[6]!!.value) }
                }
                match.groups[7] != null -> { // Strikethrough
                    builder.withStyle(strikethroughStyle) { append(match.groups[8]!!.value) }
                }
                match.groups[9] != null -> { // Inline Code
                    builder.withStyle(codeStyle) { append(match.groups[10]!!.value) }
                }
                match.groups[11] != null -> { // Link
                    builder.withStyle(linkStyle) { append(match.groups[12]!!.value) }
                }
            }
            lastIndex = match.range.last + 1
        }
        builder.append(text.substring(lastIndex))
    }

    /**
     * Returns a style for headers based on their level (1-6).
     */
    private fun getHeaderStyle(level: Int): SpanStyle {
        val size = when (level) {
            1 -> 22.sp
            2 -> 20.sp
            3 -> 18.sp
            else -> 16.sp
        }
        return SpanStyle(fontWeight = FontWeight.Bold, fontSize = size)
    }

    /**
     * Applies syntax highlighting (JSON, XML) to the final text.
     */
    private fun applySyntaxHighlighting(builder: AnnotatedString.Builder, text: String) {
        // 1. Highlight JSON Keys
        jsonKeyRegex.findAll(text).forEach { match ->
            builder.addStyle(jsonKeyStyle, match.range.first, match.range.last + 1)
        }

        // 2. Highlight Strings
        stringRegex.findAll(text).forEach { match ->
            builder.addStyle(stringStyle, match.range.first, match.range.last + 1)
        }

        // 3. Highlight XML Tags
        xmlTagRegex.findAll(text).forEach { match ->
            val tagName = match.groups[1]?.value ?: ""
            if (!TagPromptParser.isInputTag(tagName)) {
                builder.addStyle(xmlTagStyle, match.range.first, match.range.last + 1)
            }
        }
    }
}
