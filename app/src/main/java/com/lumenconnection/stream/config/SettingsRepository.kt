package com.lumenconnection.stream.config

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class Engine { AUTO, NEWPIPE, YTDLP }

/** Espelha o tema do desktop, que tem claro/escuro explícitos além do sistema. */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ENGINE = stringPreferencesKey("engine")
        val RATE_LIMIT_KBPS = intPreferencesKey("rate_limit_kbps")
        val CUSTOM_TREE_URI = stringPreferencesKey("custom_tree_uri")
        val SUBTITLES = booleanPreferencesKey("subtitles")
        val PLAYLIST = booleanPreferencesKey("playlist")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val COMPACT = booleanPreferencesKey("compact")
    }

    val engine: Flow<Engine> = context.dataStore.data.map {
        runCatching { Engine.valueOf(it[Keys.ENGINE] ?: Engine.AUTO.name) }.getOrDefault(Engine.AUTO)
    }
    val rateLimitKbps: Flow<Int> = context.dataStore.data.map { it[Keys.RATE_LIMIT_KBPS] ?: 0 }
    val customTreeUri: Flow<String?> = context.dataStore.data.map { it[Keys.CUSTOM_TREE_URI] }
    val subtitles: Flow<Boolean> = context.dataStore.data.map { it[Keys.SUBTITLES] ?: false }
    val playlist: Flow<Boolean> = context.dataStore.data.map { it[Keys.PLAYLIST] ?: false }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }
    val highContrast: Flow<Boolean> = context.dataStore.data.map { it[Keys.HIGH_CONTRAST] ?: false }
    val compact: Flow<Boolean> = context.dataStore.data.map { it[Keys.COMPACT] ?: false }

    suspend fun setEngine(value: Engine) =
        context.dataStore.edit { it[Keys.ENGINE] = value.name }

    suspend fun setRateLimitKbps(value: Int) =
        context.dataStore.edit { it[Keys.RATE_LIMIT_KBPS] = value.coerceAtLeast(0) }

    suspend fun setCustomTreeUri(value: String?) =
        context.dataStore.edit {
            if (value == null) it.remove(Keys.CUSTOM_TREE_URI) else it[Keys.CUSTOM_TREE_URI] = value
        }

    suspend fun setSubtitles(value: Boolean) =
        context.dataStore.edit { it[Keys.SUBTITLES] = value }

    suspend fun setPlaylist(value: Boolean) =
        context.dataStore.edit { it[Keys.PLAYLIST] = value }

    suspend fun setThemeMode(value: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME_MODE] = value.name }

    suspend fun setHighContrast(value: Boolean) =
        context.dataStore.edit { it[Keys.HIGH_CONTRAST] = value }

    suspend fun setCompact(value: Boolean) =
        context.dataStore.edit { it[Keys.COMPACT] = value }
}
