package com.lumenconnection.stream.extractor

enum class DownloadFormat {
    VIDEO_BEST,
    VIDEO_720,
    AUDIO_BEST,
    AUDIO_MP3,
    AUDIO_OPUS;

    val isAudio: Boolean
        get() = this == AUDIO_BEST || this == AUDIO_MP3 || this == AUDIO_OPUS

    /** Formatos que exigem ffmpeg (conversão/merge) só funcionam via yt-dlp. */
    val needsYtDlp: Boolean
        get() = this == AUDIO_MP3 || this == AUDIO_OPUS || this == VIDEO_BEST
}

/** Resultado da extração via NewPipe: URLs diretas para download HTTP. */
data class ExtractedStreams(
    val title: String,
    val uploader: String?,
    val thumbnailUrl: String?,
    val durationSec: Long?,
    /** Melhor stream muxado (vídeo+áudio) e sua extensão. */
    val muxedVideoUrl: String?,
    val muxedVideoExt: String?,
    /** Stream muxado limitado a 720p. */
    val muxedVideo720Url: String?,
    val muxedVideo720Ext: String?,
    /** Melhor stream só de áudio. */
    val audioUrl: String?,
    val audioExt: String?,
)

class ExtractionUnsupportedException(message: String) : Exception(message)
