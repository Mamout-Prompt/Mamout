package it.xyra.mamout.ui.promptlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.xyra.mamout.ui.promptlist.components.HomeSearchBar
import it.xyra.mamout.ui.promptlist.components.PromptCard

/**
 * Displays the main screen containing the prompt list, search bar trigger, and creation button.
 *
 * @param viewModel The state holder for the prompt list screen.
 * @param onPromptClick Callback invoked when a prompt item is selected.
 * @param onSearchClick Callback invoked when the search bar is tapped.
 * @param onAddPromptClick Callback invoked when the add prompt button is tapped.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun PromptListScreen(
    viewModel: PromptListViewModel,
    onPromptClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onAddPromptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPromptClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add prompt"
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HomeSearchBar(
                onSearchClick = onSearchClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 12.dp)
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (val state = uiState) {
                    is PromptListUiState.Loading -> CircularProgressIndicator()
                    is PromptListUiState.Empty -> Text(
                        text = "No prompts saved",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    is PromptListUiState.Error -> Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    is PromptListUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 80.dp
                            )
                        ) {
                            items(
                                items = state.prompts,
                                key = { it.id }
                            ) { prompt ->
                                PromptCard(
                                    prompt = prompt,
                                    onPromptClick = onPromptClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
