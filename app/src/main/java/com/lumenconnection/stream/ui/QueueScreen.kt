package com.lumenconnection.stream.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenconnection.stream.Graph
import com.lumenconnection.stream.R
import com.lumenconnection.stream.db.DownloadStatus
import com.lumenconnection.stream.ui.theme.Lumen
import kotlinx.coroutines.launch

@Composable
fun QueueScreen() {
    val c = Lumen.colors
    val scope = rememberCoroutineScope()
    val downloads by Graph.db.downloadDao().observeAll().collectAsState(initial = emptyList())
    val hasFinished = downloads.any {
        it.status == DownloadStatus.DONE || it.status == DownloadStatus.FAILED ||
            it.status == DownloadStatus.CANCELLED
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                PageHeader(
                    title = stringResource(R.string.queue_title),
                    subtitle = stringResource(R.string.queue_subtitle),
                )
            }
            if (hasFinished) {
                TextButton(onClick = { scope.launch { Graph.db.downloadDao().clearFinished() } }) {
                    Text(
                        stringResource(R.string.queue_clear_finished),
                        color = c.textMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (downloads.isEmpty()) {
            Text(stringResource(R.string.queue_empty), color = c.textMuted, fontSize = 14.sp)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                items(downloads, key = { it.id }) { task -> DownloadRow(task) }
            }
        }
    }
}
