package com.lumenconnection.stream.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MediaItem>>

    @Query(
        "SELECT * FROM media WHERE title LIKE '%' || :query || '%' " +
            "OR uploader LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' " +
            "ORDER BY createdAt DESC"
    )
    fun search(query: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media WHERE favorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun byId(id: Long): MediaItem?

    @Insert
    suspend fun insert(item: MediaItem): Long

    @Update
    suspend fun update(item: MediaItem)

    @Delete
    suspend fun delete(item: MediaItem)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadTask>>

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextQueued(): DownloadTask?

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun byId(id: Long): DownloadTask?

    @Insert
    suspend fun insert(task: DownloadTask): Long

    @Update
    suspend fun update(task: DownloadTask)

    @Query("UPDATE downloads SET status = :status, error = :error WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, error: String? = null)

    @Query("UPDATE downloads SET progress = :progress, status = :status, engine = :engine, title = :title WHERE id = :id")
    suspend fun setProgress(id: Long, progress: Float, status: String, engine: String?, title: String?)

    @Query("DELETE FROM downloads WHERE status IN ('DONE','FAILED','CANCELLED')")
    suspend fun clearFinished()
}
