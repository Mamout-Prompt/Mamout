package it.xyra.mamout.ui.promptlist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.xyra.mamout.ui.promptlist.components.DeleteConfirmationDialog
import it.xyra.mamout.ui.promptlist.components.PromptCard
import it.xyra.mamout.ui.search.EmptySearchState
import it.xyra.mamout.ui.search.InitialSearchState
import it.xyra.mamout.ui.search.SearchResultsList

/**
 * Displays the main screen containing the interactive prompt list, search overlay bar,
 * floating creation action button, and deletion confirmation dialog.
 *
 * @param viewModel The state holder managing UI states and user interactions for this screen.
 * @param onPromptClick Callback invoked when a prompt item is selected for viewing or editing.
 * @param onAddPromptClick Callback invoked when the user taps the floating action button to create a new prompt.
 * @param modifier The [Modifier] to be applied to the layout root.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptListScreen(
    viewModel: PromptListViewModel,
    onPromptClick: (Long) -> Unit,
    onAddPromptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    // Smoothly animates the horizontal padding of the SearchBar when entering or exiting active state.
    val animatedHorizontalPadding by animateDpAsState(
        targetValue = if (isSearchActive) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 10),
        label = "SearchBarPadding"
    )

    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        floatingActionButton = {
            if (!isSearchActive) {
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
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Top header containing the interactive SearchBar anchor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarTopPadding)
            ) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onSearch = {},
                            expanded = isSearchActive,
                            onExpandedChange = { isSearchActive = it },
                            placeholder = { Text("Search prompts") },
                            leadingIcon = {
                                if (isSearchActive) {
                                    IconButton(onClick = { isSearchActive = false }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search prompts"
                                    )
                                }
                            },
                            trailingIcon = {
                                if (isSearchActive && searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            }
                        )
                    },
                    expanded = isSearchActive,
                    onExpandedChange = { isSearchActive = it },
                    windowInsets = WindowInsets(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = animatedHorizontalPadding,
                            end = animatedHorizontalPadding,
                            bottom = 8.dp
                        )
                ) {
                    // Content displayed inside the expanded search view overlay
                    if (searchQuery.isEmpty()) {
                        InitialSearchState()
                    } else {
                        when (val state = uiState) {
                            is PromptListUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is PromptListUiState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            is PromptListUiState.Empty -> {
                                EmptySearchState()
                            }
                            is PromptListUiState.Success -> {
                                SearchResultsList(
                                    results = state.prompts,
                                    selectedPromptId = state.selectedPromptId,
                                    onPromptClick = { prompt ->
                                        if (state.selectedPromptId != null) {
                                            viewModel.onPromptClick(prompt.id)
                                        } else {
                                            isSearchActive = false
                                            onPromptClick(prompt.id)
                                        }
                                    },
                                    onPromptLongClick = viewModel::onPromptLongClick,
                                    onDeleteClick = { viewModel.onDeleteRequested() }
                                )
                            }
                        }
                    }
                }
            }

            // Main body section containing the primary prompt list
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .clipToBounds(),
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
                        if (state.isDeleteDialogVisible) {
                            DeleteConfirmationDialog(
                                onConfirm = viewModel::onDeleteConfirmed,
                                onDismiss = viewModel::onDeleteDialogDismissed
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 80.dp
                            )
                        ) {
                            items(
                                items = state.prompts,
                                key = { it.id }
                            ) { prompt ->
                                PromptCard(
                                    prompt = prompt,
                                    isSelected = prompt.id == state.selectedPromptId,
                                    onPromptClick = { id ->
                                        if (state.selectedPromptId != null) {
                                            viewModel.onPromptClick(id)
                                        } else {
                                            onPromptClick(id)
                                        }
                                    },
                                    onPromptLongClick = viewModel::onPromptLongClick,
                                    onDeleteClick = viewModel::onDeleteRequested
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
