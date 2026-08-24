package com.lumenconnection.stream.download

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lumenconnection.stream.R

/**
 * Porte de `friendly_error()` de `src/download/engine/ytdlp_util.rs`.
 *
 * O erro cru do yt-dlp fica salvo no banco; a tradução acontece na hora de
 * exibir, para que a mensagem acompanhe o idioma do aparelho.
 */
object FriendlyError {

    data class Friendly(val resId: Int, val arg: String? = null)

    fun isHttp403(stderr: String): Boolean {
        val low = stderr.lowercase()
        return low.contains("http error 403") || low.contains("403 forbidden")
    }

    fun map(stderr: String?): Friendly {
        val raw = stderr?.trim().orEmpty()
        if (raw.isEmpty()) return Friendly(R.string.status_failed)
        val low = raw.lowercase()

        val known: Int? = when {
            low.contains("private video") || low.contains("sign in to confirm") ->
                R.string.err_private

            low.contains("confirm your age") || low.contains("age-restricted") ||
                low.contains("age restricted") -> R.string.err_age

            low.contains("video unavailable") || low.contains("this video is not available") ->
                R.string.err_unavailable

            low.contains("requested format is not available") -> R.string.err_format

            (low.contains("unsupported url") || low.contains("do not open an issue") ||
                low.contains("is not a valid url")) &&
                (low.contains("spotify") || raw.contains("spotify.com") ||
                    raw.contains("open.spotify")) -> R.string.err_spotify

            low.contains("unsupported url") || low.contains("is not a valid url") ->
                R.string.err_unsupported

            isHttp403(raw) -> R.string.err_403

            low.contains("http error 404") -> R.string.err_404

            low.contains("getaddrinfo") || low.contains("failed to resolve") ||
                low.contains("unable to download webpage") ||
                low.contains("temporary failure in name resolution") ||
                low.contains("connection") -> R.string.err_network

            else -> null
        }

        if (known != null) return Friendly(known)

        // Sem correspondência: mostra a última linha útil do stderr, como o desktop.
        val last = raw.lineSequence().lastOrNull { it.isNotBlank() }?.trim().orEmpty()
        return Friendly(R.string.err_generic, last.take(180))
    }
}

@Composable
fun friendlyErrorText(stderr: String?): String {
    val f = FriendlyError.map(stderr)
    return if (f.arg != null) stringResource(f.resId, f.arg) else stringResource(f.resId)
}
