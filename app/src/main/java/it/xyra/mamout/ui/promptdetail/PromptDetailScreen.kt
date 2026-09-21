package it.xyra.mamout.ui.promptdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.xyra.mamout.ui.promptviewer.AdvancedPromptViewer
import it.xyra.mamout.ui.promptdetail.components.PromptDetailTopBar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon

/**
 * Composable screen for displaying and interacting with prompt details.
 *
 * Provides a top bar for actions, a header editing dialog, shared transition bounds,
 * and integration with [AdvancedPromptViewer] to support preview and raw editing modes.
 *
 * @param viewModel The [PromptDetailViewModel] instance managing UI state and user actions.
 * @param sharedTransitionScope The shared transition scope used for element animations.
 * @param animatedVisibilityScope The animated visibility scope managing navigation transitions.
 * @param onBackClick Callback invoked when navigating back to the previous screen.
 * @param modifier Optional [Modifier] applied to the screen layout.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PromptDetailScreen(
    viewModel: PromptDetailViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (uiState.isEditingHeader) {
        AlertDialog(
            onDismissRequest = { viewModel.onToggleEditHeader(false) },
            title = { Text("Edit Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Description") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onToggleEditHeader(false) }) {
                    Text("Done")
                }
            }
        )
    }

    with(sharedTransitionScope) {
        Scaffold(
            topBar = {
                PromptDetailTopBar(
                    title = uiState.title,
                    isSyncing = uiState.isSyncing,
                    onBackClick = onBackClick,
                    onEditHeaderClick = { viewModel.onToggleEditHeader(true) },
                    onSaveClick = {
                        viewModel.savePrompt()
                        Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                    },
                    onCopyClick = {
                        val compiled = viewModel.getCompiledPrompt()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Compiled Prompt", compiled))
                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    onSyncClick = {
                        viewModel.toggleSync()
                    }
                )
            },
            modifier = modifier
                .fillMaxSize()
                .sharedBounds(
                    rememberSharedContentState(key = "prompt-container-${uiState.promptId}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column {
                    if (uiState.isSyncing && uiState.syncIp != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isClientConnected) Icons.Default.CheckCircle else Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = if (uiState.isClientConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (uiState.isClientConnected) "Synchronized" else "Waiting for synchronization",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    if (!uiState.isClientConnected) {
                                        Text(
                                            text = "${uiState.syncIp}:${uiState.syncPort}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    } else if (uiState.errorMessage != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        AdvancedPromptViewer(
                            templateTextValue = uiState.templateTextValue,
                            onTemplateTextValueChange = viewModel::onTemplateTextValueChange,
                            isRawMode = uiState.isRawMode,
                            onToggleRawMode = viewModel::onToggleRawMode,
                            onNextInput = { viewModel.onNavigateInput(1) },
                            onPreviousInput = { viewModel.onNavigateInput(-1) },
                            inputValues = uiState.inputValues,
                            onInputValueChange = viewModel::onInputValueChange,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
