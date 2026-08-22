package com.lumenconnection.stream.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumenconnection.stream.Graph
import com.lumenconnection.stream.R
import com.lumenconnection.stream.config.Engine
import com.lumenconnection.stream.extractor.YtDlpEngine
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = Graph.settings

    val engine by settings.engine.collectAsState(initial = Engine.AUTO)
    val rateLimit by settings.rateLimitKbps.collectAsState(initial = 0)
    val customTree by settings.customTreeUri.collectAsState(initial = null)
    val subtitles by settings.subtitles.collectAsState(initial = false)
    val playlist by settings.playlist.collectAsState(initial = false)

    var rateLimitText by remember(rateLimit) { mutableStateOf(rateLimit.toString()) }
    var updating by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            scope.launch { settings.setCustomTreeUri(uri.toString()) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Motor de extração
        Text(stringResource(R.string.settings_engine), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = engine == Engine.AUTO,
                onClick = { scope.launch { settings.setEngine(Engine.AUTO) } },
                label = { Text(stringResource(R.string.engine_auto)) },
            )
            FilterChip(
                selected = engine == Engine.NEWPIPE,
                onClick = { scope.launch { settings.setEngine(Engine.NEWPIPE) } },
                label = { Text(stringResource(R.string.engine_newpipe)) },
            )
            FilterChip(
                selected = engine == Engine.YTDLP,
                onClick = { scope.launch { settings.setEngine(Engine.YTDLP) } },
                label = { Text(stringResource(R.string.engine_ytdlp)) },
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Armazenamento
        Text(stringResource(R.string.settings_storage), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = customTree == null,
                onClick = { scope.launch { settings.setCustomTreeUri(null) } },
            )
            Text(stringResource(R.string.settings_storage_default))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = customTree != null,
                onClick = { folderPicker.launch(null) },
            )
            Column {
                Text(stringResource(R.string.settings_storage_custom))
                if (customTree != null) {
                    Text(
                        customTree ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        OutlinedButton(onClick = { folderPicker.launch(null) }) {
            Text(stringResource(R.string.settings_choose_folder))
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Limite de velocidade
        Text(stringResource(R.string.settings_rate_limit), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = rateLimitText,
            onValueChange = { text ->
                rateLimitText = text.filter { it.isDigit() }.take(7)
                scope.launch { settings.setRateLimitKbps(rateLimitText.toIntOrNull() ?: 0) }
            },
            modifier = Modifier.width(160.dp),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Opções de download
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_subtitles), modifier = Modifier.weight(1f))
            Switch(
                checked = subtitles,
                onCheckedChange = { scope.launch { settings.setSubtitles(it) } },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_playlist), modifier = Modifier.weight(1f))
            Switch(
                checked = playlist,
                onCheckedChange = { scope.launch { settings.setPlaylist(it) } },
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Button(
            enabled = !updating,
            onClick = {
                updating = true
                scope.launch {
                    val ok = YtDlpEngine.update(context.applicationContext)
                    updating = false
                    Toast.makeText(
                        context,
                        if (ok) R.string.ytdlp_updated else R.string.ytdlp_update_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        ) {
            Text(stringResource(R.string.settings_update_ytdlp))
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.settings_about_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}
