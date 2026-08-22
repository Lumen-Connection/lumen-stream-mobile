package com.lumenconnection.stream

import android.content.Context
import androidx.room.Room
import com.lumenconnection.stream.config.SettingsRepository
import com.lumenconnection.stream.db.AppDatabase

/**
 * Service locator simples (sem DI framework, espelhando a simplicidade do desktop).
 */
object Graph {
    lateinit var db: AppDatabase
        private set
    lateinit var settings: SettingsRepository
        private set

    fun init(context: Context) {
        db = Room.databaseBuilder(context, AppDatabase::class.java, "lumen-stream.db")
            .fallbackToDestructiveMigration()
            .build()
        settings = SettingsRepository(context.applicationContext)
    }
}
