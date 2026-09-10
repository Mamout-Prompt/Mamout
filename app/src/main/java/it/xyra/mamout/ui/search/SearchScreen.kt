package it.xyra.mamout.ui.search

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.xyra.mamout.domain.model.Prompt
import it.xyra.mamout.ui.theme.MamoutTheme

/**
 * The main Search Screen component.
 *
 * Displays a search bar at the top and a list of results below.
 *
 * @param viewModel The ViewModel providing search state and logic.
 * @param onPromptClick Callback triggered when a prompt is selected from the results.
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPromptClick: (Prompt) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    MamoutTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SearchContent(
                query = uiState.query,
                results = uiState.results,
                onQueryChange = viewModel::onQueryChanged,
                onPromptClick = onPromptClick
            )
        }
    }
}

/**
 * Stateless content for the Search Screen, ideal for previews and testing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    query: String,
    results: List<Prompt>,
    onQueryChange: (String) -> Unit,
    onPromptClick: (Prompt) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { },
            active = true,
            onActiveChange = { },
            placeholder = { Text("Search your prompts...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (results.isEmpty() && query.isNotEmpty()) {
                EmptySearchState(query)
            } else if (results.isEmpty() && query.isEmpty()) {
                InitialSearchState()
            } else {
                SearchResultsList(
                    results = results,
                    onPromptClick = onPromptClick
                )
            }
        }
    }
}

/**
 * UI displayed when the user hasn't started searching yet.
 */
@Composable
fun InitialSearchState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Type to find your prompts",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Displays a modern placeholder UI when no results match the query.
 */
@Composable
fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No matches for \"$query\"",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try using more general keywords or check your spelling.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Displays the list of prompt search results using modern Cards.
 */
@Composable
fun SearchResultsList(
    results: List<Prompt>,
    onPromptClick: (Prompt) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(results) { prompt ->
            PromptSearchCard(
                prompt = prompt,
                onClick = { onPromptClick(prompt) }
            )
        }
    }
}

/**
 * Represents a single search result item styled as a modern Material 3 Card.
 */
@Composable
fun PromptSearchCard(
    prompt: Prompt,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = prompt.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = {
                if (prompt.description.isNotEmpty()) {
                    Text(
                        text = prompt.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenLightPreview() {
    MamoutTheme(darkTheme = false) {
        SearchPreviewContent()
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenDarkPreview() {
    MamoutTheme(darkTheme = true) {
        SearchPreviewContent()
    }
}

@Composable
private fun SearchPreviewContent() {
    Surface(color = MaterialTheme.colorScheme.background) {
        val mockResults = listOf(
            Prompt(1, "Email Draft", "Professional business inquiry template"),
            Prompt(2, "Code Review", "Checklist for clean code and performance"),
            Prompt(3, "Fitness Plan", "High intensity interval training schedule")
        )

        SearchContent(
            query = "Plan",
            results = mockResults,
            onQueryChange = {},
            onPromptClick = {}
        )
    }
}
