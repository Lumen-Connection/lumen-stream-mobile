package com.lumenconnection.stream

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.lumenconnection.stream.extractor.NewPipeDownloaderImpl
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.NewPipe

class LumenApp : Application(), ImageLoaderFactory {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var ytDlpReady: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        NewPipe.init(NewPipeDownloaderImpl.instance)
        createNotificationChannels()
        // Python + yt-dlp + ffmpeg são pesados: inicializa fora da main thread
        appScope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().init(this@LumenApp)
                FFmpeg.getInstance().init(this@LumenApp)
                ytDlpReady = true
            } catch (t: Throwable) {
                // Throwable, não Exception: falhas de <clinit> chegam como Error
                // e derrubariam o app inteiro. Sem yt-dlp o NewPipe ainda atende.
                Log.e("LumenApp", "yt-dlp init failed", t)
            }
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DOWNLOADS,
                getString(R.string.notif_channel_downloads),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()

    companion object {
        const val CHANNEL_DOWNLOADS = "downloads"
    }
}
