package it.xyra.mamout.ui.promptlist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.xyra.mamout.ui.promptlist.components.PromptCard
import it.xyra.mamout.ui.search.InitialSearchState
import it.xyra.mamout.ui.search.EmptySearchState
import it.xyra.mamout.ui.search.SearchResultsList

/**
 * Displays the main screen containing the prompt list, search bar trigger, and creation button.
 *
 * @param viewModel The state holder for the prompt list screen.
 * @param onPromptClick Callback invoked when a prompt item is selected.
 * @param onAddPromptClick Callback invoked when the add prompt button is tapped.
 * @param modifier The modifier to be applied to the layout.
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

    // Speed up the transition even further (180ms) for an ultra-responsive, snappier feel
    val animatedHorizontalPadding by animateDpAsState(
        targetValue = if (isSearchActive) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 10),
        label = "SearchBarPadding"
    )

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
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Content Layer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Fixed spacer that matches exactly the space taken by the search bar when unexpanded
                Spacer(modifier = Modifier.height(64.dp))

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

            // Search Layer: Positioned at top = 0.dp using windowInsets to eliminate any offset or vertical jump
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSearch = {},
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
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
                },
                windowInsets = SearchBarDefaults.windowInsets,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        start = animatedHorizontalPadding,
                        end = animatedHorizontalPadding,
                        top = if (isSearchActive) 0.dp else 8.dp
                    )
            ) {
                if (searchQuery.isEmpty()) {
                    InitialSearchState()
                } else {
                    when (val state = uiState) {
                        is PromptListUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is PromptListUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        is PromptListUiState.Empty -> {
                            EmptySearchState()
                        }
                        is PromptListUiState.Success -> {
                            SearchResultsList(
                                results = state.prompts,
                                onPromptClick = { prompt ->
                                    isSearchActive = false
                                    onPromptClick(prompt.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
