package it.xyra.mamout.domain.usecase

import it.xyra.mamout.domain.model.MetaPromptResponse
import it.xyra.mamout.domain.model.PromptMatch
import it.xyra.mamout.domain.parser.MarkerTool
import it.xyra.mamout.domain.parser.ParsedPromptTemplate
import it.xyra.mamout.domain.parser.TagPromptParser
import kotlinx.serialization.json.Json

class TemplatizePromptUseCase {

    private val metaPromptTemplate = """
    <CONTEXT>
    You are an analysis module for a prompt management app. It lets users templatize prompts by isolating the parts that need subjective input. Instead of rewriting the whole prompt, return ONLY the coordinates of the points to change, in JSON, so the app — not you — applies the edits. This avoids truncation or unintended changes.
    </CONTEXT>
    
    <INPUT_FORMAT>
    The prompt you receive is pre-processed: the app has inserted numeric markers in the form |N| at every possible boundary in the text (after spaces, and after structural punctuation like : , " ' [ ] { } and line breaks).
    
    Each marker is a precise, unique position in the original text. The text between two consecutive markers (e.g. between |12| and |13|) is a token/fragment of the original text.
    
    - Markers are NOT part of the original text — they are position references added by the app.
    - Never invent, alter, or "correct" a marker; use only marker numbers you actually see in the input.
    - If a value to replace spans multiple tokens, use the marker right before the first token as start_marker and the marker right after the last token as end_marker.
    </INPUT_FORMAT>
    
    <OBJECTIVE>
    Identify every point that is a dynamic value or a field to fill in. For each, return: its position (start_marker, end_marker), the intervention type (insertion or edit), and the expected data type.
    </OBJECTIVE>
    
    <MATCH_KIND>
    - "insertion" → an actual empty spot in the text (placeholder char like \0, double space, missing word/preposition). No existing text to replace. start_marker and end_marker are the same number.
    - "edit" → text already present that must be fully replaced (e.g. "...", "___", "[insert here]", a literal like true, a number, a placeholder string). Put the existing text in original_text. start_marker = marker right before it, end_marker = marker right after it.
    </MATCH_KIND>
    
    <FIELD_TYPE>
    - "text" → expected value of 10+ words.
    - "smallText" → expected value under 10 words.
    - "options" → value from a finite set. If not explicit, infer plausible values and list them in "values".
    
    CRITICAL: placeholder length does NOT indicate expected content length. A short "..." may need a long answer if the prompt states so elsewhere (e.g. "write at least 5 sentences", "explain in detail", "a short title"). Always search the WHOLE prompt for such length cues first; they override the placeholder's own length. With no length cue, judge by semantics: names, dates, numbers, titles, single words → smallText; descriptions, explanations, narrative/argumentative content → text.
    
    Special case: a boolean literal (true/false) tied to a key name that reads as a configurable binary preference (e.g. "enabled", "active", "paid_books") → classify as "options", values ["true","false"], match_kind "edit", original_text = the literal shown (e.g. "true").
    </FIELD_TYPE>
    
    <OPTIONS_INFERENCE priority="HIGH">
    Be PROACTIVE about "options" even when alternatives aren't spelled out. Nouns that commonly imply a closed set (a tone/register, a language, an output format, an intensity/size level, a binary state, a priority tier, a day of week, and similar) are strong signals. When you spot one, classify as "options" and generate 3–6 plausible, mutually exclusive values yourself from context. Don't reserve "options" only for 100%-obvious cases — but don't force it onto clearly open-ended fields either (a proper name, a free description, an open topic).
    </OPTIONS_INFERENCE>
    
    <DETECTION_CRITERION priority="HIGH">
    Be parsimonious about WHETHER to include a point at all: when in doubt, exclude it — a false negative is better than a false positive, since the user can still edit the prompt manually.
    
    Include a point ONLY when it's clearly true that:
    - without that value the prompt is nonsensical, syntactically incomplete, or produces a generic/wrong output;
    - the segment is visibly a placeholder, empty field, or interrupted portion;
    - that data changes every time the prompt is reused.
    
    Do NOT include ambiguous segments, technical terminology, role/persona names, or wording that could be intentionally fixed.
    
    This parsimony applies ONLY to the inclusion decision. Once a point is included, be accurate and proactive (not minimalist) about field_type and options — defaulting everything to "smallText" out of laziness is as wrong as over-including points.
    </DETECTION_CRITERION>
    
    <MARKER_SELECTION_RULES>
    - start_marker/end_marker must be integers that actually appear in the input; never invent one.
    - Keep the marker span as tight as possible around the target — exclude surrounding spaces/quotes/punctuation unless they too must be replaced.
    - Adjacent distinct points must have non-overlapping marker spans.
    </MARKER_SELECTION_RULES>
    
    <OUTPUT_SCHEMA>
    Respond ONLY with this JSON, no text before or after:
    
    {
      "matches": [
        {
          "start_marker": 12,
          "end_marker": 13,
          "match_kind": "insertion" | "edit",
          "original_text": "exact text being replaced — include ONLY if match_kind is 'edit'",
          "field_type": "text" | "smallText" | "options",
          "values": ["v1", "v2", "v3"]
        }
      ]
    }

    Include "values" only when field_type is "options". Include "original_text" only when match_kind is "edit".
    </OUTPUT_SCHEMA>

    <EXAMPLES>
    1) Length inferred from context, not placeholder:
    Input: "Write a description of at least 5 sentences for the product: |40|...|41|, that is engaging."
    → {"start_marker": 40, "end_marker": 41, "match_kind": "edit", "original_text": "...", "field_type": "text"}

    2) Proactive "options" on a boolean:
    Input: "\"notifications_enabled\": |8|true|9|,"
    → {"start_marker": 8, "end_marker": 9, "match_kind": "edit", "original_text": "true", "field_type": "options", "values": ["true", "false"]}

    3) Insertion vs edit, same content type:
    Input: "The recipient is |15||15| and the document is about |22|...|23|"
    → point 1 (empty gap): {"start_marker": 15, "end_marker": 15, "match_kind": "insertion", "field_type": "smallText"}
    → point 2 (existing placeholder): {"start_marker": 22, "end_marker": 23, "match_kind": "edit", "original_text": "...", "field_type": ...}
    </EXAMPLES>

    ########

    {{marked_text}}

    ########
    """.trimIndent()

    fun preparePromptForLlm(originalPrompt: String): String {
        val (markedText, _) = MarkerTool.markText(originalPrompt)
        return metaPromptTemplate.replace("{{marked_text}}", markedText)
    }

    /**
     * Parses the LLM's JSON response and applies it to [originalPrompt].
     *
     * Chatbots asked to "return only JSON" often still wrap it in prose or a Markdown
     * code fence (e.g. "Sure! ```json\n{...}\n``` Let me know if..."). Rather than
     * failing on that extra text, [extractJsonObject] first isolates the outermost
     * `{...}` object before deserializing, so a straight copy-paste from a chat UI
     * still works.
     *
     * @throws SerializationException if no JSON object can be found or parsed.
     */
    fun templatize(originalPrompt: String, llmJsonResponse: String): ParsedPromptTemplate {
        val jsonObject = extractJsonObject(llmJsonResponse)
        val response = Json { ignoreUnknownKeys = true }.decodeFromString<MetaPromptResponse>(jsonObject)
        return processLlmResponse(originalPrompt, response.matches)
    }

    /**
     * Extracts the outermost `{...}` JSON object from [rawResponse], tolerating any
     * surrounding prose or Markdown code fences the LLM may have added despite being
     * asked for JSON only.
     *
     * Finds the first `{` and its matching closing `}` by tracking brace depth (and
     * skipping braces inside string literals, so `{"text": "e.g. {foo}"}` isn't cut
     * short), then returns that span verbatim for the JSON parser to validate. If no
     * balanced object is found, returns [rawResponse] unchanged so the subsequent
     * `Json.decodeFromString` call fails with its own descriptive error instead of a
     * silently different one from here.
     */
    private fun extractJsonObject(rawResponse: String): String {
        val start = rawResponse.indexOf('{')
        if (start == -1) return rawResponse

        var depth = 0
        var inString = false
        var isEscaped = false

        for (i in start until rawResponse.length) {
            val c = rawResponse[i]
            when {
                isEscaped -> isEscaped = false
                c == '\\' && inString -> isEscaped = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> depth++
                c == '}' -> {
                    depth--
                    if (depth == 0) return rawResponse.substring(start, i + 1)
                }
            }
        }

        return rawResponse
    }

    fun processLlmResponse(originalPrompt: String, matches: List<PromptMatch>): ParsedPromptTemplate {
        val promptWithInputTags = MarkerTool.applyMatches(originalPrompt, matches)
        return TagPromptParser.parse(promptWithInputTags)
    }
}