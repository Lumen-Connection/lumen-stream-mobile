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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
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
import androidx.compose.ui.unit.sp
import com.lumenconnection.stream.Graph
import com.lumenconnection.stream.R
import com.lumenconnection.stream.download.DownloadService
import com.lumenconnection.stream.extractor.DownloadFormat
import com.lumenconnection.stream.ui.theme.Lumen
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onOpenQueue: () -> Unit) {
    val c = Lumen.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var url by remember { mutableStateOf("") }
    var showFormatDialog by remember { mutableStateOf(false) }
    var clipboardSuggestion by remember { mutableStateOf<String?>(null) }

    val downloads by Graph.db.downloadDao().observeAll().collectAsState(initial = emptyList())

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
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        PageHeader(
            title = stringResource(R.string.home_title),
            subtitle = stringResource(R.string.home_subtitle),
        )
        Spacer(Modifier.height(20.dp))

        LumenCard {
            SectionLabel(stringResource(R.string.home_quick))
            Spacer(Modifier.height(10.dp))
            LumenTextField(
                value = url,
                onValueChange = { url = it },
                placeholder = stringResource(R.string.home_paste_hint),
                modifier = Modifier.fillMaxWidth(),
            )
            clipboardSuggestion?.let { suggestion ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${stringResource(R.string.clipboard_detected)}: ${suggestion.take(34)}…",
                        color = c.textFaint,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        url = suggestion
                        clipboardSuggestion = null
                    }) {
                        Text(stringResource(R.string.action_use), color = c.accent, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            AccentButton(
                text = stringResource(R.string.home_download),
                icon = Icons.Outlined.FileDownload,
                onClick = {
                    if (url.trim().startsWith("http")) {
                        showFormatDialog = true
                    } else {
                        Toast.makeText(context, R.string.error_invalid_url, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.home_queue),
                color = c.text,
                fontSize = 17.sp,
            )
            if (downloads.isNotEmpty()) {
                TextButton(onClick = onOpenQueue) {
                    Text(stringResource(R.string.queue_see_all), color = c.accent, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        if (downloads.isEmpty()) {
            Text(stringResource(R.string.queue_empty), color = c.textMuted, fontSize = 14.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                downloads.take(4).forEach { task -> DownloadRow(task) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
