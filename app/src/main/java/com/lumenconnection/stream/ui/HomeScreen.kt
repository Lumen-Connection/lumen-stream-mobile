package com.lumenconnection.stream.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumenconnection.stream.Graph
import com.lumenconnection.stream.R
import com.lumenconnection.stream.db.DownloadStatus
import com.lumenconnection.stream.db.DownloadTask
import com.lumenconnection.stream.download.DownloadService
import com.lumenconnection.stream.extractor.DownloadFormat
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(sharedUrl: String?, onSharedUrlConsumed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var url by remember { mutableStateOf("") }
    var showFormatDialog by remember { mutableStateOf(false) }
    var clipboardSuggestion by remember { mutableStateOf<String?>(null) }

    val downloads by Graph.db.downloadDao().observeAll().collectAsState(initial = emptyList())

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            url = sharedUrl
            showFormatDialog = true
            onSharedUrlConsumed()
        }
    }

    LaunchedEffect(Unit) {
        val clip = clipboard.getText()?.text?.trim()
        if (url.isBlank() && clip != null && clip.startsWith("http") && " " !in clip) {
            clipboardSuggestion = clip
        }
    }

    if (showFormatDialog) {
        FormatDialog(
            onDismiss = { showFormatDialog = false },
            onSelect = { format ->
                showFormatDialog = false
                val target = url.trim()
                scope.launch {
                    DownloadService.enqueue(context, target, format)
                    Toast.makeText(context, R.string.share_added, Toast.LENGTH_SHORT).show()
                }
                url = ""
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.home_paste_hint)) },
            singleLine = true,
        )

        clipboardSuggestion?.let { suggestion ->
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = {
                    url = suggestion
                    clipboardSuggestion = null
                },
                label = {
                    Text(
                        "${stringResource(R.string.clipboard_detected)}: ${suggestion.take(40)}…",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (url.trim().startsWith("http")) {
                    showFormatDialog = true
                } else {
                    Toast.makeText(context, R.string.error_invalid_url, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.home_download))
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.home_queue), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (downloads.isEmpty()) {
            Text(
                stringResource(R.string.queue_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(downloads, key = { it.id }) { task ->
                    DownloadRow(task)
                }
            }
        }
    }
}

@Composable
private fun FormatDialog(onDismiss: () -> Unit, onSelect: (DownloadFormat) -> Unit) {
    val options = listOf(
        DownloadFormat.VIDEO_BEST to R.string.format_video,
        DownloadFormat.VIDEO_720 to R.string.format_video_720,
        DownloadFormat.AUDIO_BEST to R.string.format_audio_best,
        DownloadFormat.AUDIO_MP3 to R.string.format_audio_mp3,
        DownloadFormat.AUDIO_OPUS to R.string.format_audio_opus,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.format_dialog_title)) },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        text = {
            Column {
                options.forEach { (format, label) ->
                    TextButton(
                        onClick = { onSelect(format) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(label))
                    }
                }
            }
        },
    )
}

@Composable
private fun DownloadRow(task: DownloadTask) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                task.title ?: task.url,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusRes = when (task.status) {
                    DownloadStatus.QUEUED -> R.string.status_queued
                    DownloadStatus.RUNNING -> R.string.status_running
                    DownloadStatus.PROCESSING -> R.string.status_processing
                    DownloadStatus.DONE -> R.string.status_done
                    DownloadStatus.CANCELLED -> R.string.status_cancelled
                    else -> R.string.status_failed
                }
                Text(
                    stringResource(statusRes) + (task.engine?.let { "  ·  $it" } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                when (task.status) {
                    DownloadStatus.QUEUED, DownloadStatus.RUNNING, DownloadStatus.PROCESSING -> {
                        IconButton(onClick = { DownloadService.cancel(context, task.id) }) {
                            Icon(Icons.Filled.Cancel, contentDescription = stringResource(R.string.action_cancel))
                        }
                    }
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = {
                            scope.launch {
                                DownloadService.enqueue(
                                    context, task.url,
                                    runCatching { DownloadFormat.valueOf(task.format) }
                                        .getOrDefault(DownloadFormat.VIDEO_BEST),
                                )
                            }
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_retry))
                        }
                    }
                    else -> {}
                }
            }
            if (task.status == DownloadStatus.RUNNING || task.status == DownloadStatus.PROCESSING) {
                LinearProgressIndicator(
                    progress = { task.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (task.status == DownloadStatus.FAILED && task.error != null) {
                Text(
                    task.error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
