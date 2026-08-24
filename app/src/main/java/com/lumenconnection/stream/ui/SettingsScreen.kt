package com.lumenconnection.stream.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenconnection.stream.Graph
import com.lumenconnection.stream.R
import com.lumenconnection.stream.config.Engine
import com.lumenconnection.stream.config.ThemeMode
import com.lumenconnection.stream.extractor.YtDlpEngine
import com.lumenconnection.stream.ui.theme.Lumen
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val c = Lumen.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = Graph.settings

    val engine by settings.engine.collectAsState(initial = Engine.AUTO)
    val rateLimit by settings.rateLimitKbps.collectAsState(initial = 0)
    val customTree by settings.customTreeUri.collectAsState(initial = null)
    val subtitles by settings.subtitles.collectAsState(initial = false)
    val playlist by settings.playlist.collectAsState(initial = false)
    val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val highContrast by settings.highContrast.collectAsState(initial = false)
    val compact by settings.compact.collectAsState(initial = false)

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
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        PageHeader(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.settings_subtitle),
        )
        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            LumenCard {
                SectionLabel(stringResource(R.string.settings_engine))
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Engine.AUTO to R.string.engine_auto,
                        Engine.NEWPIPE to R.string.engine_newpipe,
                        Engine.YTDLP to R.string.engine_ytdlp,
                    ).forEach { (value, label) ->
                        FormatChip(
                            label = stringResource(label),
                            selected = engine == value,
                            onClick = { scope.launch { settings.setEngine(value) } },
                        )
                    }
                }
            }

            LumenCard {
                SectionLabel(stringResource(R.string.settings_appearance))
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to R.string.settings_theme_system,
                        ThemeMode.DARK to R.string.settings_theme_dark,
                        ThemeMode.LIGHT to R.string.settings_theme_light,
                    ).forEach { (value, label) ->
                        FormatChip(
                            label = stringResource(label),
                            selected = themeMode == value,
                            onClick = { scope.launch { settings.setThemeMode(value) } },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                SettingSwitch(
                    label = stringResource(R.string.settings_high_contrast),
                    checked = highContrast,
                    onChange = { scope.launch { settings.setHighContrast(it) } },
                )
                SettingSwitch(
                    label = stringResource(R.string.settings_compact),
                    checked = compact,
                    onChange = { scope.launch { settings.setCompact(it) } },
                )
            }

            LumenCard {
                SectionLabel(stringResource(R.string.settings_storage))
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatChip(
                        label = stringResource(R.string.settings_storage_default),
                        selected = customTree == null,
                        onClick = { scope.launch { settings.setCustomTreeUri(null) } },
                    )
                    FormatChip(
                        label = stringResource(R.string.settings_storage_custom),
                        selected = customTree != null,
                        onClick = { folderPicker.launch(null) },
                    )
                }
                if (customTree != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        customTree.orEmpty(),
                        color = c.textFaint,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                GhostButton(
                    text = stringResource(R.string.settings_choose_folder),
                    onClick = { folderPicker.launch(null) },
                )
            }

            LumenCard {
                SectionLabel(stringResource(R.string.settings_rate_limit))
                Spacer(Modifier.height(10.dp))
                LumenTextField(
                    value = rateLimitText,
                    onValueChange = { text ->
                        rateLimitText = text.filter { it.isDigit() }.take(7)
                        scope.launch { settings.setRateLimitKbps(rateLimitText.toIntOrNull() ?: 0) }
                    },
                    placeholder = "0",
                    modifier = Modifier.width(160.dp),
                )
                Spacer(Modifier.height(12.dp))
                SettingSwitch(
                    label = stringResource(R.string.settings_subtitles),
                    checked = subtitles,
                    onChange = { scope.launch { settings.setSubtitles(it) } },
                )
                SettingSwitch(
                    label = stringResource(R.string.settings_playlist),
                    checked = playlist,
                    onChange = { scope.launch { settings.setPlaylist(it) } },
                )
            }

            LumenCard {
                SectionLabel("yt-dlp")
                Spacer(Modifier.height(10.dp))
                AccentButton(
                    text = stringResource(R.string.settings_update_ytdlp),
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
                )
            }

            LumenCard {
                Text(
                    stringResource(R.string.settings_about),
                    color = c.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.settings_about_body),
                    color = c.textMuted,
                    fontSize = 13.5.sp,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Lumen.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = c.accent,
                uncheckedThumbColor = c.textMuted,
                uncheckedTrackColor = c.bgInput,
                uncheckedBorderColor = c.border,
            ),
        )
    }
}
