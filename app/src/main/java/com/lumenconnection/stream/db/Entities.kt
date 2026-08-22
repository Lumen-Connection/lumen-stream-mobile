package com.lumenconnection.stream.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val uploader: String? = null,
    val sourceUrl: String,
    val contentUri: String,
    val kind: String,              // "video" | "audio"
    val mimeType: String? = null,
    val thumbnailUrl: String? = null,
    val durationSec: Long? = null,
    val tags: String = "",
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

object DownloadStatus {
    const val QUEUED = "QUEUED"
    const val RUNNING = "RUNNING"
    const val PROCESSING = "PROCESSING"
    const val DONE = "DONE"
    const val FAILED = "FAILED"
    const val CANCELLED = "CANCELLED"
}

@Entity(tableName = "downloads")
data class DownloadTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String? = null,
    val format: String,            // DownloadFormat.name
    val status: String = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val error: String? = null,
    val engine: String? = null,    // "newpipe" | "yt-dlp"
    val createdAt: Long = System.currentTimeMillis(),
)
