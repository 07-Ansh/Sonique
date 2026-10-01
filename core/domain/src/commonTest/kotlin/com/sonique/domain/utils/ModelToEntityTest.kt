package com.sonique.domain.utils

import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.domain.data.model.metadata.Line
import com.sonique.domain.data.model.metadata.Lyrics
import com.sonique.domain.data.model.searchResult.songs.Album
import com.sonique.domain.data.model.searchResult.songs.Artist
import com.sonique.domain.data.model.searchResult.songs.Thumbnail
import com.sonique.domain.extension.toGenericMediaItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelToEntityTest {

    @Test
    fun testConnectArtists_multipleArtists() {
        val artists = listOf("Taylor Swift", "Post Malone", "Florence + The Machine")
        val result = artists.connectArtists()
        assertEquals("Taylor Swift, Post Malone, Florence + The Machine", result)
    }

    @Test
    fun testConnectArtists_singleArtist() {
        val artists = listOf("The Weeknd")
        assertEquals("The Weeknd", artists.connectArtists())
    }

    @Test
    fun testConnectArtists_emptyList() {
        val artists = emptyList<String>()
        assertEquals("", artists.connectArtists())
    }

    @Test
    fun testTrackToSongEntity_preservesCoreFields() {
        val track = Track(
            album = Album(id = "album123", name = "Dawn FM"),
            artists = listOf(Artist(id = "artist123", name = "The Weeknd")),
            duration = "03:30",
            durationSeconds = 210,
            isAvailable = true,
            isExplicit = false,
            likeStatus = "INDIFFERENT",
            thumbnails = listOf(
                Thumbnail(height = 120, url = "https://i.ytimg.com/vi/vid123/default.jpg", width = 120),
                Thumbnail(height = 544, url = "https://i.ytimg.com/vi/vid123/maxresdefault.jpg", width = 544),
            ),
            title = "Sacrifice",
            videoId = "vid123",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            category = null,
            feedbackTokens = null,
            resultType = null,
            year = "2022",
        )

        val songEntity = track.toSongEntity()
        assertEquals("vid123", songEntity.videoId)
        assertEquals("Sacrifice", songEntity.title)
        assertEquals("album123", songEntity.albumId)
        assertEquals("Dawn FM", songEntity.albumName)
        assertEquals(listOf("artist123"), songEntity.artistId)
        assertEquals(listOf("The Weeknd"), songEntity.artistName)
        assertEquals(210, songEntity.durationSeconds)
        assertTrue(songEntity.isAvailable)
    }

    @Test
    fun testTrackToGenericMediaItem_enforcesHttpsAndReplacesThumbnailResolution() {
        val track = Track(
            album = Album(id = "album456", name = "After Hours"),
            artists = listOf(Artist(id = "art1", name = "The Weeknd")),
            duration = "03:20",
            durationSeconds = 200,
            isAvailable = true,
            isExplicit = true,
            likeStatus = "LIKE",
            thumbnails = listOf(
                Thumbnail(height = 120, url = "https://lh3.googleusercontent.com/w120-h120", width = 120),
            ),
            title = "Blinding Lights",
            videoId = "bl123",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            category = null,
            feedbackTokens = null,
            resultType = null,
            year = "2020",
        )

        val mediaItem = track.toGenericMediaItem()
        assertEquals("bl123", mediaItem.mediaId)
        assertEquals("Blinding Lights", mediaItem.metadata.title)
        assertEquals("The Weeknd", mediaItem.metadata.artist)
        assertEquals("After Hours", mediaItem.metadata.albumTitle)
        // Check that w120 is scaled up to 544
        assertTrue(mediaItem.metadata.artworkUri?.contains("544") == true)
    }

    @Test
    fun testTrackToGenericMediaItem_fallbackThumbnailEnforcesHttps() {
        val track = Track(
            album = null,
            artists = null,
            duration = "02:45",
            durationSeconds = 165,
            isAvailable = true,
            isExplicit = false,
            likeStatus = null,
            thumbnails = null,
            title = "Fallback Song",
            videoId = "test_vid_999",
            videoType = "",
            category = null,
            feedbackTokens = null,
            resultType = null,
            year = null,
        )

        val mediaItem = track.toGenericMediaItem()
        assertNotNull(mediaItem.metadata.artworkUri)
        assertTrue(mediaItem.metadata.artworkUri!!.startsWith("https://"))
        assertTrue(mediaItem.metadata.artworkUri!!.contains("test_vid_999"))
    }

    @Test
    fun testRichSyncToSyncedLyrics_stripsTimingTags() {
        val richLyrics = Lyrics(
            error = false,
            lines = listOf(
                Line(
                    endTimeMs = "2000",
                    startTimeMs = "1000",
                    words = "<00:01.00>Never <00:01.50>gonna <00:01.80>give",
                    syllables = null,
                ),
                Line(
                    endTimeMs = "3500",
                    startTimeMs = "2100",
                    words = "<00:02.10>you <00:02.50>up",
                    syllables = null,
                ),
            ),
            syncType = "RICH_SYNCED",
        )

        val synced = richLyrics.toSyncedLyrics()
        assertEquals("LINE_SYNCED", synced.syncType)
        val lines = synced.lines
        assertNotNull(lines)
        assertEquals(2, lines.size)
        assertEquals("Never gonna give", lines[0].words)
        assertEquals("you up", lines[1].words)
    }
}
