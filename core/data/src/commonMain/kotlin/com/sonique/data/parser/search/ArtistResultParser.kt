package com.sonique.data.parser.search

import com.sonique.domain.data.model.searchResult.artists.ArtistsResult
import com.sonique.domain.data.model.searchResult.songs.Thumbnail
import com.sonique.kotlinytmusicscraper.models.ArtistItem
import com.sonique.kotlinytmusicscraper.pages.SearchResult

internal fun parseSearchArtist(result: SearchResult): ArrayList<ArtistsResult> {
    val artistsResult: ArrayList<ArtistsResult> = arrayListOf()
    result.items.forEach {
        val artist = it as ArtistItem
        artistsResult.add(
            ArtistsResult(
                artist = artist.title,
                browseId = artist.id,
                category = "Artist",
                radioId = artist.radioEndpoint?.playlistId ?: "",
                resultType = "Artist",
                shuffleId = artist.shuffleEndpoint?.playlistId ?: "",
                thumbnails = listOf(Thumbnail(544, Regex("([wh])120").replace(artist.thumbnail, "$1544"), 544)),
            ),
        )
    }
    return artistsResult
}

