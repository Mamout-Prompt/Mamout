package it.xyra.mamout.domain.usecase

import it.xyra.mamout.domain.model.FieldType
import it.xyra.mamout.domain.model.MatchKind
import it.xyra.mamout.domain.model.PromptMatch
import it.xyra.mamout.domain.parser.InputType
import it.xyra.mamout.domain.parser.MarkerTool
import it.xyra.mamout.domain.parser.PromptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test simulating the end-to-end templatization flow.
 */
class TemplatizeFlowTest {

    private val useCase = TemplatizePromptUseCase()

    @Test
    fun `full flow with mock LLM response produces correct segments`() {
        // 1. Initial user input
        val rawPrompt = "Hello: name."

        // 2. Stage 1: Preparation for LLM
        val preparedPrompt = useCase.preparePromptForLlm(rawPrompt)
        
        // MarkerTool logic for "Hello: name."
        // |0|Hello|1|:|2| name|3|.|4|
        assertTrue("Prepared prompt should contain markers", preparedPrompt.contains("|0|"))

        // 3. Stage 2: Mock LLM Response
        // Replace "name" which is between |2| and |3|
        val mockMatches = listOf(
            PromptMatch(
                startMarker = 2,
                endMarker = 3,
                matchKind = MatchKind.EDIT,
                originalText = " name",
                fieldType = FieldType.SMALL_TEXT
            )
        )

        // 4. Stage 3: Processing
        val parsedTemplate = useCase.processLlmResponse(rawPrompt, mockMatches)

        // 5. Final Verification
        val segments = parsedTemplate.segments
        
        val inputField = segments.filterIsInstance<PromptSegment.InputField>().firstOrNull()
        assertTrue("Should have found an input field", inputField != null)
        assertEquals(InputType.SMALL_TEXT, inputField?.type)
        // MarkerTool trims the default value during application
        assertEquals("name", inputField?.defaultValue)
    }

    @Test
    fun `MarkerTool correctly renders XML-like input tags`() {
        val rawPrompt = "Hello: name."
        val mockMatches = listOf(
            PromptMatch(
                startMarker = 2,
                endMarker = 3,
                matchKind = MatchKind.EDIT,
                originalText = " name",
                fieldType = FieldType.SMALL_TEXT
            )
        )

        // Directly testing the rendering part
        val renderedText = MarkerTool.applyMatches(rawPrompt, mockMatches)

        // MarkerTool trims the inner content but keeps surrounding spaces
        // Result should be "Hello: <INPUT type="smallText">name</INPUT>."
        assertTrue(
            "Rendered text should contain the INPUT tag with correct type",
            renderedText.contains("<INPUT type=\"smallText\">name</INPUT>")
        )
        assertEquals("Hello: <INPUT type=\"smallText\">name</INPUT>.", renderedText)
    }
}
