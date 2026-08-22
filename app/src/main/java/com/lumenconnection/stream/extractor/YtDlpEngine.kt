package com.lumenconnection.stream.extractor

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Motor de fallback: yt-dlp com Python embutido (cobertura máxima de sites)
 * e ffmpeg embarcado (merge DASH, conversão mp3/opus, legendas).
 */
object YtDlpEngine {

    data class Options(
        val format: DownloadFormat,
        val rateLimitKbps: Int,
        val subtitles: Boolean,
        val playlist: Boolean,
    )

    /**
     * Baixa [url] para [destDir] e retorna os arquivos produzidos.
     * [processId] permite cancelamento via [cancel].
     */
    suspend fun download(
        url: String,
        destDir: File,
        options: Options,
        processId: String,
        onProgress: (Float) -> Unit,
    ): List<File> = withContext(Dispatchers.IO) {
        destDir.mkdirs()
        val request = YoutubeDLRequest(url).apply {
            addOption("-o", "${destDir.absolutePath}/%(title).200s.%(ext)s")
            addOption("--no-mtime")

            when (options.format) {
                DownloadFormat.VIDEO_BEST -> {
                    addOption("-f", "bv*+ba/b")
                    addOption("--merge-output-format", "mp4")
                }
                DownloadFormat.VIDEO_720 -> {
                    addOption("-f", "bv*[height<=720]+ba/b[height<=720]")
                    addOption("--merge-output-format", "mp4")
                }
                DownloadFormat.AUDIO_BEST -> {
                    addOption("-f", "ba/b")
                    addOption("-x")
                }
                DownloadFormat.AUDIO_MP3 -> {
                    addOption("-x")
                    addOption("--audio-format", "mp3")
                }
                DownloadFormat.AUDIO_OPUS -> {
                    addOption("-x")
                    addOption("--audio-format", "opus")
                }
            }

            if (options.rateLimitKbps > 0) {
                addOption("--limit-rate", "${options.rateLimitKbps}K")
            }
            if (options.subtitles) {
                addOption("--write-subs")
                addOption("--sub-langs", "pt.*,en.*")
                addOption("--convert-subs", "srt")
            }
            if (options.playlist) addOption("--yes-playlist") else addOption("--no-playlist")
        }

        YoutubeDL.getInstance().execute(request, processId) { progress, _, _ ->
            if (progress >= 0f) onProgress(progress / 100f)
        }

        destDir.listFiles()?.sortedBy { it.name }.orEmpty()
    }

    fun cancel(processId: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
    }

    suspend fun update(context: android.content.Context): Boolean = withContext(Dispatchers.IO) {
        runCatching { YoutubeDL.getInstance().updateYoutubeDL(context) }.isSuccess
    }
}
