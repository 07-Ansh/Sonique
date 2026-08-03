package com.sonique.data.parser.search

import com.sonique.domain.data.model.searchResult.songs.Artist
import com.sonique.domain.data.model.searchResult.songs.Thumbnail
import com.sonique.domain.data.model.searchResult.videos.VideosResult
import com.sonique.kotlinytmusicscraper.models.SongItem
import com.sonique.kotlinytmusicscraper.pages.SearchResult

internal fun parseSearchVideo(result: SearchResult): ArrayList<VideosResult> {
    val songsResult: ArrayList<VideosResult> = arrayListOf()
    result.items.forEach {
        val song = it as SongItem
        songsResult.add(
            VideosResult(
                artists =
                    song.artists.map { artistItem ->
                        Artist(
                            id = artistItem.id,
                            name = artistItem.name,
                        )
                    },
                category = "Video",
                duration = if (song.duration != null) {
                    val dur = song.duration!!
                    val m = dur / 60
                    val s = dur % 60
                    "${if (m < 10) "0$m" else "$m"}:${if (s < 10) "0$s" else "$s"}"
                } else "",
                durationSeconds = song.duration ?: 0,
                resultType = "Video",
                thumbnails = listOf(Thumbnail(306, Regex("([wh])120").replace(song.thumbnail, "$1544"), 544)),
                title = song.title,
                videoId = song.id,
                videoType = "Video",
                views = null,
                year = "",
            ),
        )
    }
    return songsResult
}

