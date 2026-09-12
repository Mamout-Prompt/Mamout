package it.xyra.mamout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.xyra.mamout.data.local.AppDatabase
import it.xyra.mamout.data.repository.PromptRepositoryImpl
import it.xyra.mamout.domain.usecase.SearchPromptsUseCase
import it.xyra.mamout.ui.addprompt.AddPromptScreen
import it.xyra.mamout.ui.addprompt.AddPromptViewModel
import it.xyra.mamout.ui.promptdetail.PromptDetailScreen
import it.xyra.mamout.ui.promptdetail.PromptDetailViewModel
import it.xyra.mamout.ui.promptlist.PromptListScreen
import it.xyra.mamout.ui.promptlist.PromptListViewModel
import it.xyra.mamout.ui.theme.MamoutTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = PromptRepositoryImpl(database.promptDao())
        val searchPromptsUseCase = SearchPromptsUseCase()

        setContent {
            MamoutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    SharedTransitionLayout {
                        NavHost(
                            navController = navController,
                            startDestination = "prompt_list",
                            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                        ) {
                            composable(
                                route = "prompt_list",
                                enterTransition = { EnterTransition.None },
                                exitTransition = { ExitTransition.None },
                                popEnterTransition = { EnterTransition.None },
                                popExitTransition = { ExitTransition.None }
                            ) {
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
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    onPromptClick = { promptId ->
                                        navController.navigate("prompt_detail/$promptId")
                                    },
                                    onAddPromptClick = { navController.navigate("add_prompt") },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            composable(
                                route = "add_prompt",
                                enterTransition = { EnterTransition.None },
                                exitTransition = { ExitTransition.None },
                                popEnterTransition = { EnterTransition.None },
                                popExitTransition = { ExitTransition.None }
                            ) {
                                val addPromptViewModel: AddPromptViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return AddPromptViewModel(repository) as T
                                        }
                                    }
                                )

                                AddPromptScreen(
                                    viewModel = addPromptViewModel,
                                    onBack = { navController.popBackStack() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            composable(
                                route = "prompt_detail/{promptId}",
                                arguments = listOf(navArgument("promptId") { type = NavType.LongType }),
                                enterTransition = { EnterTransition.None },
                                exitTransition = { ExitTransition.None },
                                popEnterTransition = { EnterTransition.None },
                                popExitTransition = { ExitTransition.None }
                            ) {
                                val detailViewModel: PromptDetailViewModel = viewModel(
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(
                                            modelClass: Class<T>,
                                            extras: CreationExtras
                                        ): T {
                                            val savedStateHandle = extras.createSavedStateHandle()
                                            return PromptDetailViewModel(
                                                savedStateHandle = savedStateHandle,
                                                repository = repository
                                            ) as T
                                        }
                                    }
                                )

                                PromptDetailScreen(
                                    viewModel = detailViewModel,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
