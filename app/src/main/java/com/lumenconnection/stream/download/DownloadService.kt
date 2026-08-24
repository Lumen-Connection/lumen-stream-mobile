package com.lumenconnection.stream.download

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lumenconnection.stream.Graph
import com.lumenconnection.stream.LumenApp
import com.lumenconnection.stream.MainActivity
import com.lumenconnection.stream.R
import com.lumenconnection.stream.config.Engine
import com.lumenconnection.stream.db.DownloadStatus
import com.lumenconnection.stream.db.DownloadTask
import com.lumenconnection.stream.db.MediaItem
import com.lumenconnection.stream.extractor.DownloadFormat
import com.lumenconnection.stream.extractor.ExtractionUnsupportedException
import com.lumenconnection.stream.extractor.NewPipeEngine
import com.lumenconnection.stream.extractor.YtDlpEngine
import com.lumenconnection.stream.media.MediaSaver
import com.lumenconnection.stream.metadata.SpotifyMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Collections

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var worker: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cancelId = intent?.getLongExtra(EXTRA_CANCEL_ID, -1L) ?: -1L
        if (cancelId > 0) {
            cancelledIds.add(cancelId)
            YtDlpEngine.cancel(cancelId.toString())
            scope.launch {
                Graph.db.downloadDao().setStatus(cancelId, DownloadStatus.CANCELLED)
            }
        }

        startForeground(NOTIF_ID, buildNotification(getString(R.string.status_queued), 0f, indeterminate = true))
        if (worker?.isActive != true) {
            worker = scope.launch {
                processQueue()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun processQueue() {
        val dao = Graph.db.downloadDao()
        while (true) {
            val task = dao.nextQueued() ?: break
            if (task.id in cancelledIds) continue
            runTask(task)
        }
    }

    private suspend fun runTask(task: DownloadTask) {
        val dao = Graph.db.downloadDao()
        val settings = Graph.settings
        val engine = settings.engine.first()
        val rateLimit = settings.rateLimitKbps.first()
        val subtitles = settings.subtitles.first()
        val playlist = settings.playlist.first()
        val customTree = settings.customTreeUri.first()
        val format = runCatching { DownloadFormat.valueOf(task.format) }
            .getOrDefault(DownloadFormat.VIDEO_BEST)

        dao.setProgress(task.id, 0f, DownloadStatus.RUNNING, null, task.title)
        notifyProgress(task.title ?: task.url, 0f)

        try {
            var effectiveTask = task

            // Spotify é DRM: resolve metadados (oEmbed/embed público) e converte
            // cada faixa em `ytsearch1:Artista - Faixa` buscado no YouTube.
            if (SpotifyMetadata.isSpotifyUrl(task.url)) {
                val tracks = SpotifyMetadata.resolve(task.url)
                tracks.drop(1).forEach { t ->
                    dao.insert(DownloadTask(url = t.searchTarget, title = t.label, format = task.format))
                }
                effectiveTask = task.copy(
                    url = tracks.first().searchTarget,
                    title = tracks.first().label,
                )
                dao.setProgress(task.id, 0f, DownloadStatus.RUNNING, "yt-dlp", effectiveTask.title)
            }

            val newPipeAllowed = engine != Engine.YTDLP &&
                !format.needsYtDlp && !subtitles && !playlist &&
                !effectiveTask.url.startsWith("ytsearch")

            var done = false
            if (newPipeAllowed) {
                try {
                    runNewPipe(effectiveTask, format, rateLimit, customTree)
                    done = true
                } catch (e: HttpDownloader.CancelledException) {
                    throw e
                } catch (e: Exception) {
                    if (engine == Engine.NEWPIPE) throw e
                    // AUTO: cai para o yt-dlp. Logar aqui é o que torna a queda
                    // diagnosticável — sem isso a falha do NewPipe fica invisível.
                    Log.w(TAG, "NewPipe failed for ${effectiveTask.url}, falling back to yt-dlp", e)
                }
            }
            if (!done) {
                if (engine == Engine.NEWPIPE) {
                    throw ExtractionUnsupportedException(
                        "Format/options require yt-dlp but engine is NewPipe-only"
                    )
                }
                runYtDlp(effectiveTask, format, rateLimit, subtitles, playlist, customTree)
            }
            dao.setProgress(task.id, 1f, DownloadStatus.DONE, null, null)
        } catch (e: HttpDownloader.CancelledException) {
            dao.setStatus(task.id, DownloadStatus.CANCELLED)
        } catch (e: Exception) {
            if (task.id in cancelledIds) {
                dao.setStatus(task.id, DownloadStatus.CANCELLED)
            } else {
                dao.setStatus(task.id, DownloadStatus.FAILED, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private suspend fun runNewPipe(
        task: DownloadTask,
        format: DownloadFormat,
        rateLimit: Int,
        customTree: String?,
    ) {
        val dao = Graph.db.downloadDao()
        val info = NewPipeEngine.extract(task.url)
        dao.setProgress(task.id, 0f, DownloadStatus.RUNNING, "newpipe", info.title)

        val (streamUrl, ext) = when (format) {
            DownloadFormat.AUDIO_BEST -> info.audioUrl to (info.audioExt ?: "m4a")
            DownloadFormat.VIDEO_720 ->
                (info.muxedVideo720Url ?: info.muxedVideoUrl) to (info.muxedVideo720Ext ?: "mp4")
            else -> info.muxedVideoUrl to (info.muxedVideoExt ?: "mp4")
        }
        if (streamUrl == null) throw ExtractionUnsupportedException("No suitable stream")

        val safeTitle = sanitizeFileName(info.title)
        val tempDir = File(cacheDir, "np").apply { mkdirs() }
        val tempFile = File(tempDir, "${task.id}-$safeTitle.$ext")

        HttpDownloader.download(
            url = streamUrl,
            dest = tempFile,
            rateLimitKbps = rateLimit,
            isCancelled = { task.id in cancelledIds },
        ) { progress ->
            runBlocking { dao.setProgress(task.id, progress, DownloadStatus.RUNNING, "newpipe", info.title) }
            notifyProgress(info.title, progress)
        }

        // renomeia sem o prefixo do id antes de persistir
        val finalTemp = File(tempDir, "$safeTitle.$ext")
        tempFile.renameTo(finalTemp)
        val uri = MediaSaver.persist(this, finalTemp, customTree)

        Graph.db.mediaDao().insert(
            MediaItem(
                title = info.title,
                uploader = info.uploader,
                sourceUrl = task.url,
                contentUri = uri.toString(),
                kind = if (format.isAudio) "audio" else "video",
                mimeType = MediaSaver.mimeTypeFor(ext),
                thumbnailUrl = info.thumbnailUrl,
                durationSec = info.durationSec,
            )
        )
    }

    private suspend fun runYtDlp(
        task: DownloadTask,
        format: DownloadFormat,
        rateLimit: Int,
        subtitles: Boolean,
        playlist: Boolean,
        customTree: String?,
    ) {
        val app = application as LumenApp
        if (!app.ytDlpReady) throw IllegalStateException("yt-dlp not initialized yet")

        val dao = Graph.db.downloadDao()
        dao.setProgress(task.id, 0f, DownloadStatus.RUNNING, "yt-dlp", task.title)

        val destDir = File(cacheDir, "ytdlp/${task.id}")
        try {
            val onProgress: (Float) -> Unit = { progress ->
                runBlocking { dao.setProgress(task.id, progress, DownloadStatus.RUNNING, "yt-dlp", task.title) }
                notifyProgress(task.title ?: task.url, progress)
            }
            val options = YtDlpEngine.Options(format, rateLimit, subtitles, playlist)
            val files = try {
                YtDlpEngine.download(task.url, destDir, options, task.id.toString(), onProgress)
            } catch (e: Exception) {
                // 403 do YouTube costuma ser yt-dlp desatualizado: atualiza e
                // tenta uma vez de novo, como o desktop faz.
                val msg = e.message.orEmpty()
                val updatable = FriendlyError.isHttp403(msg)
                if (task.id in cancelledIds || !updatable || !YtDlpEngine.update(applicationContext)) throw e
                destDir.deleteRecursively()
                YtDlpEngine.download(task.url, destDir, options, task.id.toString(), onProgress)
            }
            if (task.id in cancelledIds) throw HttpDownloader.CancelledException()
            if (files.isEmpty()) throw IllegalStateException("yt-dlp produced no files")

            dao.setProgress(task.id, 1f, DownloadStatus.PROCESSING, "yt-dlp", task.title)
            for (file in files) {
                val ext = file.extension
                val uri = MediaSaver.persist(this, file, customTree)
                if (MediaSaver.isSubtitleExt(ext)) continue
                Graph.db.mediaDao().insert(
                    MediaItem(
                        title = file.nameWithoutExtension,
                        uploader = null,
                        sourceUrl = task.url,
                        contentUri = uri.toString(),
                        kind = if (MediaSaver.isAudioExt(ext)) "audio" else "video",
                        mimeType = MediaSaver.mimeTypeFor(ext),
                    )
                )
            }
        } finally {
            destDir.deleteRecursively()
        }
    }

    private fun notifyProgress(title: String, progress: Float) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(title, progress, indeterminate = false))
    }

    private fun buildNotification(title: String, progress: Float, indeterminate: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, LumenApp.CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(getString(R.string.download_notification_title))
            .setContentText(title)
            .setProgress(100, (progress * 100).toInt(), indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        private const val TAG = "DownloadService"
        private const val NOTIF_ID = 1001
        private const val EXTRA_CANCEL_ID = "cancel_id"

        private val cancelledIds: MutableSet<Long> = Collections.synchronizedSet(mutableSetOf<Long>())

        suspend fun enqueue(context: Context, url: String, format: DownloadFormat) {
            Graph.db.downloadDao().insert(DownloadTask(url = url.trim(), format = format.name))
            context.startForegroundService(Intent(context, DownloadService::class.java))
        }

        fun cancel(context: Context, taskId: Long) {
            val intent = Intent(context, DownloadService::class.java)
                .putExtra(EXTRA_CANCEL_ID, taskId)
            context.startForegroundService(intent)
        }

        fun sanitizeFileName(name: String): String =
            name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(180).trim()
    }
}
