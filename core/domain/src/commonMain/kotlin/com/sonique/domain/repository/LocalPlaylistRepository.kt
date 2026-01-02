package com.sonique.domain.repository

import androidx.paging.PagingData
import com.sonique.domain.data.entities.LocalPlaylistEntity
import com.sonique.domain.data.entities.PairSongLocalPlaylist
import com.sonique.domain.data.entities.SetVideoIdEntity
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.domain.data.model.browse.playlist.PlaylistState
import com.sonique.domain.utils.FilterState
import com.sonique.domain.utils.LocalResource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface LocalPlaylistRepository {
    fun getLocalPlaylist(id: Long): Flow<LocalResource<LocalPlaylistEntity?>>

    fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>>

    suspend fun updateLocalPlaylistTracks(
        tracks: List<String>,
        id: Long,
    )

    suspend fun updateLocalPlaylistDownloadState(
        downloadState: Int,
        id: Long,
    )

    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(
        id: Long,
        syncState: Int,
    )

    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist)

    fun getPlaylistPairSongByListPosition(
        playlistId: Long,
        listPosition: List<Int>,
    ): Flow<List<PairSongLocalPlaylist>?>

    fun getPlaylistPairSongByOffset(
        playlistId: Long,
        offset: Int,
        filterState: FilterState,
    ): Flow<List<PairSongLocalPlaylist>?>

    fun getPlaylistPairSongByTime(
        playlistId: Long,
        filterState: FilterState,
        localDateTime: LocalDateTime,
    ): Flow<List<PairSongLocalPlaylist>?>

    fun getPlaylistPairOfSong(
        playlistId: Long,
        videoId: String,
    ): Flow<PairSongLocalPlaylist?>

    fun changePositionOfSongInPlaylist(
        playlistId: Long,
        videoId: String,
        newPosition: Int,
    ): Flow<String>

    fun downloadStateFlow(id: Long): Flow<Int>

    fun getAllDownloadingLocalPlaylists(): Flow<List<LocalPlaylistEntity>>

    fun listTrackFlow(id: Long): Flow<List<String>>

    fun getTracksPaging(
        id: Long,
        filter: FilterState,
    ): Flow<PagingData<Pair<SongEntity, PairSongLocalPlaylist>>>

    suspend fun getFullPlaylistTracks(id: Long): List<SongEntity>

    suspend fun getListTrackVideoId(id: Long): List<String>

    fun insertLocalPlaylist(
        localPlaylist: LocalPlaylistEntity,
        successMessage: String,
    ): Flow<LocalResource<String>>

    fun deleteLocalPlaylist(
        id: Long,
        successMessage: String,
    ): Flow<LocalResource<String>>

    fun updateTitleLocalPlaylist(
        id: Long,
        newTitle: String,
        updatedMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>>

    fun updateThumbnailLocalPlaylist(
        id: Long,
        newThumbnail: String,
        successMessage: String,
    ): Flow<LocalResource<String>>

    fun updateDownloadState(
        id: Long,
        downloadState: Int,
        successMessage: String,
    ): Flow<LocalResource<String>>

    fun syncYouTubePlaylistToLocalPlaylist(
        playlist: PlaylistState,
        tracks: List<Track>,
        successMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>>

    fun syncLocalPlaylistToYouTubePlaylist(
        playlistId: Long,
        successMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>>

    fun unsyncLocalPlaylist(
        id: Long,
        successMessage: String,
    ): Flow<LocalResource<String>>

    fun updateSyncState(
        id: Long,
        syncState: Int,
        successMessage: String,
    ): Flow<LocalResource<String>>

    fun updateYouTubePlaylistId(
        id: Long,
        youtubePlaylistId: String,
        successMessage: String,
    ): Flow<LocalResource<String>>

    fun updateListTrackSynced(id: Long): Flow<Boolean>

    fun addTrackToLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>>

    fun removeTrackFromLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>>

    fun getSuggestionsTrackForPlaylist(id: Long): Flow<LocalResource<Pair<String?, List<Track>>>>

    fun reloadSuggestionPlaylist(reloadParams: String): Flow<LocalResource<Pair<String?, List<Track>>>>

    fun getYouTubeSetVideoId(youtubePlaylistId: String): Flow<List<SetVideoIdEntity>>

    fun addYouTubePlaylistItem(
        youtubePlaylistId: String,
        videoId: String,
    ): Flow<LocalResource<String>>
}

