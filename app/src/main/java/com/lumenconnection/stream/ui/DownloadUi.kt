package com.lumenconnection.stream.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenconnection.stream.R
import com.lumenconnection.stream.db.DownloadStatus
import com.lumenconnection.stream.db.DownloadTask
import com.lumenconnection.stream.download.DownloadService
import com.lumenconnection.stream.download.friendlyErrorText
import com.lumenconnection.stream.extractor.DownloadFormat
import com.lumenconnection.stream.ui.theme.Lumen
import kotlinx.coroutines.launch

val ALL_FORMATS = listOf(
    DownloadFormat.VIDEO_BEST to R.string.format_video,
    DownloadFormat.VIDEO_720 to R.string.format_video_720,
    DownloadFormat.AUDIO_BEST to R.string.format_audio_best,
    DownloadFormat.AUDIO_MP3 to R.string.format_audio_mp3,
    DownloadFormat.AUDIO_OPUS to R.string.format_audio_opus,
)

@Composable
fun FormatDialog(onDismiss: () -> Unit, onSelect: (DownloadFormat) -> Unit) {
    val c = Lumen.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgCard,
        titleContentColor = c.text,
        textContentColor = c.text,
        shape = RoundedCornerShape(12.dp),
        title = { Text(stringResource(R.string.format_dialog_title), fontSize = 18.sp) },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = c.textMuted)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ALL_FORMATS.forEach { (format, label) ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(c.bgInput, RoundedCornerShape(8.dp))
                            .clickable { onSelect(format) },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            stringResource(label),
                            color = c.text,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                    }
                }
            }
        },
    )
}

/** Chip de formato no estilo do desktop: ativo = accent_soft + borda laranja. */
@Composable
fun FormatChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Lumen.colors
    Box(
        Modifier
            .height(36.dp)
            .background(
                if (selected) c.accentSoft else c.bgInput,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) c.accent else c.textMuted, fontSize = 13.sp)
    }
}

@Composable
fun DownloadRow(task: DownloadTask) {
    val c = Lumen.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LumenCard(contentPadding = 14) {
        Text(
            task.title ?: task.url,
            color = c.text,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))

        val statusRes = when (task.status) {
            DownloadStatus.QUEUED -> R.string.status_queued
            DownloadStatus.RUNNING -> R.string.status_running
            DownloadStatus.PROCESSING -> R.string.status_processing
            DownloadStatus.DONE -> R.string.status_done
            DownloadStatus.CANCELLED -> R.string.status_cancelled
            else -> R.string.status_failed
        }
        val statusColor = when (task.status) {
            DownloadStatus.DONE -> c.accent
            DownloadStatus.FAILED -> c.danger
            else -> c.textMuted
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(statusRes), color = statusColor, fontSize = 12.5.sp)
                task.engine?.let {
                    Text("  ·  $it", color = c.textFaint, fontSize = 12.5.sp)
                }
            }
            when (task.status) {
                DownloadStatus.QUEUED, DownloadStatus.RUNNING, DownloadStatus.PROCESSING ->
                    TextButton(onClick = { DownloadService.cancel(context, task.id) }) {
                        Text(stringResource(R.string.action_cancel), color = c.textMuted, fontSize = 13.sp)
                    }
                DownloadStatus.FAILED ->
                    TextButton(onClick = {
                        scope.launch {
                            DownloadService.enqueue(
                                context, task.url,
                                runCatching { DownloadFormat.valueOf(task.format) }
                                    .getOrDefault(DownloadFormat.VIDEO_BEST),
                            )
                        }
                    }) {
                        Text(stringResource(R.string.action_retry), color = c.accent, fontSize = 13.sp)
                    }
                else -> {}
            }
        }

        if (task.status == DownloadStatus.RUNNING || task.status == DownloadStatus.PROCESSING) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { task.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = c.accent,
                trackColor = c.bgCardHover,
            )
        }
        if (task.status == DownloadStatus.FAILED && task.error != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                friendlyErrorText(task.error),
                color = c.danger,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
