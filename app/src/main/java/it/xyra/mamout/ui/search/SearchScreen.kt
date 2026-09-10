package it.xyra.mamout.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.ui.promptlist.components.PromptCard

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
