package it.xyra.mamout.domain.parser

import android.util.Log
import it.xyra.mamout.domain.model.FieldType
import it.xyra.mamout.domain.model.MatchKind
import it.xyra.mamout.domain.model.PromptMatch

object MarkerTool {

    private const val TAG = "MarkerTool"

    private val PUNCT_CHARS = setOf(':', '"', '\'', '[', ']', '{', '}', '(', ')', ',', ';')
    private val RUNNABLE_PUNCT = Regex("""\.{2,}|_{2,}|-{2,}|\.""")
    private val ANOMALOUS_GAP = Regex("""[ \t]{2,}""")
    private val WHITESPACE_RUN = Regex("""\s+""")

    data class MarkResult(
        val markedText: String,
        val markerOffsets: List<Int>
    )

    fun markText(text: String): MarkResult {
        val offsets = buildMarkerMap(text)
        val markedText = renderMarkedText(text, offsets)
        return MarkResult(markedText, offsets)
    }

    private fun buildMarkerMap(text: String): List<Int> {
        val boundaries = mutableSetOf(0, text.length)

        text.forEachIndexed { i, ch ->
            if (ch in PUNCT_CHARS) {
                boundaries.add(i)
                boundaries.add(i + 1)
            }
        }

        RUNNABLE_PUNCT.findAll(text).forEach { m ->
            boundaries.add(m.range.first)
            boundaries.add(m.range.last + 1)
        }

        ANOMALOUS_GAP.findAll(text).forEach { m ->
            if (m.range.first > 0 && m.range.last + 1 < text.length) {
                boundaries.add(m.range.first)
                boundaries.add(m.range.last + 1)
            }
        }

        val sorted = boundaries.sorted()
        if (sorted.isEmpty()) return emptyList()

        val merged = mutableListOf(sorted.first())
        for (i in 1 until sorted.size) {
            val pos = sorted[i]
            val prev = merged.last()
            val segment = text.substring(prev, pos)

            if (pos != prev && segment.trim().isEmpty()) {
                continue
            }
            merged.add(pos)
        }
        return merged
    }

    private fun renderMarkedText(text: String, markerOffsets: List<Int>): String {
        return buildString {
            val n = markerOffsets.size
            for (i in 0 until n) {
                append("|$i|")
                if (i + 1 < n) {
                    val start = markerOffsets[i]
                    val end = markerOffsets[i + 1]
                    val segment = text.substring(start, end)
                    val stripped = segment.trim()

                    if (stripped.isEmpty()) {
                        if (ANOMALOUS_GAP.matches(segment)) append(" ")
                    } else {
                        append(WHITESPACE_RUN.replace(stripped, " "))
                    }
                }
            }
        }
    }

    fun applyMatches(text: String, matches: List<PromptMatch>): String {
        val markerOffsets = buildMarkerMap(text)
        val nMarkers = markerOffsets.size

        fun offsetOf(idx: Int): Int? = if (idx in 0 until nMarkers) markerOffsets[idx] else null

        data class ResolvedMatch(
            val contentStart: Int,
            val contentEnd: Int,
            val placeholder: String
        )

        val resolved = mutableListOf<ResolvedMatch>()

        for (m in matches) {
            val startOff = offsetOf(m.startMarker)
            val endOff = offsetOf(m.endMarker)

            if (startOff == null || endOff == null) {
                Log.w(TAG, "Marker out of range, skipping match: $m")
                continue
            }
            if (startOff > endOff) {
                Log.w(TAG, "start_marker after end_marker, skipping match: $m")
                continue
            }

            val rawSpan = text.substring(startOff, endOff)
            val leftTrim = rawSpan.length - rawSpan.trimStart().length
            val rightTrim = rawSpan.length - rawSpan.trimEnd().length

            var contentStart = startOff + leftTrim
            var contentEnd = endOff - rightTrim

            if (contentStart > contentEnd) {
                contentStart = startOff
                contentEnd = startOff
            }

            val originalContent = text.substring(contentStart, contentEnd)

            if (m.matchKind == MatchKind.EDIT && originalContent.isEmpty()) {
                Log.w(TAG, "Match 'edit' between ${m.startMarker}-${m.endMarker} resolves to an empty span.")
            }

            val placeholder = buildInputTagPlaceholder(m, originalContent)
            resolved.add(ResolvedMatch(contentStart, contentEnd, placeholder))
        }

        resolved.sortBy { it.contentStart }
        val safeResolved = mutableListOf<ResolvedMatch>()
        for (i in resolved.indices) {
            if (i > 0 && resolved[i].contentStart < safeResolved.last().contentEnd) {
                Log.w(TAG, "Overlapping match ignored (offset ${resolved[i].contentStart})")
                continue
            }
            safeResolved.add(resolved[i])
        }

        safeResolved.sortByDescending { it.contentStart }
        var result = text

        for (r in safeResolved) {
            val safeStart = r.contentStart.coerceAtMost(result.length)
            val safeEnd = r.contentEnd.coerceAtMost(result.length)
            result = result.substring(0, safeStart) + r.placeholder + result.substring(safeEnd)
        }

        return result
    }

    private fun buildInputTagPlaceholder(match: PromptMatch, originalContent: String): String {
        val typeAttr = when (match.fieldType) {
            FieldType.TEXT -> "text"
            FieldType.SMALL_TEXT -> "smallText"
            FieldType.OPTIONS -> "options"
        }

        val attributes = StringBuilder("type=\"$typeAttr\"")

        if (match.fieldType == FieldType.OPTIONS && !match.values.isNullOrEmpty()) {
            val valuesStr = match.values.joinToString(",")
            attributes.append(" values=\"$valuesStr\"")
        }

        val defaultValue = if (match.matchKind == MatchKind.EDIT) {
            originalContent
        } else {
            ""
        }

        return "<INPUT $attributes>$defaultValue</INPUT>"
    }
}