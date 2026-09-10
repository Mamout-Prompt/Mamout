package it.xyra.mamout.ui.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.xyra.mamout.domain.model.Prompt
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented test for the Search Screen.
 *
 * This test runs in isolation on a device or emulator, verifying that the
 * UI components behave correctly when provided with data.
 */
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun searchScreen_displaysResults_whenDataIsProvided() {
        // Given a list of mock prompts
        val mockResults = listOf(
            Prompt(1, "Test Title", "Test Description")
        )

        // When the screen is rendered in the test environment
        composeTestRule.setContent {
            SearchContent(
                query = "Test",
                results = mockResults,
                onQueryChange = {},
                onBackClick = {},
                onPromptClick = {}
            )
        }

        // Then the title of the mock prompt should be visible
        composeTestRule.onNodeWithText("Test Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Description").assertIsDisplayed()
    }

    @Test
    fun searchScreen_displaysEmptyState_whenNoResultsFound() {
        // When the screen is rendered with an empty result list and a query
        composeTestRule.setContent {
            SearchContent(
                query = "Unknown",
                results = emptyList(),
                onQueryChange = {},
                onBackClick = {},
                onPromptClick = {}
            )
        }

        // Then the empty state message should be visible
        composeTestRule.onNodeWithText("No results found").assertIsDisplayed()
    }
}
