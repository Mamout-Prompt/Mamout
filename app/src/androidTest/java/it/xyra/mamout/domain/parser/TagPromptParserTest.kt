package it.xyra.mamout.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [TagPromptParser].
 */
class TagPromptParserTest {

    /**
     * Verifies that [TagPromptParser.parse] correctly extracts static text chunks
     * and input fields with their corresponding metadata.
     */
    @Test
    fun parseCorrectlyExtractsStaticTextAndInputFields() {
        val template = """
            <SYSTEM>You are an expert in <INPUT type="smallText">Android</INPUT>.</SYSTEM>
            <TONE><INPUT type="options" values="Formal, Informal, Friendly">Formal</INPUT></TONE>
            <CONTEXT><INPUT>Write a detailed context here.</INPUT></CONTEXT>
        """.trimIndent()

        val result = TagPromptParser.parse(template)

        // Expecting 7 total segments: 4 static text chunks + 3 input fields
        assertEquals(7, result.segments.size)

        // Verify input_0 (SMALL_TEXT)
        val input0 = result.segments[1] as PromptSegment.InputField
        assertEquals("input_0", input0.id)
        assertEquals(InputType.SMALL_TEXT, input0.type)
        assertEquals("Android", input0.defaultValue)

        // Verify input_1 (OPTIONS)
        val input1 = result.segments[3] as PromptSegment.InputField
        assertEquals("input_1", input1.id)
        assertEquals(InputType.OPTIONS, input1.type)
        assertEquals("Formal", input1.defaultValue)
        assertEquals(listOf("Formal", "Informal", "Friendly"), input1.options)

        // Verify input_2 (default TEXT)
        val input2 = result.segments[5] as PromptSegment.InputField
        assertEquals("input_2", input2.id)
        assertEquals(InputType.TEXT, input2.type)
        assertEquals("Write a detailed context here.", input2.defaultValue)
    }

    /**
     * Verifies that [TagPromptParser.buildFinalPrompt] replaces input fields
     * with values provided in the input map.
     */
    @Test
    fun buildFinalPromptReplacesInputsCorrectlyWithUserValues() {
        val template = "<SYSTEM>Role: <INPUT type=\"smallText\">Developer</INPUT></SYSTEM>"
        val parsed = TagPromptParser.parse(template)

        val userInputs = mapOf("input_0" to "Senior Kotlin Engineer")
        val finalPrompt = TagPromptParser.buildFinalPrompt(parsed, userInputs)

        assertEquals("<SYSTEM>Role: Senior Kotlin Engineer</SYSTEM>", finalPrompt)
    }

    /**
     * Verifies that [TagPromptParser.buildFinalPrompt] uses the default value
     * defined in the tag when no user value is provided for a field.
     */
    @Test
    fun buildFinalPromptUsesDefaultValueWhenUserInputIsMissing() {
        val template = "<SYSTEM>Role: <INPUT type=\"smallText\">Developer</INPUT></SYSTEM>"
        val parsed = TagPromptParser.parse(template)

        val userInputs = emptyMap<String, String>()
        val finalPrompt = TagPromptParser.buildFinalPrompt(parsed, userInputs)

        assertEquals("<SYSTEM>Role: Developer</SYSTEM>", finalPrompt)
    }
}
