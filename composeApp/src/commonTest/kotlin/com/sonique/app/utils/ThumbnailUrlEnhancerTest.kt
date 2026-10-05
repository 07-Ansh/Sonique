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
    fun testExtractSongTitleAndArtist() {
        val (title1, artist1) = com.sonique.domain.utils.extractSongTitleAndArtist(
            "Alan Walker - Faded",
            "AlanWalkerVEVO",
        )
        assertEquals("Faded", title1)
        assertEquals("Alan Walker", artist1)

        val (title2, artist2) = com.sonique.domain.utils.extractSongTitleAndArtist(
            "Twenty One Pilots - Stressed Out [Official Video]",
            "Fueled By Ramen",
        )
        assertEquals("Stressed Out", title2)
        assertEquals("Twenty One Pilots", artist2)

        val (title3, artist3) = com.sonique.domain.utils.extractSongTitleAndArtist(
            "Starboy (Official Music Video)",
            "TheWeekndVEVO",
        )
        assertEquals("Starboy", title3)
        assertEquals("TheWeeknd", artist3)
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

        val matchFaded = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Alan Walker - Faded",
            videoArtist = "AlanWalkerVEVO",
            candidateTitle = "Faded",
            candidateArtists = listOf("Alan Walker"),
        )
        assertTrue(matchFaded)

        val matchStressedOut = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Twenty One Pilots - Stressed Out [Official Video]",
            videoArtist = "Fueled By Ramen",
            candidateTitle = "Stressed Out",
            candidateArtists = listOf("twenty one pilots"),
        )
        assertTrue(matchStressedOut)

        val matchLunch = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Billie Eilish - LUNCH (Official Music Video)",
            videoArtist = "BillieEilishVEVO",
            candidateTitle = "LUNCH",
            candidateArtists = listOf("Billie Eilish"),
        )
        assertTrue(matchLunch)

        val matchKesariya = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Arijit Singh, Pritam - Kesariya (From \"Brahmastra\")",
            videoArtist = "SonyMusicIndiaVEVO",
            candidateTitle = "Kesariya",
            candidateArtists = listOf("Pritam", "Arijit Singh"),
        )
        assertTrue(matchKesariya)

        // Real-world Bollywood / Record label video song titles
        val matchTumHoToh = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Tum Ho Toh (Official Video) | Saiyaara | Ahan Shetty, Pooja Hegde | Vishal Mishra, Hansika Pareek",
            videoArtist = "T-Series",
            candidateTitle = "Tum Ho Toh",
            candidateArtists = listOf("Vishal Mishra", "Hansika Pareek"),
        )
        assertTrue(matchTumHoToh)

        val matchVaaroon = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Vaaroon Forever (Official Video) | Anand Bhaskar, Shreya Ghoshal | Mirzapur Season 3",
            videoArtist = "Sony Music India",
            candidateTitle = "Vaaroon Forever",
            candidateArtists = listOf("Anand Bhaskar", "Shreya Ghoshal"),
        )
        assertTrue(matchVaaroon)

        val matchKesariyaReal = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Kesariya - Brahmāstra | Ranbir Kapoor | Alia Bhatt | Pritam | Arijit Singh | Amitabh B",
            videoArtist = "Sony Music India",
            candidateTitle = "Kesariya",
            candidateArtists = listOf("Pritam", "Arijit Singh"),
        )
        assertTrue(matchKesariyaReal)

        // Label video title without singer mentioned in title
        val matchLabelSongNoSinger = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Tum Ho Toh (From \"Saiyaara\")",
            videoArtist = "T-Series",
            candidateTitle = "Tum Ho Toh",
            candidateArtists = listOf("Vishal Mishra"),
        )
        assertTrue(matchLabelSongNoSinger)

        // Title - Artist reversed format
        val matchFlowersReversed = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Flowers - Miley Cyrus",
            videoArtist = "Miley Cyrus",
            candidateTitle = "Flowers",
            candidateArtists = listOf("Miley Cyrus"),
        )
        assertTrue(matchFlowersReversed)

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

        val mismatchHoldOn = com.sonique.domain.utils.isSongMatch(
            videoTitle = "Hold On",
            videoArtist = "Chord Overstreet",
            candidateTitle = "Hold On",
            candidateArtists = listOf("Justin Bieber"),
        )
        assertFalse(mismatchHoldOn)
    }
}
