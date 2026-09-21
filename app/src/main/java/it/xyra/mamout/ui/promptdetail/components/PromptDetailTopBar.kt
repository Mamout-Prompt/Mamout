package it.xyra.mamout.ui.promptdetail.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Top app bar component for the Prompt Detail screen.
 *
 * Displays the prompt title alongside navigation and action buttons for editing header details,
 * saving prompt changes, and copying the compiled prompt text to the clipboard.
 *
 * @param title The title text displayed in the top bar.
 * @param isSyncing Whether synchronization is currently active.
 * @param onBackClick Callback invoked when the back navigation button is clicked.
 * @param onEditHeaderClick Callback invoked when the edit header button is clicked.
 * @param onSaveClick Callback invoked when the save prompt button is clicked.
 * @param onCopyClick Callback invoked when the copy compiled prompt button is clicked.
 * @param onSyncClick Callback invoked when the synchronization button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptDetailTopBar(
    title: String,
    isSyncing: Boolean,
    onBackClick: () -> Unit,
    onEditHeaderClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCopyClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    TopAppBar(
        title = { Text(text = title, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onSyncClick) {
                Icon(
                    imageVector = if (isSyncing) Icons.Default.SyncDisabled else Icons.Default.Sync,
                    contentDescription = "Synchronize",
                    tint = if (isSyncing) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            IconButton(onClick = onEditHeaderClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Title and Description")
            }
            IconButton(onClick = onSaveClick) {
                Icon(Icons.Default.Save, contentDescription = "Save Prompt")
            }
            IconButton(onClick = onCopyClick) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Compiled Prompt")
            }
        }
    )
}
