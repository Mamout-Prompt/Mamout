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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.ui.promptlist.components.PromptCard

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBackClick: () -> Unit,
    onPromptClick: (Prompt) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SearchContent(
        query = uiState.query,
        results = uiState.results,
        onQueryChange = viewModel::onQueryChanged,
        onBackClick = onBackClick,
        onPromptClick = onPromptClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    query: String,
    results: List<Prompt>,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onPromptClick: (Prompt) -> Unit
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
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = { Text("Search prompts") },
                singleLine = true
            )

            when {
                query.isEmpty() -> InitialSearchState()
                results.isEmpty() -> EmptySearchState()
                else -> SearchResultsList(results, onPromptClick)
            }
        }
    }
}

/**
 * UI displayed when the user hasn't started searching yet.
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
 * Displays a modern placeholder UI when no results match the query.
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
 * Displays the list of prompt search results using the shared PromptCard component.
 * Configured with the exact same content padding and item gaps as PromptListScreen.
 */
@Composable
fun SearchResultsList(
    results: List<Prompt>,
    onPromptClick: (Prompt) -> Unit
) {
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
            items = results,
            key = { it.id }
        ) { prompt ->
            PromptCard(
                prompt = prompt,
                onPromptClick = { onPromptClick(prompt) }
            )
        }
    }
}
