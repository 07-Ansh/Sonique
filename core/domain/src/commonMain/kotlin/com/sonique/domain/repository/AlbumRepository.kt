package com.sonique.domain.repository

import com.sonique.domain.data.entities.AlbumEntity
import com.sonique.domain.data.entities.FollowedArtistSingleAndAlbum
import com.sonique.domain.data.model.browse.album.AlbumBrowse
import com.sonique.domain.data.model.searchResult.albums.AlbumsResult
import com.sonique.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface AlbumRepository {
    fun getAllAlbums(limit: Int): Flow<List<AlbumEntity>>

    fun getAlbum(id: String): Flow<AlbumEntity?>

    fun getAlbumAsFlow(id: String): Flow<AlbumEntity?>

    fun getLikedAlbums(): Flow<List<AlbumEntity>>

    fun insertAlbum(albumEntity: AlbumEntity): Flow<Long>

    suspend fun updateAlbumLiked(
        albumId: String,
        likeStatus: Int,
    )

    suspend fun updateAlbumInLibrary(
        inLibrary: LocalDateTime,
        albumId: String,
    )

    suspend fun updateAlbumDownloadState(
        albumId: String,
        downloadState: Int,
    )

    suspend fun insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum: FollowedArtistSingleAndAlbum)

    suspend fun deleteFollowedArtistSingleAndAlbum(channelId: String)

    suspend fun getAllFollowedArtistSingleAndAlbums(): Flow<List<FollowedArtistSingleAndAlbum>?>

    suspend fun getFollowedArtistSingleAndAlbum(channelId: String): Flow<FollowedArtistSingleAndAlbum?>

    fun getAlbumData(browseId: String): Flow<Resource<AlbumBrowse>>

    fun getAlbumMore(
        browseId: String,
        params: String,
    ): Flow<Pair<String, List<AlbumsResult>>?>
}

