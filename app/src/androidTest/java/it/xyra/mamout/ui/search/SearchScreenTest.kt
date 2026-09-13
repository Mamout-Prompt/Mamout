package it.xyra.mamout.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
@OptIn(ExperimentalSharedTransitionApi::class)
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
            SharedTransitionLayout {
                AnimatedVisibility(visible = true) {
                    SearchContent(
                        uiState = SearchUiState(query = "Test", results = mockResults),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility,
                        onQueryChange = {},
                        onBackClick = {},
                        onPromptClick = {},
                        onPromptLongClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Then the title of the mock prompt should be visible
        composeTestRule.onNodeWithText("Test Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Description").assertIsDisplayed()
    }

    @Test
    fun searchScreen_displaysEmptyState_whenNoResultsFound() {
        // When the screen is rendered with an empty result list and a query
        composeTestRule.setContent {
            SharedTransitionLayout {
                AnimatedVisibility(visible = true) {
                    SearchContent(
                        uiState = SearchUiState(query = "Unknown", results = emptyList()),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility,
                        onQueryChange = {},
                        onBackClick = {},
                        onPromptClick = {},
                        onPromptLongClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Then the empty state message should be visible
        composeTestRule.onNodeWithText("No results found").assertIsDisplayed()
    }
}
