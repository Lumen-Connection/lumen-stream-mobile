package com.lumenconnection.stream.metadata

/**
 * Resolução de metadata via Spotify para nomes limpos de faixas de áudio,
 * como o Lumen Stream Desktop faz.
 *
 * TODO(fase 1.10): portar a lógica de `src/download` do repo desktop
 * (https://github.com/Lumen-Connection/lumen-stream) — consultar a fonte Rust
 * para replicar o mesmo fluxo de busca/normalização de título e artista.
 */
object SpotifyMetadata {

    data class Track(val title: String, val artist: String)

    /** Limpeza básica de título enquanto a integração completa não chega. */
    fun cleanTitle(raw: String): String =
        raw
            .replace(Regex("(?i)\\s*\\((official\\s*)?(music\\s*)?(video|audio|lyric[s]?|visualizer|hq|hd)\\)"), "")
            .replace(Regex("(?i)\\s*\\[(official\\s*)?(music\\s*)?(video|audio|lyric[s]?|visualizer|hq|hd)]"), "")
            .trim()
}
