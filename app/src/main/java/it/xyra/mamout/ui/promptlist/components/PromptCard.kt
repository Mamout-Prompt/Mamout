package it.xyra.mamout.ui.promptlist.components

import androidx.compose.animation.AnimatedVisibility
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
 * Card component representing an individual prompt item.
 * Supports long click to toggle selection state and displays a delete action when selected.
 *
 * @param prompt The prompt domain model to display.
 * @param isSelected Whether this card is currently selected.
 * @param onPromptClick Callback invoked on a standard click.
 * @param onPromptLongClick Callback invoked on a long press.
 * @param onDeleteClick Callback invoked when the delete icon is tapped.
 * @param modifier Modifier to be applied to the card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromptCard(
    prompt: Prompt,
    isSelected: Boolean,
    onPromptClick: (Long) -> Unit,
    onPromptLongClick: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
