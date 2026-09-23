package it.xyra.mamout.ui.search

import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.model.PromptSearchable
import it.xyra.mamout.domain.repository.PromptRepository
import it.xyra.mamout.domain.usecase.SearchPromptsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SearchViewModel] verifying logical flow from repository to UI state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePromptRepository
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePromptRepository()
        viewModel = SearchViewModel(fakeRepository, SearchPromptsUseCase())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when query changes, UI state is updated with filtered results`() = runTest {
        // Given a repository with initial data
        val prompts = listOf(
            Prompt(1, "Apple", "Red fruit", 0L),
            Prompt(2, "Banana", "Yellow fruit", 0L)
        )
        fakeRepository.emit(prompts.map { PromptSearchable(it, "") })
        
        // Wait for initial emission
        advanceUntilIdle()

        // When searching for "Apple"
        viewModel.onQueryChanged("Apple")
        advanceUntilIdle()

        // Then results should contain only Apple
        val state = viewModel.uiState.value
        assertEquals("Apple", state.query)
        assertEquals(1, state.results.size)
        assertEquals("Apple", state.results[0].title)
    }

    @Test
    fun `when query is empty, UI state shows all results`() = runTest {
        val prompts = listOf(
            Prompt(1, "Apple", "Red fruit", 0L),
            Prompt(2, "Banana", "Yellow fruit", 0L)
        )
        fakeRepository.emit(prompts.map { PromptSearchable(it, "") })
        advanceUntilIdle()

        viewModel.onQueryChanged("")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.results.size)
    }

    /**
     * Minimal fake repository for testing search flow.
     */
    private class FakePromptRepository : PromptRepository {
        private val _searchablePrompts = MutableStateFlow<List<PromptSearchable>>(emptyList())

        suspend fun emit(items: List<PromptSearchable>) {
            _searchablePrompts.emit(items)
        }

        override fun getPrompts(): Flow<List<Prompt>> {
            throw NotImplementedError()
        }

        override fun getSearchablePrompts(): Flow<List<PromptSearchable>> = _searchablePrompts

        override fun getPromptById(promptId: Long): Flow<PromptSearchable?> = flowOf(null)

        override suspend fun updatePrompt(
            promptId: Long,
            title: String,
            description: String,
            templateText: String
        ) {
            // Not needed for SearchViewModel tests
        }

        override suspend fun deletePrompt(promptId: Long) {
            // Not needed for SearchViewModel tests
        }

        override suspend fun savePrompt(title: String, description: String, templateText: String) {
            // Not needed for SearchViewModel tests
        }

        override suspend fun getAllPromptsSync(): List<PromptSearchable> = emptyList()

        override suspend fun syncPrompts(prompts: List<PromptSearchable>) {
            // Not needed for SearchViewModel tests
        }
    }
}
