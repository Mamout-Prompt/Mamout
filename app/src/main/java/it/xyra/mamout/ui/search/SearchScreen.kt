package it.xyra.mamout.ui.search

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.xyra.mamout.R
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.ui.promptlist.components.DeleteConfirmationDialog
import it.xyra.mamout.ui.promptlist.components.PromptCard

/**
 * Stateful entry point for the standalone Search Screen.
 *
 * Observes state from [SearchViewModel] and renders search input, filtered results,
 * item selection, and deletion confirmation dialogs.
 *
 * @param viewModel The state holder managing UI state and search logic.
 * @param sharedTransitionScope Scope required for shared element transitions.
 * @param animatedVisibilityScope Scope required for visibility-driven transitions.
 * @param onBackClick Callback invoked when navigating back to the previous screen.
 * @param onPromptClick Callback invoked when a search result prompt is selected.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onQueryChange = viewModel::onQueryChanged,
        onBackClick = onBackClick,
        onPromptClick = { prompt ->
            if (uiState.selectedPromptId != null) {
                viewModel.onPromptClick()
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
 * Displays top app bar navigation, a search query text field, and renders conditional content
 * based on search query state and results.
 *
 * @param uiState The current [SearchUiState] to display.
 * @param sharedTransitionScope Scope required for shared element transitions.
 * @param animatedVisibilityScope Scope required for visibility-driven transitions.
 * @param onQueryChange Callback invoked when the search query text changes.
 * @param onBackClick Callback invoked when navigating back.
 * @param onPromptClick Callback invoked when a prompt item is clicked.
 * @param onPromptLongClick Callback invoked when a prompt item is long-clicked.
 * @param onDeleteClick Callback invoked when prompt deletion is requested.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchContent(
    uiState: SearchUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
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
 * Displays placeholder UI before the user enters a search query.
 */
@Composable
fun InitialSearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Search your prompts",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Displays empty state UI when no prompt items match the active search query.
 */
@Composable
fun EmptySearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.empty),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No results found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Renders a vertically scrollable list of filtered search result items using [PromptCard].
 *
 * @param results List of [Prompt] domain models matching the search query.
 * @param selectedPromptId Identifier of the currently selected prompt item, or `null`.
 * @param sharedTransitionScope Scope required for shared element transitions.
 * @param animatedVisibilityScope Scope required for visibility-driven transitions.
 * @param onPromptClick Callback invoked when a prompt item is clicked.
 * @param onPromptLongClick Callback invoked when a prompt item is long-clicked.
 * @param onDeleteClick Callback invoked when prompt deletion is requested.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchResultsList(
    results: List<Prompt>,
    selectedPromptId: Long?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onPromptClick = { onPromptClick(prompt) },
                onPromptLongClick = onPromptLongClick,
                onDeleteClick = { onDeleteClick(prompt.id) }
            )
        }
    }
}
