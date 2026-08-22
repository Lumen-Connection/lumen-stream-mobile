package com.lumenconnection.stream.download

import com.lumenconnection.stream.extractor.NewPipeDownloaderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Download HTTP com retomada (Range) e rate limiting — semântica portada do
 * queue.rs do Lumen Stream Desktop.
 */
object HttpDownloader {

    class CancelledException : IOException("cancelled")

    suspend fun download(
        url: String,
        dest: File,
        rateLimitKbps: Int,
        isCancelled: () -> Boolean,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val existing = if (dest.exists()) dest.length() else 0L
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", NewPipeDownloaderImpl.USER_AGENT)
        if (existing > 0) builder.header("Range", "bytes=$existing-")

        val client = NewPipeDownloaderImpl.instance.client
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")

            val resumed = response.code == 206
            val body = response.body ?: throw IOException("empty body")
            val total = if (resumed) existing + body.contentLength() else body.contentLength()
            var written = if (resumed) existing else 0L

            RandomAccessFile(dest, "rw").use { out ->
                out.seek(if (resumed) existing else 0L)
                val buffer = ByteArray(64 * 1024)
                val input = body.byteStream()

                var windowStart = System.currentTimeMillis()
                var windowBytes = 0L
                val limitBytesPerSec = rateLimitKbps.toLong() * 1024

                while (true) {
                    if (isCancelled()) throw CancelledException()
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    written += read
                    if (total > 0) onProgress(written.toFloat() / total)

                    if (limitBytesPerSec > 0) {
                        windowBytes += read
                        val elapsed = System.currentTimeMillis() - windowStart
                        if (windowBytes >= limitBytesPerSec) {
                            val sleepMs = 1000 - elapsed
                            if (sleepMs > 0) Thread.sleep(sleepMs)
                            windowStart = System.currentTimeMillis()
                            windowBytes = 0
                        }
                    }
                }
            }
        }
    }
}
