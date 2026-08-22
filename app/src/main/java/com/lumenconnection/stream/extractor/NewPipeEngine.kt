package com.lumenconnection.stream.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo

/**
 * Motor primário: NewPipe Extractor (YouTube, SoundCloud, PeerTube, Bandcamp...).
 * Leve e rápido, mas cobre menos sites que o yt-dlp e não faz merge/conversão.
 */
object NewPipeEngine {

    suspend fun extract(url: String): ExtractedStreams = withContext(Dispatchers.IO) {
        val service = NewPipe.getServiceByUrl(url)
            ?: throw ExtractionUnsupportedException("No NewPipe service for $url")
        val info = StreamInfo.getInfo(service, url)

        val muxed = info.videoStreams
            .filterNot { it.isVideoOnly }
            .sortedByDescending { parseResolution(it.getResolution()) }
        val best = muxed.firstOrNull()
        val best720 = muxed.firstOrNull { parseResolution(it.getResolution()) <= 720 }

        val audio = info.audioStreams.maxByOrNull { it.averageBitrate }

        ExtractedStreams(
            title = info.name ?: "media",
            uploader = info.uploaderName,
            thumbnailUrl = info.thumbnails.maxByOrNull { it.width }?.url,
            durationSec = info.duration.takeIf { it > 0 },
            muxedVideoUrl = best?.content,
            muxedVideoExt = best?.format?.suffix ?: "mp4",
            muxedVideo720Url = best720?.content,
            muxedVideo720Ext = best720?.format?.suffix ?: "mp4",
            audioUrl = audio?.content,
            audioExt = audio?.format?.suffix ?: "m4a",
        )
    }

    private fun parseResolution(res: String?): Int =
        res?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
}
