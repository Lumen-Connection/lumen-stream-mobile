package com.lumenconnection.stream.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenconnection.stream.Graph
import com.lumenconnection.stream.R
import com.lumenconnection.stream.download.DownloadService
import com.lumenconnection.stream.extractor.DownloadFormat
import com.lumenconnection.stream.ui.theme.Lumen
import kotlinx.coroutines.launch

/**
 * Abas Música e Vídeo do desktop: mesmo cartão de download, já filtrado para o
 * tipo de mídia, com os formatos daquele tipo em chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaTabScreen(audio: Boolean) {
    val c = Lumen.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val formats = if (audio) {
        listOf(
            DownloadFormat.AUDIO_BEST to R.string.format_audio_best,
            DownloadFormat.AUDIO_MP3 to R.string.format_audio_mp3,
            DownloadFormat.AUDIO_OPUS to R.string.format_audio_opus,
        )
    } else {
        listOf(
            DownloadFormat.VIDEO_BEST to R.string.format_video,
            DownloadFormat.VIDEO_720 to R.string.format_video_720,
        )
    }

    var url by remember { mutableStateOf("") }
    var selected by remember(audio) { mutableStateOf(formats.first().first) }

    val downloads by Graph.db.downloadDao().observeAll().collectAsState(initial = emptyList())
    val recent = downloads.filter {
        val f = runCatching { DownloadFormat.valueOf(it.format) }.getOrNull()
        f != null && f.isAudio == audio
    }.take(5)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        PageHeader(
            title = stringResource(if (audio) R.string.music_title else R.string.video_title),
            subtitle = stringResource(if (audio) R.string.music_subtitle else R.string.video_subtitle),
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
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                formats.forEach { (format, label) ->
                    FormatChip(
                        label = stringResource(label),
                        selected = selected == format,
                        onClick = { selected = format },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            AccentButton(
                text = stringResource(R.string.home_download),
                icon = Icons.Outlined.FileDownload,
                onClick = {
                    val target = url.trim()
                    if (target.startsWith("http")) {
                        scope.launch {
                            DownloadService.enqueue(context, target, selected)
                            Toast.makeText(context, R.string.share_added, Toast.LENGTH_SHORT).show()
                        }
                        url = ""
                    } else {
                        Toast.makeText(context, R.string.error_invalid_url, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.home_queue), color = c.text, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recent.forEach { task -> DownloadRow(task) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
