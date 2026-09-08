package it.xyra.mamout.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PromptDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PromptDao

    @Before
    fun setup() {
        // Creates an in-memory temporary database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries() // Allows queries on the main test thread for simplicity
            .build()

        dao = database.promptDao()
    }

    @After
    fun teardown() {
        // Closes the database after each test
        database.close()
    }

    @Test
    fun insertPromptAndContent_retrievesDataCorrectly() = runBlocking {
        // 1. Insert parent prompt
        val prompt = PromptEntity(title = "Formal Email", description = "Test description")
        val promptId = dao.insertPrompt(prompt)

        // 2. Insert child content linked via promptId
        val content = PromptContentEntity(promptId = promptId, templateText = "Hello {{name}}")
        dao.insertContent(content)

        // 3. Verify prompt list retrieval (using .first() to collect the first item emitted by Flow)
        val promptsList = dao.getPrompts().first()
        assertEquals(1, promptsList.size)
        assertEquals("Formal Email", promptsList[0].title)

        // 4. Verify prompt content retrieval
        val retrievedContent = dao.getPromptContent(promptId).first()
        assertNotNull(retrievedContent)
        assertEquals("Hello {{name}}", retrievedContent?.templateText)
    }

    @Test
    fun deletePrompt_deletesAssociatedContentCascade() = runBlocking {
        // 1. Insert prompt and content
        val prompt = PromptEntity(title = "Prompt to delete", description = "Delete me")
        val promptId = dao.insertPrompt(prompt)

        val content = PromptContentEntity(promptId = promptId, templateText = "Temporary text")
        dao.insertContent(content)

        // 2. Delete parent prompt
        val insertedPrompt = dao.getPrompts().first()[0]
        dao.deletePrompt(insertedPrompt)

        // 3. Verify that the prompt list is now empty
        val promptsList = dao.getPrompts().first()
        assertEquals(0, promptsList.size)

        // 4. Verify that associated content was automatically deleted (CASCADE)
        val retrievedContent = dao.getPromptContent(promptId).first()
        assertNull(retrievedContent)
    }
}