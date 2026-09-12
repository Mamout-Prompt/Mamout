package it.xyra.mamout.ui.promptlist.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.xyra.mamout.domain.model.Prompt

/**
 * Card composable representing an individual prompt item in a list or search result grid.
 *
 * Displays the prompt title and description, supports shared element transitions during navigation,
 * and provides click, long-click selection state feedback, and deletion triggers.
 *
 * @param prompt The [Prompt] domain model to display.
 * @param isSelected Indicates whether the prompt card is currently selected.
 * @param sharedTransitionScope Scope required for shared element transition bounds.
 * @param animatedVisibilityScope Scope required for visibility-driven transitions.
 * @param onPromptClick Callback invoked when the prompt card is clicked, passing the prompt ID.
 * @param onPromptLongClick Callback invoked when the prompt card is long-clicked, passing the prompt ID.
 * @param onDeleteClick Callback invoked when the delete icon button is tapped.
 * @param modifier Optional [Modifier] applied to the card layout.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PromptCard(
    prompt: Prompt,
    isSelected: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPromptClick: (Long) -> Unit,
    onPromptLongClick: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    with(sharedTransitionScope) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            modifier = modifier
                .fillMaxWidth()
                .sharedBounds(
                    rememberSharedContentState(key = "prompt-container-${prompt.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .combinedClickable(
                    onClick = { onPromptClick(prompt.id) },
                    onLongClick = { onPromptLongClick(prompt.id) }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = if (isSelected) 40.dp else 0.dp)
                ) {
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = prompt.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete prompt",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
