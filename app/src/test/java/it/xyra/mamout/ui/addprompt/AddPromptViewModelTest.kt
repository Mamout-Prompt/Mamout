package it.xyra.mamout.ui.addprompt

import androidx.compose.ui.text.input.TextFieldValue
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.domain.model.PromptSearchable
import it.xyra.mamout.domain.repository.PromptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddPromptViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AddPromptViewModel
    private lateinit var fakeRepository: FakePromptRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePromptRepository()
        viewModel = AddPromptViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is step 1`() {
        assertEquals(1, viewModel.uiState.value.step)
    }

    @Test
    fun `nextStep increments step`() {
        viewModel.onTitleChange("Test")
        viewModel.nextStep()
        assertEquals(2, viewModel.uiState.value.step)
    }

    @Test
    fun `savePrompt updates isSaved to true`() = runTest {
        viewModel.onTitleChange("My Prompt")
        viewModel.onDescriptionChange("My Desc")
        viewModel.onTemplateTextChange(TextFieldValue("My Template"))
        
        viewModel.savePrompt()
        runCurrent()
        
        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals("My Prompt", fakeRepository.savedTitle)
        assertEquals("My Template", fakeRepository.savedText)
    }

    @Test
    fun `prepareLlmPrompt generates a prompt with markers`() {
        viewModel.onTemplateTextChange(TextFieldValue("Hello name."))
        viewModel.prepareLlmPrompt()
        
        val llmPrompt = viewModel.uiState.value.llmPrompt
        assertTrue(llmPrompt != null)
        assertTrue(llmPrompt!!.contains("|0|Hello"))
    }

    private class FakePromptRepository : PromptRepository {
        var savedTitle: String? = null
        var savedText: String? = null
        var searchablePrompts: List<PromptSearchable> = emptyList()

        override fun getPrompts(): Flow<List<Prompt>> = flowOf(emptyList())
        override fun getSearchablePrompts(): Flow<List<PromptSearchable>> = flowOf(searchablePrompts)

        override suspend fun savePrompt(title: String, description: String, templateText: String) {
            savedTitle = title
            savedText = templateText
        }
    }
}
