package com.sonique.app.utils

import com.sonique.domain.utils.cleanVideoTitle
import com.sonique.domain.utils.isVideoThumbnailUrl
import com.sonique.domain.utils.toHighResThumbnailUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThumbnailUrlEnhancerTest {

    @Test
    fun testGoogleUserContentUrlUpgrade() {
        val lowResUrl = "https://lh3.googleusercontent.com/a/ACg8ocLq=w120-h120-l90-rj"
        val highRes = lowResUrl.toHighResThumbnailUrl(1200)
        assertEquals("https://lh3.googleusercontent.com/a/ACg8ocLq=w1200-h1200-l90-rj", highRes)

        val s544Url = "https://lh3.googleusercontent.com/some_hash=s544-c-k-no"
        val highResS = s544Url.toHighResThumbnailUrl(1200)
        assertEquals("https://lh3.googleusercontent.com/some_hash=w1200-h1200-l90-rj", highResS)

        val slashSUrl = "https://lh3.googleusercontent.com/s60/photo.jpg"
        val highResSlash = slashSUrl.toHighResThumbnailUrl(1200)
        assertEquals("https://lh3.googleusercontent.com/s1200/photo.jpg", highResSlash)
    }

    @Test
    fun testGgphtUrlUpgrade() {
        val lowRes = "https://yt3.ggpht.com/ytc/AIdro_k=s88-c-k-c0x00ffffff-no-rj"
        val highRes = lowRes.toHighResThumbnailUrl(1200)
        assertEquals("https://yt3.ggpht.com/ytc/AIdro_k=w1200-h1200-l90-rj", highRes)
    }

    @Test
    fun testYouTubeVideoThumbnailUpgrade() {
        val hqDefault = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
        val maxRes = hqDefault.toHighResThumbnailUrl(1200)
        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg", maxRes)

        val defaultRes = hqDefault.toHighResThumbnailUrl(544)
        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", defaultRes)

        val mqDefault = "https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg"
        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg", mqDefault.toHighResThumbnailUrl(1200))
    }

    @Test
    fun testIsVideoThumbnailUrl() {
        assertTrue(isVideoThumbnailUrl("https://i.ytimg.com/vi/xyz/hqdefault.jpg"))
        assertTrue(isVideoThumbnailUrl("https://i.ytimg.com/vi/xyz/maxresdefault.jpg"))
        assertTrue(isVideoThumbnailUrl("https://i.ytimg.com/vi/xyz/hq720.jpg"))
        assertFalse(isVideoThumbnailUrl("https://lh3.googleusercontent.com/a/ACg8ocLq=w120-h120-l90-rj"))
        assertFalse(isVideoThumbnailUrl(null))
        assertFalse(isVideoThumbnailUrl(""))
    }

    @Test
    fun testCleanVideoTitle() {
        assertEquals("Blinding Lights", cleanVideoTitle("Blinding Lights (Official Video)"))
        assertEquals("Starboy", cleanVideoTitle("Starboy [Official Music Video]"))
        assertEquals("Save Your Tears", cleanVideoTitle("Save Your Tears (Remix) [Lyric Video]"))
        assertEquals("Stay", cleanVideoTitle("Stay | 4K Visualizer"))
        assertEquals("Normal Title", cleanVideoTitle("Normal Title"))
    }

    @Test
    fun testSongMatchingAndSimilarity() {
        val match = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Coldplay - Sparks (Live in Madrid)",
            videoArtist = "Coldplay",
            candidateTitle = "Sparks",
            candidateArtists = listOf("Coldplay"),
        )
        assertTrue(match)

        val mismatchWrongSong = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Coldplay - Sparks",
            videoArtist = "Coldplay",
            candidateTitle = "Yellow",
            candidateArtists = listOf("Coldplay"),
        )
        assertFalse(mismatchWrongSong)

        val mismatchWrongArtist = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Hello",
            videoArtist = "Adele",
            candidateTitle = "Hello",
            candidateArtists = listOf("Lionel Richie"),
        )
        assertFalse(mismatchWrongArtist)
    }
}
