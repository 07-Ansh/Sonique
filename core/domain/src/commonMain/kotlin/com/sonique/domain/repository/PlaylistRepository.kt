package com.sonique.domain.repository

import com.sonique.domain.data.entities.ArtistEntity
import com.sonique.domain.data.entities.PlaylistEntity
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.data.entities.YourYouTubePlaylistList
import com.sonique.domain.data.model.browse.playlist.PlaylistBrowse
import com.sonique.domain.data.model.searchResult.playlists.PlaylistsResult
import com.sonique.domain.data.type.PlaylistType
import com.sonique.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface PlaylistRepository {
    fun getAllPlaylists(limit: Int): Flow<List<PlaylistEntity>>

    fun getPlaylist(id: String): Flow<PlaylistEntity?>

    fun getLikedPlaylists(): Flow<List<PlaylistEntity>>

    suspend fun insertPlaylist(playlistEntity: PlaylistEntity)

    suspend fun insertAndReplacePlaylist(playlistEntity: PlaylistEntity)

    suspend fun insertRadioPlaylist(playlistEntity: PlaylistEntity)

    suspend fun updatePlaylistLiked(
        playlistId: String,
        likeStatus: Int,
    )

    suspend fun updatePlaylistInLibrary(
        inLibrary: LocalDateTime,
        playlistId: String,
    )

    suspend fun updatePlaylistDownloadState(
        playlistId: String,
        downloadState: Int,
    )

    fun getAllDownloadedPlaylist(): Flow<List<PlaylistType>>

    fun getAllDownloadingPlaylist(): Flow<List<PlaylistType>>

    fun getRadio(
        radioId: String,
        defaultDescription: String,
        radioString: String,
        viewString: String,
        originalTrack: SongEntity? = null,
        artist: ArtistEntity? = null,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>>

    fun getRDATRadioData(
        radioId: String,
        viewString: String,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>>

    fun getFullPlaylistData(
        playlistId: String,
        viewString: String,
    ): Flow<Resource<PlaylistBrowse>>

    fun getPlaylistData(
        playlistId: String,
        viewString: String,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>>

    fun getLibraryPlaylist(): Flow<List<PlaylistsResult>?>

    fun getMixedForYou(): Flow<List<PlaylistsResult>?>

    fun updateYourYouTubePlaylistTitle(
        playlistId: String,
        newTitle: String,
    ): Flow<Resource<String>>

    suspend fun insertYourYouTubePlaylist(yourYouTubePlaylist: YourYouTubePlaylistList)

     
    fun getYourYouTubePlaylistList(
        emailPageId: String
    ): Flow<YourYouTubePlaylistList?>

    suspend fun deleteAllYourYouTubePlaylist()

}

