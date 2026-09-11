package it.xyra.mamout.domain.parser

/**
 * Represents a segment of a parsed prompt template, which can be either static text or a dynamic input field.
 */
sealed class PromptSegment {

    /**
     * Immutable text surrounding input fields.
     *
     * @property text The raw static text content.
     */
    data class StaticText(
        val text: String
    ) : PromptSegment()

    /**
     * Dynamic input field extracted from an `<INPUT>` tag.
     *
     * @property id Unique identifier for this input field (e.g., "input_0").
     * @property type The UI control type required to render this field.
     * @property defaultValue Default fallback value specified inside the `<INPUT>` tag.
     * @property options Predefined choices when [type] is [InputType.OPTIONS].
     */
    data class InputField(
        val id: String,
        val type: InputType,
        val defaultValue: String,
        val options: List<String> = emptyList()
    ) : PromptSegment()
}

/**
 * Container holding the original raw template string and its parsed ordered segments.
 *
 * @property rawTemplate The original template string before parsing.
 * @property segments The ordered sequence of static text and input field segments.
 */
data class ParsedPromptTemplate(
    val rawTemplate: String,
    val segments: List<PromptSegment>
)

/**
 * Utility object responsible for parsing `<INPUT>` tags from prompt templates
 * and constructing final prompt strings with user-supplied values.
 */
object TagPromptParser {

    /** The name of the tag used for dynamic input fields. */
    const val INPUT_TAG = "INPUT"

    /**
     * Checks if a given tag name corresponds to an input field tag.
     */
    fun isInputTag(tagName: String): Boolean {
        return tagName.equals(INPUT_TAG, ignoreCase = true)
    }

    // Matches the full <INPUT ...>default_value</INPUT> tag structure
    private val inputTagRegex = Regex(
        """<$INPUT_TAG\b(?<attributes>[^>]*)>(?<default>.*?)</$INPUT_TAG>""",
        RegexOption.DOT_MATCHES_ALL
    )

    // Matches 'type' and 'values' attributes inside the <INPUT> tag
    private val typeAttributeRegex = Regex("""type="([^"]+)"""")
    private val valuesAttributeRegex = Regex("""values="([^"]+)"""")

    /**
     * Parses a raw prompt template containing `<INPUT>` tags into structured segments.
     *
     * @param templateText The raw prompt template string to parse.
     * @return A [ParsedPromptTemplate] containing the ordered list of segments.
     */
    fun parse(templateText: String): ParsedPromptTemplate {
        val segments = mutableListOf<PromptSegment>()
        var lastIndex = 0

        inputTagRegex.findAll(templateText).forEachIndexed { index, matchResult ->
            // 1. Extract and append static text preceding the current <INPUT> tag
            if (matchResult.range.first > lastIndex) {
                val staticChunk = templateText.substring(lastIndex, matchResult.range.first)
                segments.add(PromptSegment.StaticText(staticChunk))
            }

            // 2. Extract attribute string and inner default content
            val attributesString = matchResult.groups["attributes"]?.value ?: ""
            val defaultValue = matchResult.groups["default"]?.value ?: ""

            // 3. Parse optional 'type' and 'values' attributes
            val typeStr = typeAttributeRegex.find(attributesString)?.groupValues?.get(1)
            val valuesStr = valuesAttributeRegex.find(attributesString)?.groupValues?.get(1)

            val inputType = InputType.fromKey(typeStr)

            val optionsList = if (inputType == InputType.OPTIONS && !valuesStr.isNullOrEmpty()) {
                valuesStr.split(",").map { it.trim() }
            } else {
                emptyList()
            }

            // 4. Create and append the InputField segment
            segments.add(
                PromptSegment.InputField(
                    id = "input_$index",
                    type = inputType,
                    defaultValue = defaultValue,
                    options = optionsList
                )
            )

            lastIndex = matchResult.range.last + 1
        }

        // 5. Append trailing static text after the final <INPUT> tag if present
        if (lastIndex < templateText.length) {
            segments.add(PromptSegment.StaticText(templateText.substring(lastIndex)))
        }

        return ParsedPromptTemplate(
            rawTemplate = templateText,
            segments = segments
        )
    }

    /**
     * Reconstructs the final prompt string by replacing input field placeholders
     * with user-provided inputs, falling back to default values when missing.
     *
     * @param parsed The parsed template containing ordered segments.
     * @param userInputs Map of input field IDs to user-supplied string values.
     * @return The compiled final prompt string.
     */
    fun buildFinalPrompt(
        parsed: ParsedPromptTemplate,
        userInputs: Map<String, String>
    ): String {
        return parsed.segments.joinToString("") { segment ->
            when (segment) {
                is PromptSegment.StaticText -> segment.text
                is PromptSegment.InputField -> userInputs[segment.id] ?: segment.defaultValue
            }
        }
    }
}
