package com.sonique.data.db

import DatabaseDao
import com.sonique.domain.data.entities.AlbumEntity
import com.sonique.domain.data.entities.ArtistEntity
import com.sonique.domain.data.entities.EpisodeEntity
import com.sonique.domain.data.entities.FollowedArtistSingleAndAlbum
import com.sonique.domain.data.entities.GoogleAccountEntity
import com.sonique.domain.data.entities.LocalPlaylistEntity
import com.sonique.domain.data.entities.LyricsEntity
import com.sonique.domain.data.entities.NewFormatEntity
import com.sonique.domain.data.entities.NotificationEntity
import com.sonique.domain.data.entities.PairSongLocalPlaylist
import com.sonique.domain.data.entities.PlaylistEntity
import com.sonique.domain.data.entities.PodcastsEntity
import com.sonique.domain.data.entities.QueueEntity
import com.sonique.domain.data.entities.SearchHistory
import com.sonique.domain.data.entities.SetVideoIdEntity
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.data.entities.SongInfoEntity
import com.sonique.domain.data.entities.TranslatedLyricsEntity
import com.sonique.domain.data.entities.YourYouTubePlaylistList
import com.sonique.domain.extension.now
import com.sonique.domain.utils.FilterState
import kotlinx.datetime.LocalDateTime

internal class LocalDataSource(
    private val databaseDao: DatabaseDao,
) {
    suspend fun checkpoint() = databaseDao.checkpoint()

    suspend fun getAllRecentData() = databaseDao.getAllRecentData()

    suspend fun getAllDownloadedPlaylist() = databaseDao.getAllDownloadedPlaylist()

    suspend fun getAllDownloadingPlaylist() = databaseDao.getAllDownloadingPlaylist()

    suspend fun getSearchHistory() = databaseDao.getSearchHistory()

    suspend fun deleteSearchHistory() = databaseDao.deleteSearchHistory()

    suspend fun insertSearchHistory(searchHistory: SearchHistory) = databaseDao.insertSearchHistory(searchHistory)

    suspend fun getAllSongs(limit: Int) = databaseDao.getAllSongs(limit)

    suspend fun getRecentSongs(
        limit: Int,
        offset: Int,
    ) = databaseDao.getRecentSongs(limit, offset)

    suspend fun getSongByListVideoId(
        primaryKeyList: List<String>,
        offset: Int,
    ) = databaseDao.getSongByListVideoId(primaryKeyList, offset)

    suspend fun getCanvasSong(max: Int) = databaseDao.getCanvasSong(max)

    suspend fun getSongByListVideoIdFull(primaryKeyList: List<String>) = databaseDao.getSongByListVideoIdFull(primaryKeyList)

    suspend fun getDownloadedSongs(
        limit: Int,
        offset: Int,
    ) = databaseDao.getDownloadedSongs(
        limit,
        offset,
    )

    fun getDownloadedVideoIdListFromListVideoIdAsFlow(listVideoId: List<String>) = databaseDao.getDownloadedVideoIdByListVideoId(listVideoId)

    suspend fun getDownloadingSongs(
        limit: Int,
        offset: Int,
    ) = databaseDao.getDownloadingSongs(
        limit,
        offset,
    )

    suspend fun getLikedSongs(
        limit: Int,
        offset: Int,
    ) = databaseDao.getLikedSongs(
        limit,
        offset,
    )

    suspend fun getSong(videoId: String) = databaseDao.getSong(videoId)

    fun getSongAsFlow(videoId: String) = databaseDao.getSongAsFlow(videoId)

    suspend fun insertSong(song: SongEntity) = databaseDao.insertSong(song)

    suspend fun updateThumbnailsSongEntity(
        thumbnail: String,
        videoId: String,
    ) = databaseDao.updateThumbnailsSongEntity(thumbnail, videoId)

    suspend fun updateListenCount(videoId: String) = databaseDao.updateTotalPlayTime(videoId)

    suspend fun resetTotalPlayTime(videoId: String) = databaseDao.resetTotalPlayTime(videoId)

    suspend fun updateCanvasUrl(
        videoId: String,
        canvasUrl: String,
    ) = databaseDao.updateCanvasUrl(videoId, canvasUrl)

    suspend fun updateCanvasThumbUrl(
        videoId: String,
        canvasThumbUrl: String,
    ) = databaseDao.updateCanvasThumbUrl(videoId, canvasThumbUrl)

    suspend fun updateLiked(
        liked: Int,
        videoId: String,
    ) = databaseDao.updateLiked(liked, videoId)

    suspend fun updateDurationSeconds(
        durationSeconds: Int,
        videoId: String,
    ) = databaseDao.updateDurationSeconds(durationSeconds, videoId)

    suspend fun updateSongInLibrary(
        inLibrary: LocalDateTime,
        videoId: String,
    ) = databaseDao.updateSongInLibrary(inLibrary, videoId)

    fun getMostPlayedSongs() = databaseDao.getMostPlayedSongs()

    suspend fun updateDownloadState(
        downloadState: Int,
        videoId: String,
    ) = databaseDao.updateDownloadState(downloadState, videoId)

    suspend fun getAllArtists(limit: Int) = databaseDao.getAllArtists(limit)

    suspend fun insertArtist(artist: ArtistEntity) = databaseDao.insertArtist(artist)

    suspend fun updateArtistImage(
        channelId: String,
        thumbnails: String,
    ) = databaseDao.updateArtistImage(channelId, thumbnails)

    suspend fun updateFollowed(
        followed: Int,
        channelId: String,
    ) = databaseDao.updateFollowed(followed, channelId)

    suspend fun getArtist(channelId: String) = databaseDao.getArtist(channelId)

    suspend fun getFollowedArtists(
        limit: Int,
        offset: Int,
    ) = databaseDao.getFollowedArtists(
        limit,
        offset,
    )

    suspend fun updateArtistInLibrary(
        inLibrary: LocalDateTime,
        channelId: String,
    ) = databaseDao.updateArtistInLibrary(inLibrary, channelId)

    suspend fun getAllAlbums(limit: Int) = databaseDao.getAllAlbums(limit)

    suspend fun insertAlbum(album: AlbumEntity) = databaseDao.insertAlbum(album)

    suspend fun updateAlbumLiked(
        liked: Int,
        albumId: String,
    ) = databaseDao.updateAlbumLiked(liked, albumId)

    suspend fun getAlbum(albumId: String) = databaseDao.getAlbum(albumId)

    fun getAlbumAsFlow(albumId: String) = databaseDao.getAlbumAsFlow(albumId)

    suspend fun getLikedAlbums(
        limit: Int,
        offset: Int,
    ) = databaseDao.getLikedAlbums(
        limit,
        offset,
    )

    suspend fun updateAlbumInLibrary(
        inLibrary: LocalDateTime,
        albumId: String,
    ) = databaseDao.updateAlbumInLibrary(inLibrary, albumId)

    suspend fun updateAlbumDownloadState(
        downloadState: Int,
        albumId: String,
    ) = databaseDao.updateAlbumDownloadState(downloadState, albumId)

    suspend fun getAllPlaylists(limit: Int) = databaseDao.getAllPlaylists(limit)

    suspend fun insertPlaylist(playlist: PlaylistEntity) = databaseDao.insertPlaylist(playlist)

    suspend fun insertAndReplacePlaylist(playlist: PlaylistEntity) = databaseDao.insertAndReplacePlaylist(playlist)

    suspend fun insertRadioPlaylist(playlist: PlaylistEntity) = databaseDao.insertRadioPlaylist(playlist)

    suspend fun updatePlaylistLiked(
        liked: Int,
        playlistId: String,
    ) = databaseDao.updatePlaylistLiked(liked, playlistId)

    suspend fun getPlaylist(playlistId: String) = databaseDao.getPlaylist(playlistId)

    suspend fun getLikedPlaylists(
        limit: Int,
        offset: Int,
    ) = databaseDao.getLikedPlaylists(
        limit,
        offset,
    )

    suspend fun updatePlaylistInLibrary(
        inLibrary: LocalDateTime,
        playlistId: String,
    ) = databaseDao.updatePlaylistInLibrary(inLibrary, playlistId)

    suspend fun updatePlaylistDownloadState(
        downloadState: Int,
        playlistId: String,
    ) = databaseDao.updatePlaylistDownloadState(downloadState, playlistId)

    suspend fun getAllLocalPlaylists(
        limit: Int,
        offset: Int,
    ) = databaseDao.getAllLocalPlaylists(
        limit,
        offset,
    )

    suspend fun getAllDownloadingLocalPlaylists(
        limit: Int,
        offset: Int,
    ) = databaseDao.getAllDownloadingLocalPlaylists(
        limit,
        offset,
    )

    suspend fun getLocalPlaylist(id: Long) = databaseDao.getLocalPlaylist(id)

    suspend fun insertLocalPlaylist(localPlaylist: LocalPlaylistEntity) = databaseDao.insertLocalPlaylist(localPlaylist)

    suspend fun deleteLocalPlaylist(id: Long) = databaseDao.deleteLocalPlaylist(id)

    suspend fun updateLocalPlaylistTitle(
        title: String,
        id: Long,
    ) = databaseDao.updateLocalPlaylistTitle(title, id)

    suspend fun updateLocalPlaylistThumbnail(
        thumbnail: String,
        id: Long,
    ) = databaseDao.updateLocalPlaylistThumbnail(thumbnail, id)

    suspend fun updateLocalPlaylistTracks(
        tracks: List<String>,
        id: Long,
    ) = databaseDao.updateLocalPlaylistTracks(tracks, id)

    suspend fun updateLocalPlaylistInLibrary(
        inLibrary: LocalDateTime,
        id: Long,
    ) = databaseDao.updateLocalPlaylistInLibrary(inLibrary, id)

    suspend fun updateLocalPlaylistDownloadState(
        downloadState: Int,
        id: Long,
    ) = databaseDao.updateLocalPlaylistDownloadState(downloadState, id)

    suspend fun getDownloadedLocalPlaylists(
        limit: Int,
        offset: Int,
    ) = databaseDao.getDownloadedLocalPlaylists(
        limit,
        offset,
    )

    suspend fun updateLocalPlaylistYouTubePlaylistId(
        id: Long,
        ytId: String?,
    ) = databaseDao.updateLocalPlaylistYouTubePlaylistId(id, ytId)

    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(
        id: Long,
        syncState: Int,
    ) = databaseDao.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)

    fun getDownloadStateFlowOfLocalPlaylist(id: Long) = databaseDao.getDownloadStateFlowOfLocalPlaylist(id)

    fun getListTracksFlowOfLocalPlaylist(id: Long) = databaseDao.getListTracksFlowOfLocalPlaylist(id)

    suspend fun getSavedLyrics(videoId: String) = databaseDao.getLyrics(videoId)

    suspend fun insertLyrics(lyrics: LyricsEntity) = databaseDao.insertLyrics(lyrics)

    suspend fun getPreparingSongs(
        limit: Int,
        offset: Int,
    ) = databaseDao.getPreparingSongs(
        limit,
        offset,
    )

    suspend fun insertNewFormat(format: NewFormatEntity) = databaseDao.insertNewFormat(format)

    suspend fun getNewFormat(videoId: String) = databaseDao.getNewFormat(videoId)

    suspend fun updateNewFormat(newFormatEntity: NewFormatEntity) = databaseDao.updateNewFormat(newFormatEntity)

    suspend fun getNewFormatAsFlow(videoId: String) = databaseDao.getNewFormatAsFlow(videoId)

    suspend fun insertSongInfo(songInfo: SongInfoEntity) = databaseDao.insertSongInfo(songInfo)

    suspend fun getSongInfo(videoId: String) = databaseDao.getSongInfo(videoId)

    suspend fun recoverQueue(queueEntity: QueueEntity) = databaseDao.recoverQueue(queueEntity)

    suspend fun getQueue() = databaseDao.getQueue()

    suspend fun deleteQueue() = databaseDao.deleteQueue()

    suspend fun getLocalPlaylistByYoutubePlaylistId(playlistId: String) = databaseDao.getLocalPlaylistByYoutubePlaylistId(playlistId)

    suspend fun insertSetVideoId(setVideoIdEntity: SetVideoIdEntity) = databaseDao.insertSetVideoId(setVideoIdEntity)

    suspend fun getSetVideoId(videoId: String) = databaseDao.getSetVideoId(videoId)

    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) =
        databaseDao.insertPairSongLocalPlaylist(pairSongLocalPlaylist)

    suspend fun unsyncLocalPlaylist(id: Long) = databaseDao.unsyncLocalPlaylist(id)

    suspend fun getPlaylistPairOfSong(
        videoId: String,
        localPlaylistId: Long,
    ) = databaseDao.getPlaylistPairOfSong(videoId, localPlaylistId)

    suspend fun getPlaylistPairSong(
        playlistId: Long,
        limit: Int,
        offset: Int,
    ) = databaseDao.getPlaylistPairSong(
        playlistId,
        limit,
        offset,
    )

    suspend fun getPlaylistPairSongByListPosition(
        playlistId: Long,
        listPosition: List<Int>,
    ) = databaseDao.getPlaylistPairSongByListPosition(playlistId, listPosition)

    suspend fun getPlaylistPairSongByOffset(
        playlistId: Long,
        offset: Int,
        filterState: FilterState,
    ) = if (filterState == FilterState.CustomOrder) {
        databaseDao.getPlaylistPairSongByOffset(
            playlistId,
            offset * 50,
        )
    } else if (filterState == FilterState.Title) {
        databaseDao.getPlaylistPairSongByTitle(
            playlistId,
            offset * 50,
        )
    } else {
        null
    }

    suspend fun getPlaylistPairSongByTime(
        playlistId: Long,
        filterState: FilterState,
        localDateTime: LocalDateTime,
    ) = if (filterState == FilterState.OlderFirst) {
        databaseDao.getPlaylistPairSongByOlderFirst(
            playlistId,
            localDateTime,
        )
    } else if (filterState == FilterState.NewerFirst) {
        databaseDao.getPlaylistPairSongByNewerFirst(
            playlistId,
            localDateTime,
        )
    } else {
        null
    }

    suspend fun getNewestPlaylistPairSong(playlistId: Long) = databaseDao.getNewestPlaylistPairSong(playlistId)

    suspend fun editPositionOfSongInPlaylist(
        playlistId: Long,
        videoId: String,
        newPosition: Int,
    ) = databaseDao.editPositionOfSongInPlaylist(
        playlistId,
        videoId,
        newPosition,
    )

    suspend fun deletePairSongLocalPlaylist(
        playlistId: Long,
        videoId: String,
    ) = databaseDao.deletePairSongLocalPlaylist(playlistId, videoId)

    suspend fun getGoogleAccounts() = databaseDao.getAllGoogleAccount()

    suspend fun insertGoogleAccount(googleAccountEntity: GoogleAccountEntity) = databaseDao.insertGoogleAccount(googleAccountEntity)

    suspend fun getUsedGoogleAccount() = databaseDao.getUsedGoogleAccount()

    suspend fun deleteGoogleAccount(email: String) = databaseDao.deleteGoogleAccount(email)

    suspend fun updateGoogleAccountUsed(
        email: String,
        isUsed: Boolean,
    ) = databaseDao.updateGoogleAccountUsed(isUsed, email)

    suspend fun setInLibrary(
        videoId: String,
        inLibrary: LocalDateTime,
    ) = databaseDao.setInLibrary(videoId, inLibrary)

    suspend fun insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum: FollowedArtistSingleAndAlbum) =
        databaseDao.insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum)

    suspend fun deleteFollowedArtistSingleAndAlbum(channelId: String) = databaseDao.deleteFollowedArtistSingleAndAlbum(channelId)

    suspend fun getFollowedArtistSingleAndAlbum(channelId: String) = databaseDao.getFollowedArtistSingleAndAlbum(channelId)

    suspend fun getAllFollowedArtistSingleAndAlbums(
        limit: Int,
        offset: Int,
    ) = databaseDao.getAllFollowedArtistSingleAndAlbum(
        limit,
        offset,
    )

    suspend fun insertNotification(notificationEntity: NotificationEntity) = databaseDao.insertNotification(notificationEntity)

    suspend fun getAllNotification() = databaseDao.getAllNotification()

    suspend fun deleteNotification(id: Long) = databaseDao.deleteNotification(id)

    suspend fun getTranslatedLyrics(
        videoId: String,
        language: String,
    ) = databaseDao.getTranslatedLyrics(videoId, language)

    suspend fun removeTranslatedLyrics(
        videoId: String,
        language: String,
    ) = databaseDao.removeTranslatedLyrics(videoId, language)

    suspend fun insertTranslatedLyrics(translatedLyricsEntity: TranslatedLyricsEntity) = databaseDao.insertTranslatedLyrics(translatedLyricsEntity)

    suspend fun insertPodcast(podcastsEntity: PodcastsEntity) = databaseDao.insertPodcast(podcastsEntity)

    suspend fun insertEpisodes(episodes: List<EpisodeEntity>) = databaseDao.insertEpisodes(episodes)

    suspend fun getPodcastWithEpisodes(podcastId: String) = databaseDao.getPodcastWithEpisodes(podcastId)

    suspend fun getAllPodcasts(limit: Int) = databaseDao.getAllPodcasts(limit)

    suspend fun getAllPodcastWithEpisodes(
        limit: Int,
        offset: Int,
    ) = databaseDao.getAllPodcastWithEpisodes(
        limit,
        offset,
    )

    suspend fun getPodcast(podcastId: String) = databaseDao.getPodcast(podcastId)

    suspend fun getFavoritePodcasts(
        limit: Int,
        offset: Int,
    ) = databaseDao.getFavoritePodcasts(
        limit,
        offset,
    )

    suspend fun getEpisode(videoId: String) = databaseDao.getEpisode(videoId)

    suspend fun deletePodcast(podcastId: String) = databaseDao.deletePodcast(podcastId)

    suspend fun favoritePodcast(
        podcastId: String,
        isFavorite: Boolean,
    ): Boolean {
        val podcast = databaseDao.getPodcast(podcastId)
        if (podcast != null) {
            val updatedPodcast =
                podcast.copy(
                    isFavorite = isFavorite,
                    favoriteTime = if (isFavorite) now() else null,
                )
            return databaseDao.insertPodcast(updatedPodcast) > 0
        } else {
            return false
        }
    }

    suspend fun getPodcastEpisodes(
        podcastId: String,
        limit: Int,
        offset: Int,
    ) = databaseDao.getPodcastEpisodes(
        podcastId,
        limit,
        offset,
    )

    suspend fun updatePodcastInLibraryNow(id: String) = databaseDao.updatePodcastInLibrary(id, now())

    suspend fun insertYourYouTubePlaylist(yourYouTubePlaylist: YourYouTubePlaylistList) =
        databaseDao.insertYourYouTubePlaylist(yourYouTubePlaylist)

    suspend fun getYourYouTubePlaylistList(
        emailPageId: String
    ): YourYouTubePlaylistList? =
        databaseDao.getYourYouTubePlaylistList(emailPageId)

    suspend fun deleteAllYourYouTubePlaylist() =
        databaseDao.deleteAllYourYouTubePlaylist()

}

