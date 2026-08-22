package com.lumenconnection.stream.metadata

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fixtures portadas dos testes do desktop (`src/download/download.rs`),
 * cobrindo o formato atual do embed (2026) e o legado.
 */
class SpotifyMetadataTest {

    @Test
    fun parseEmbedTracksCurrentFormat() {
        // Formato real do embed em 2026: title/subtitle/entityType (não name/artists).
        val html = """
        <html><body>
        <script id="__NEXT_DATA__" type="application/json">
        {"props":{"pageProps":{"state":{"data":{"entity":{"trackList":[
          {"uri":"spotify:track:111","title":"Earrings","subtitle":"Malcolm Todd","entityType":"track"},
          {"uri":"spotify:track:222","title":"Song B","subtitle":"Artist Two","entityType":"track"}
        ]}}}}}}
        </script>
        </body></html>
        """
        val items = SpotifyMetadata.parseEmbedTracks(html)
        assertEquals(2, items.size)
        assertEquals("Malcolm Todd - Earrings", items[0].label)
        assertEquals("ytsearch1:Malcolm Todd - Earrings", items[0].searchTarget)
        assertEquals("Artist Two - Song B", items[1].label)
    }

    @Test
    fun parseEmbedTracksLegacyNameArtists() {
        val html = """
        <html><body>
        <script id="__NEXT_DATA__" type="application/json">
        {"props":{"pageProps":{"state":{"data":{"entity":{"trackList":[
          {"name":"Song A","uri":"spotify:track:111","artists":[{"name":"Artist One"}]},
          {"name":"Song B","uri":"spotify:track:222","artists":[{"name":"Artist Two"},{"name":"Feat"}]}
        ]}}}}}}
        </script>
        </body></html>
        """
        val items = SpotifyMetadata.parseEmbedTracks(html)
        assertEquals(2, items.size)
        assertEquals("Artist One - Song A", items[0].label)
        assertEquals("Artist Two, Feat - Song B", items[1].label)
    }

    @Test
    fun parseEmbedTracksEmptyHtml() {
        assertEquals(0, SpotifyMetadata.parseEmbedTracks("<html><body>nothing</body></html>").size)
    }
}
