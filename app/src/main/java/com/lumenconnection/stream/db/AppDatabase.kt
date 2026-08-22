package com.lumenconnection.stream.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MediaItem::class, DownloadTask::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun downloadDao(): DownloadDao
}
