package it.xyra.mamout

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import it.xyra.mamout.data.local.AppDatabase
import it.xyra.mamout.data.repository.PromptRepositoryImpl
import it.xyra.mamout.domain.usecase.SearchPromptsUseCase
import it.xyra.mamout.ui.promptlist.PromptListScreen
import it.xyra.mamout.ui.promptlist.PromptListViewModel
import it.xyra.mamout.ui.theme.MamoutTheme

/**
 * Main entry point activity for the application.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = PromptRepositoryImpl(database.promptDao())
        val searchPromptsUseCase = SearchPromptsUseCase()

        setContent {
            MamoutTheme {
                val viewModel: PromptListViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return PromptListViewModel(repository, searchPromptsUseCase) as T
                        }
                    }
                )

                PromptListScreen(
                    viewModel = viewModel,
                    onPromptClick = { promptId ->
                        Toast.makeText(this@MainActivity, "Prompt: $promptId", Toast.LENGTH_SHORT).show()
                    },
                    onAddPromptClick = {
                        Toast.makeText(this@MainActivity, "Add Prompt", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
