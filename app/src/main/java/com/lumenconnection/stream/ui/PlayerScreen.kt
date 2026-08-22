package com.lumenconnection.stream.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lumenconnection.stream.Graph

/**
 * Player foreground (MVP): toca vídeo e áudio da biblioteca.
 * Reprodução em background/MediaSession fica para a fase 1.x.
 */
@Composable
fun PlayerScreen(mediaId: Long) {
    val context = LocalContext.current
    val media by produceState<com.lumenconnection.stream.db.MediaItem?>(initialValue = null, mediaId) {
        value = Graph.db.mediaDao().byId(mediaId)
    }

    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    DisposableEffect(media) {
        media?.let {
            player.setMediaItem(ExoMediaItem.fromUri(Uri.parse(it.contentUri)))
            player.prepare()
            player.playWhenReady = true
        }
        onDispose { }
    }

    Box(Modifier.fillMaxSize()) {
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
    }
}
