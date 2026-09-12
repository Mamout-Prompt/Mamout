package it.xyra.mamout

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import it.xyra.mamout.data.local.AppDatabase
import it.xyra.mamout.data.repository.PromptRepositoryImpl
import it.xyra.mamout.domain.usecase.SearchPromptsUseCase
import it.xyra.mamout.ui.addprompt.AddPromptScreen
import it.xyra.mamout.ui.addprompt.AddPromptViewModel
import it.xyra.mamout.ui.promptlist.PromptListScreen
import it.xyra.mamout.ui.promptlist.PromptListViewModel
import it.xyra.mamout.ui.theme.MamoutTheme

enum class Screen {
    List, Add
}

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
                var currentScreen by remember { mutableStateOf(Screen.List) }

                when (currentScreen) {
                    Screen.List -> {
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
                                currentScreen = Screen.Add
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Screen.Add -> {
                        val viewModel: AddPromptViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return AddPromptViewModel(repository) as T
                                }
                            }
                        )

                        AddPromptScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = Screen.List },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
