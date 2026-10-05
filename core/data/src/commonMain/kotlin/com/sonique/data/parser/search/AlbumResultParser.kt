package com.sonique.data.parser.search

import com.sonique.domain.data.model.searchResult.albums.AlbumsResult
import com.sonique.domain.data.model.searchResult.songs.Artist
import com.sonique.domain.data.model.searchResult.songs.Thumbnail
import com.sonique.domain.utils.toHighResThumbnailUrl
import com.sonique.kotlinytmusicscraper.models.AlbumItem
import com.sonique.kotlinytmusicscraper.pages.SearchResult

internal fun parseSearchAlbum(result: SearchResult): ArrayList<AlbumsResult> {
    val albumsResult: ArrayList<AlbumsResult> = arrayListOf()
    result.items.forEach {
        val album = it as AlbumItem
        albumsResult.add(
            AlbumsResult(
                artists =
                    album.artists?.map { artistItem ->
                        Artist(
                            id = artistItem.id,
                            name = artistItem.name,
                        )
                    } ?: listOf(),
                browseId = album.browseId,
                category = "Album",
                duration = "",
                isExplicit = false,
                resultType = "Album",
                thumbnails = listOf(Thumbnail(1200, album.thumbnail.toHighResThumbnailUrl(1200), 1200)),
                title = album.title,
                type = "Album",
                year = album.year.toString(),
            ),
        )
    }
    return albumsResult
}

