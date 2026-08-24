package com.lumenconnection.stream.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import com.lumenconnection.stream.Graph
import com.lumenconnection.stream.db.MediaItem

/**
 * Player em primeiro plano. Reprodução em segundo plano (MediaSession) segue
 * fora de escopo; o Picture-in-Picture aqui é acionado só pelo botão, nunca
 * automaticamente ao sair do app.
 */
@Composable
fun PlayerScreen(mediaId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val media by produceState<MediaItem?>(initialValue = null, mediaId) {
        value = Graph.db.mediaDao().byId(mediaId)
    }

    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    // Sem player em segundo plano: ao sair da tela, a reprodução pausa —
    // exceto quando o app entrou em PiP, onde a janela continua tocando.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    activity?.isInPictureInPictureMode == true
                if (!inPip) player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(media) {
        media?.let {
            player.setMediaItem(ExoMediaItem.fromUri(Uri.parse(it.contentUri)))
            player.prepare()
            player.playWhenReady = true
        }
        onDispose { }
    }

    val pipSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Box(Modifier.weight(1f))
            if (pipSupported && media?.kind != "audio") {
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val ratio = Rational(16, 9)
                        activity?.enterPictureInPictureMode(
                            PictureInPictureParams.Builder().setAspectRatio(ratio).build()
                        )
                    }
                }) {
                    Icon(Icons.Outlined.PictureInPictureAlt, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}
