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
 * Each call to [highlight] tokenizes its input in a single left-to-right sweep:
 * block-level Markdown (headers/quotes/hr/lists) is detected per line, then the
 * remaining inline content (bold/italic/code/links, XML tags, and JSON key/string
 * pairs) is tokenized in one pass. No second pass re-scans text that a prior pass
 * already rewrote, so overlapping matches (e.g. a quoted string inside an
 * already-styled code span) can't double-style or mis-style each other.
 *
 * IMPORTANT — scope of JSON highlighting: a template's static text can be split
 * into multiple [PromptSegment.StaticText] fragments by `<INPUT>` fields sitting in
 * the middle of a JSON string (e.g. `"api_key": <INPUT.../>` then `</INPUT>,`).
 * This function deliberately does NOT try to guess JSON *key-vs-value* state across
 * separate [highlight] calls: whether a string continuing into the next fragment is
 * a key or a value depends on what the user types into the input field —
 * information that doesn't exist yet at highlight time, so it can only ever be
 * guessed. What it WILL accept is [forceJsonContext]: a plain fact ("is this
 * fragment inside an open `{ }` block, yes or no") that the caller can compute by
 * scanning the whole template's brace balance once, without guessing anything
 * about field contents. Passing it lets a trailing `",` fragment after an
 * `<INPUT>` still be recognized as a JSON string even though, in isolation, it
 * doesn't look like a key/value line.
 */
object PromptVisualizerEngine {

    // --- Styles (GitHub-inspired) ---
    private val jsonKeyStyle = SpanStyle(color = Color(0xFF22863A), fontWeight = FontWeight.Bold)
    private val jsonStringStyle = SpanStyle(color = Color(0xFF032F62))
    private val xmlTagStyle = SpanStyle(color = Color(0xFFE36209))
    private val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
    private val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
    private val strikethroughStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
    private val codeStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = Color(0xFFF6F8FA),
        color = Color(0xFF24292E)
    )
    private val linkStyle = SpanStyle(color = Color(0xFF0366D6), textDecoration = TextDecoration.Underline)
    private val quoteStyle = SpanStyle(color = Color(0xFF6A737D), fontStyle = FontStyle.Italic)
    private val hrStyle = SpanStyle(color = Color.Gray.copy(alpha = 0.5f))

    // A line is treated as JSON context if it contains a quoted key followed by a
    // colon (e.g. "api_key": "..."), or if the fragment as a whole contains an open
    // brace with no matching close (a JSON object starting in this same fragment).
    // This is intentionally fragment-scoped — see the class doc for why it doesn't
    // extend across separate highlight() calls.
    private val jsonLineHintRegex = Regex(""""[^"]+"\s*:""")

    // Inline token regex: alternation ordered so the tokenizer can identify which
    // construct matched via named groups, then advance past it in one sweep.
    private val inlineTokenRegex = Regex(
        """(?<bolditalic>(\*\*\*|___)(.+?)\2)|""" +
                """(?<bold>(\*\*|__)(.+?)\5)|""" +
                """(?<italic>(\*|_)(.+?)\8)|""" +
                """(?<strike>(~~)(.+?)\11)|""" +
                """(?<code>(`{1,3})(.+?)\13)|""" +
                """(?<link>\[(?<linkLabel>.*?)\]\((?<linkUrl>.*?)\))|""" +
                """(?<xmltag><(?<tagName>/?[a-zA-Z][a-zA-Z0-9]*)([^>]*)>)|""" +
                """(?<jsonkey>"[^"]+"(?=\s*:))|""" +
                """(?<jsonstring>"[^"]*")"""
    )

    /**
     * Transforms raw text into an [AnnotatedString] with Markdown formatting and
     * syntax highlighting applied in a single left-to-right pass per line.
     *
     * @param forceJsonContext When true, every line in [text] is treated as JSON
     *   context even if it doesn't itself contain a `"key":` pattern — use this for
     *   a fragment that the caller has determined sits inside an open `{ }` block
     *   started in an earlier fragment of the same template (see class doc). Plain
     *   text and Markdown fragments should leave this false.
     */
    fun highlight(text: String, forceJsonContext: Boolean = false): AnnotatedString = buildAnnotatedString {
        val lines = text.split("\n")
        var inCodeBlock = false

        lines.forEachIndexed { index, line ->
            when {
                line.trim().startsWith("```") -> {
                    inCodeBlock = !inCodeBlock
                }
                inCodeBlock -> {
                    withStyle(codeStyle) { append(line) }
                }
                else -> renderLine(this, line, forceJsonContext)
            }
            if (index < lines.lastIndex) append("\n")
        }
    }

    /**
     * Handles block-level Markdown for a single line (headers, quotes, hr, lists),
     * then delegates the remaining inline content to [renderInline].
     */
    private fun renderLine(builder: AnnotatedString.Builder, line: String, forceJsonContext: Boolean) {
        val trimmed = line.trim()
        when {
            line.startsWith("#") -> {
                val level = line.takeWhile { it == '#' }.length.coerceAtMost(6)
                val content = line.drop(level).trim()
                builder.withStyle(headerStyle(level)) { renderInline(builder, content, forceJsonContext) }
            }
            line.startsWith(">") -> {
                val content = line.drop(1).trim()
                builder.withStyle(quoteStyle) { renderInline(builder, content, forceJsonContext) }
            }
            trimmed == "---" || (trimmed.startsWith("***") && trimmed.all { it == '*' } && trimmed.length >= 3) -> {
                builder.withStyle(hrStyle) { append("────────────────────────────────") }
            }
            trimmed.matches(Regex("""^[*+-]\s.*""")) -> {
                builder.append("  • ")
                renderInline(builder, trimmed.drop(2), forceJsonContext)
            }
            trimmed.matches(Regex("""^\d+\.\s.*""")) -> {
                val dotIndex = trimmed.indexOf('.')
                builder.append("  ${trimmed.take(dotIndex + 2)}")
                renderInline(builder, trimmed.drop(dotIndex + 2), forceJsonContext)
            }
            else -> renderInline(builder, line, forceJsonContext)
        }
    }

    /**
     * Single-pass inline tokenizer. Scans [text] left to right once; for each match
     * it appends any plain text before it, then appends the token's content with the
     * appropriate style already resolved — no second pass is run over the output.
     *
     * JSON key/string tokens only fire when the line looks like a JSON key/value
     * line ([jsonLineHintRegex]); otherwise quoted text is left as plain text to
     * avoid false positives on ordinary prose. A quote left unpaired at the end of
     * this text (e.g. because an `<INPUT>` field cut the fragment short) is styled
     * up to where the fragment ends, and nothing more is assumed about it.
     */
    private fun renderInline(builder: AnnotatedString.Builder, text: String, forceJsonContext: Boolean) {
        val looksLikeJsonLine = forceJsonContext || jsonLineHintRegex.containsMatchIn(text)
        var lastIndex = 0

        inlineTokenRegex.findAll(text).forEach { match ->
            builder.append(text.substring(lastIndex, match.range.first))

            when {
                match.groups["bolditalic"] != null -> {
                    val inner = match.groups["bolditalic"]!!.value.let { it.substring(3, it.length - 3) }
                    builder.withStyle(boldStyle.merge(italicStyle)) { append(inner) }
                }
                match.groups["bold"] != null -> {
                    val inner = match.groups["bold"]!!.value.let { it.substring(2, it.length - 2) }
                    builder.withStyle(boldStyle) { append(inner) }
                }
                match.groups["italic"] != null -> {
                    val inner = match.groups["italic"]!!.value.let { it.substring(1, it.length - 1) }
                    builder.withStyle(italicStyle) { append(inner) }
                }
                match.groups["strike"] != null -> {
                    val inner = match.groups["strike"]!!.value.let { it.substring(2, it.length - 2) }
                    builder.withStyle(strikethroughStyle) { append(inner) }
                }
                match.groups["code"] != null -> {
                    val raw = match.groups["code"]!!.value
                    val fenceLen = raw.takeWhile { it == '`' }.length
                    val inner = raw.substring(fenceLen, raw.length - fenceLen)
                    builder.withStyle(codeStyle) { append(inner) }
                }
                match.groups["link"] != null -> {
                    val label = match.groups["linkLabel"]?.value ?: match.groups["link"]!!.value
                    builder.withStyle(linkStyle) { append(label) }
                }
                match.groups["xmltag"] != null -> {
                    val tagName = match.groups["tagName"]?.value ?: ""
                    val raw = match.groups["xmltag"]!!.value
                    if (TagPromptParser.isInputTag(tagName)) {
                        builder.append(raw)
                    } else {
                        builder.withStyle(xmlTagStyle) { append(raw) }
                    }
                }
                match.groups["jsonkey"] != null && looksLikeJsonLine -> {
                    builder.withStyle(jsonKeyStyle) { append(match.groups["jsonkey"]!!.value) }
                }
                match.groups["jsonstring"] != null && looksLikeJsonLine -> {
                    builder.withStyle(jsonStringStyle) { append(match.groups["jsonstring"]!!.value) }
                }
                else -> {
                    // JSON-shaped token but the line didn't qualify as JSON context: keep as plain text.
                    builder.append(match.value)
                }
            }
            lastIndex = match.range.last + 1
        }
        builder.append(text.substring(lastIndex))
    }

    /**
     * Computes, for each [PromptSegment.StaticText] in a template, whether it sits
     * inside an open `{ }` block carried over from an earlier fragment — a plain,
     * verifiable fact about brace balance, not a guess about field contents.
     *
     * Callers rendering a full template (see [InteractivePromptViewer]) should call
     * this once per template and pass the corresponding entry as `forceJsonContext`
     * to [highlight] for each static-text segment, so a JSON object split apart by
     * `<INPUT>` fields still highlights consistently across all its fragments.
     *
     * @return a list the same length as [staticTexts], where entry *i* is true if
     *   that fragment starts already inside an unclosed `{`.
     */
    fun computeJsonBlockContext(staticTexts: List<String>): List<Boolean> {
        val result = ArrayList<Boolean>(staticTexts.size)
        var depth = 0
        for (fragment in staticTexts) {
            result.add(depth > 0)
            for (ch in fragment) {
                when (ch) {
                    '{' -> depth++
                    '}' -> if (depth > 0) depth--
                }
            }
        }
        return result
    }

    private fun headerStyle(level: Int): SpanStyle {
        val size = when (level) {
            1 -> 24.sp
            2 -> 20.sp
            3 -> 18.sp
            else -> 16.sp
        }
        return SpanStyle(fontWeight = FontWeight.Bold, fontSize = size)
    }
}