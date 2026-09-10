package it.xyra.mamout.ui.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.xyra.mamout.data.local.AppDatabase
import it.xyra.mamout.data.local.PromptContentEntity
import it.xyra.mamout.data.local.PromptEntity
import it.xyra.mamout.data.repository.PromptRepositoryImpl
import it.xyra.mamout.domain.usecase.SearchPromptsUseCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test verifying the full path from Room Database to Search UI.
 */
@RunWith(AndroidJUnit4::class)
class SearchIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: PromptRepositoryImpl
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        // 1. Initialize an in-memory database (isolated for this test)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        val dao = database.promptDao()
        repository = PromptRepositoryImpl(dao)
        
        // 2. Initialize the real ViewModel with the real Repository
        viewModel = SearchViewModel(repository, SearchPromptsUseCase())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun searchFlow_fromDatabaseToUI() {
        runBlocking {
            // 3. Insert real data into Room
            val dao = database.promptDao()
            val promptId = dao.insertPrompt(
                PromptEntity(title = "Work Email", description = "Professional template")
            )
            dao.insertContent(
                PromptContentEntity(promptId = promptId, templateText = "Dear team, ...")
            )
        }

        // 4. Set up the UI with the real ViewModel
        composeTestRule.setContent {
            SearchScreen(
                viewModel = viewModel,
                onBackClick = {},
                onPromptClick = {}
            )
        }

        // 5. Simulate user typing a query that should match the DB entry
        composeTestRule.onNodeWithText("Search prompts").performTextInput("Work")

        // 6. Verify that the UI displays the data coming from the Database
        composeTestRule.onNodeWithText("Work Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Professional template").assertIsDisplayed()
    }
    
    @Test
    fun searchFlow_noResults_whenDatabaseDoesNotMatch() {
        runBlocking {
            // Insert some data
            database.promptDao().insertPrompt(
                PromptEntity(title = "Recipe", description = "Cake instructions")
            )
        }

        composeTestRule.setContent {
            SearchScreen(
                viewModel = viewModel,
                onBackClick = {},
                onPromptClick = {}
            )
        }

        // Search for something non-existent
        composeTestRule.onNodeWithText("Search prompts").performTextInput("Car")

        // Verify empty state is shown instead of results
        composeTestRule.onNodeWithText("No results found").assertIsDisplayed()
    }
}
