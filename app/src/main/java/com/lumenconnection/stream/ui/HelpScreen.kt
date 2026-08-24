package com.lumenconnection.stream.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumenconnection.stream.BuildConfig
import com.lumenconnection.stream.R
import com.lumenconnection.stream.ui.theme.Lumen

@Composable
fun HelpScreen() {
    val c = Lumen.colors
    val topics = listOf(
        R.string.help_engines_title to R.string.help_engines_body,
        R.string.help_spotify_title to R.string.help_spotify_body,
        R.string.help_storage_title to R.string.help_storage_body,
        R.string.help_403_title to R.string.help_403_body,
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        PageHeader(
            title = stringResource(R.string.help_title),
            subtitle = stringResource(R.string.help_subtitle),
        )
        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            topics.forEach { (title, body) ->
                LumenCard {
                    Text(
                        stringResource(title),
                        color = c.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(body), color = c.textMuted, fontSize = 13.5.sp)
                }
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
                Spacer(Modifier.height(10.dp))
                InfoRow(
                    stringResource(R.string.help_version, ""),
                    BuildConfig.VERSION_NAME,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
