package it.xyra.mamout.domain.usecase

import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.model.PromptSearchable
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [SearchPromptsUseCase].
 */
class SearchPromptsUseCaseTest {

    private val searchPromptsUseCase = SearchPromptsUseCase()

    private val sampleItems = listOf(
        PromptSearchable(
            prompt = Prompt(1L, "Android Reviewer", "Code review helper"),
            templateText = "Review Kotlin code."
        ),
        PromptSearchable(
            prompt = Prompt(2L, "SQL Optimizer", "Database tuning"),
            templateText = "Optimize Room database queries."
        )
    )

    @Test
    fun invokeReturnsAllPromptsWhenQueryIsBelowMinLength() {
        val result = searchPromptsUseCase(sampleItems, "a")
        assertEquals(2, result.size)
    }

    @Test
    fun invokeFiltersPromptsByTitle() {
        val result = searchPromptsUseCase(sampleItems, "Android")
        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    @Test
    fun invokeFiltersPromptsByTemplateText() {
        val result = searchPromptsUseCase(sampleItems, "Room database")
        assertEquals(1, result.size)
        assertEquals(2L, result.first().id)
    }
}
