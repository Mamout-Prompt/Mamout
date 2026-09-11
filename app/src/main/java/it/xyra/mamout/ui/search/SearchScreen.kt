package it.xyra.mamout.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.ui.promptlist.components.DeleteConfirmationDialog
import it.xyra.mamout.ui.promptlist.components.PromptCard

/**
 * Stateful entry point for the standalone Search Screen.
 *
 * Observes state from [SearchViewModel] and renders search input, filtering results,
 * item selection, and deletion confirmation dialogs.
 *
 * @param viewModel The state holder for the search screen.
 * @param onBackClick Callback invoked when the user taps the top app bar navigation back button.
 * @param onPromptClick Callback invoked when a search result prompt is selected for viewing or editing.
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBackClick: () -> Unit,
    onPromptClick: (Prompt) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isDeleteDialogVisible) {
        DeleteConfirmationDialog(
            onConfirm = viewModel::onDeleteConfirmed,
            onDismiss = viewModel::onDeleteDialogDismissed
        )
    }

    SearchContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChanged,
        onBackClick = onBackClick,
        onPromptClick = { prompt ->
            if (uiState.selectedPromptId != null) {
                viewModel.onPromptClick(prompt)
            } else {
                onPromptClick(prompt)
            }
        },
        onPromptLongClick = viewModel::onPromptLongClick,
        onDeleteClick = viewModel::onDeleteRequested
    )
}

/**
 * Stateless content layout for the search screen.
 *
 * @param uiState Current UI state containing query text, search results, and selection states.
 * @param onQueryChange Callback invoked when the text input changes in the search field.
 * @param onBackClick Callback invoked to navigate back from the top app bar.
 * @param onPromptClick Callback invoked when a prompt item is clicked.
 * @param onPromptLongClick Callback invoked when a prompt item is long-clicked for selection.
 * @param onDeleteClick Callback invoked to request deletion of a prompt by its identifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onPromptClick: (Prompt) -> Unit,
    onPromptLongClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Search prompts") },
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .clipToBounds()
            ) {
                when {
                    uiState.query.isEmpty() -> InitialSearchState()
                    uiState.results.isEmpty() -> EmptySearchState()
                    else -> SearchResultsList(
                        results = uiState.results,
                        selectedPromptId = uiState.selectedPromptId,
                        onPromptClick = onPromptClick,
                        onPromptLongClick = onPromptLongClick,
                        onDeleteClick = onDeleteClick
                    )
                }
            }
        }
    }
}

/**
 * Displays the placeholder UI shown before the user enters a search query.
 */
@Composable
fun InitialSearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Search your prompts",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Displays the empty state UI when no prompt items match the active search query.
 */
@Composable
fun EmptySearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No results found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Renders a vertically scrollable list of filtered search result items using [PromptCard].
 *
 * @param results List of matching [Prompt] items to display.
 * @param selectedPromptId The identifier of the currently selected prompt, or null if none is selected.
 * @param onPromptClick Callback invoked when a result card is clicked.
 * @param onPromptLongClick Callback invoked when a result card is long-clicked to toggle selection.
 * @param onDeleteClick Callback invoked to request prompt deletion by its identifier.
 */
@Composable
fun SearchResultsList(
    results: List<Prompt>,
    selectedPromptId: Long?,
    onPromptClick: (Prompt) -> Unit,
    onPromptLongClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 80.dp
        )
    ) {
        items(
            items = results,
            key = { it.id }
        ) { prompt ->
            PromptCard(
                prompt = prompt,
                isSelected = prompt.id == selectedPromptId,
                onPromptClick = { onPromptClick(prompt) },
                onPromptLongClick = onPromptLongClick,
                onDeleteClick = { onDeleteClick(prompt.id) }
            )
        }
    }
}
