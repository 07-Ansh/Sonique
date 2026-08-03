package com.sonique.data.parser.search

import com.sonique.domain.data.model.searchResult.songs.Album
import com.sonique.domain.data.model.searchResult.songs.SongsResult
import com.sonique.domain.data.model.searchResult.songs.Thumbnail
import com.sonique.kotlinytmusicscraper.models.SongItem
import com.sonique.kotlinytmusicscraper.pages.SearchResult

internal fun parseSearchSong(result: SearchResult): ArrayList<SongsResult> {
    val songsResult: ArrayList<SongsResult> = arrayListOf()
    result.items.forEach {
        val song = it as SongItem
        songsResult.add(
            SongsResult(
                album =
                    if (song.album != null) {
                        Album(
                            id = song.album!!.id,
                            name = song.album!!.name,
                        )
                    } else {
                        null
                    },
                artists =
                    song.artists.map { artistItem ->
                        com.sonique.domain.data.model.searchResult.songs.Artist(
                            id = artistItem.id,
                            name = artistItem.name,
                        )
                    },
                category = "Song",
                duration = if (song.duration != null) {
                    val dur = song.duration!!
                    val m = dur / 60
                    val s = dur % 60
                    "${if (m < 10) "0$m" else "$m"}:${if (s < 10) "0$s" else "$s"}"
                } else "",
                durationSeconds = song.duration ?: 0,
                feedbackTokens = null,
                isExplicit = song.explicit,
                resultType = "Song",
                thumbnails = listOf(Thumbnail(544, Regex("([wh])120").replace(song.thumbnail, "$1544"), 544)),
                title = song.title,
                videoId = song.id,
                videoType = "Song",
                year = "",
            ),
        )
    }
    return songsResult
}

