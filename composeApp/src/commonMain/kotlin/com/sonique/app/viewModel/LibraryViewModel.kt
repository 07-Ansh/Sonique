package com.sonique.app.viewModel

import androidx.lifecycle.viewModelScope
import com.sonique.common.Config
import com.sonique.common.LibraryChipType
import com.sonique.domain.data.entities.AlbumEntity
import com.sonique.domain.data.entities.LocalPlaylistEntity
import com.sonique.domain.data.entities.PlaylistEntity
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.data.entities.DownloadState
import com.sonique.domain.data.model.searchResult.playlists.PlaylistsResult
import com.sonique.domain.data.type.PlaylistType
import com.sonique.domain.data.type.RecentlyType
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.repository.AlbumRepository
import com.sonique.domain.repository.ArtistRepository
import com.sonique.domain.repository.CommonRepository
import com.sonique.domain.repository.LocalPlaylistRepository
import com.sonique.domain.repository.PlaylistRepository
import com.sonique.domain.repository.PodcastRepository
import com.sonique.domain.repository.SongRepository
import com.sonique.domain.utils.LocalResource
import com.sonique.domain.data.entities.ArtistEntity
import com.sonique.app.viewModel.base.BaseViewModel
import com.sonique.domain.mediaservice.handler.DownloadHandler
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.added_local_playlist
import sonique.composeapp.generated.resources.youtube_liked_music
import sonique.composeapp.generated.resources.liked_songs
import sonique.composeapp.generated.resources.downloaded_songs
import sonique.composeapp.generated.resources.sonique_lyrics
import sonique.composeapp.generated.resources.your_youtube_playlists
import sonique.composeapp.generated.resources.youtube_albums
import sonique.composeapp.generated.resources.mix_for_you
import com.sonique.domain.extension.now
import com.sonique.common.LOCAL_PLAYLIST_ID_LIKED
import com.sonique.common.LOCAL_PLAYLIST_ID_DOWNLOADED



class LibraryViewModel(
    private val dataStoreManager: DataStoreManager,
    private val songRepository: SongRepository,
    private val commonRepository: CommonRepository,
    private val playlistRepository: PlaylistRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val podcastRepository: PodcastRepository,
    private val downloadHandler: DownloadHandler,
) : BaseViewModel() {
    private val _currentScreen: MutableStateFlow<LibraryChipType> = MutableStateFlow(LibraryChipType.YOUR_LIBRARY)
    val currentScreen: StateFlow<LibraryChipType> get() = _currentScreen.asStateFlow()
    private val _recentlyAdded: MutableStateFlow<LocalResource<List<RecentlyType>>> =
        MutableStateFlow(LocalResource.Loading())
    val recentlyAdded: StateFlow<LocalResource<List<RecentlyType>>> get() = _recentlyAdded.asStateFlow()

    private val _pinnedItems: MutableStateFlow<List<PlaylistEntity>> = MutableStateFlow(emptyList())
    val pinnedItems: StateFlow<List<PlaylistEntity>> get() = _pinnedItems.asStateFlow()

    private val _yourLocalPlaylist: MutableStateFlow<LocalResource<List<LocalPlaylistEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val yourLocalPlaylist: StateFlow<LocalResource<List<LocalPlaylistEntity>>> get() = _yourLocalPlaylist.asStateFlow()

    private val _youTubePlaylist: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubePlaylist: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubePlaylist.asStateFlow()

    private val _youTubeMixForYou: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubeMixForYou: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubeMixForYou.asStateFlow()

    private val _youTubeAlbums: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubeAlbums: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubeAlbums.asStateFlow()

    private val _favoritePlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePlaylist.asStateFlow()

    private val _favoritePodcasts: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePodcasts: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePodcasts.asStateFlow()

    private val _downloadedPlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val downloadedPlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _downloadedPlaylist.asStateFlow()

    private val _listCanvasSong: MutableStateFlow<LocalResource<List<SongEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val listCanvasSong: StateFlow<LocalResource<List<SongEntity>>> get() = _listCanvasSong.asStateFlow()

    private val _accountThumbnail: MutableStateFlow<String?> = MutableStateFlow(null)
    val accountThumbnail: StateFlow<String?> get() = _accountThumbnail.asStateFlow()

    private val _followedArtists: MutableStateFlow<LocalResource<List<ArtistEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val followedArtists: StateFlow<LocalResource<List<ArtistEntity>>> get() = _followedArtists.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> get() = _isGridView.asStateFlow()

    private val _isPinnedGridView = MutableStateFlow(true)
    val isPinnedGridView: StateFlow<Boolean> get() = _isPinnedGridView.asStateFlow()

    fun toggleLayoutView() {
        _isGridView.value = !_isGridView.value
    }

    fun togglePinnedLayoutView() {
        _isPinnedGridView.value = !_isPinnedGridView.value
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val youtubeLoggedIn = dataStoreManager.loggedIn.mapLatest { it == DataStoreManager.TRUE }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeDownloads = downloadHandler.downloadTask.mapLatest { tasks ->
        tasks.count {
            it.value == DownloadState.STATE_DOWNLOADING || it.value == DownloadState.STATE_PREPARING
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

 

    fun cancelActiveDownloads() {
        val tasks = downloadHandler.downloadTask.value
        tasks.filter { it.value == DownloadState.STATE_DOWNLOADING || it.value == DownloadState.STATE_PREPARING }
            .keys
            .forEach { videoId ->
                downloadHandler.removeDownload(videoId)
            }
    }

    init {
        viewModelScope.launch {
            val currentScreenJob = launch {
                dataStoreManager.getString("library_current_screen").first()?.let { chipType ->
                    LibraryChipType.fromStringValue(chipType)?.let {
                     _currentScreen.value = it
                    }
                }
            }
            val cookieJob = launch {
                dataStoreManager.cookie.distinctUntilChanged().collect {
                    _accountThumbnail.value = dataStoreManager.getString("AccountThumbUrl").first().takeIf { !it.isNullOrEmpty() }
                }
            }
            val artistJob = launch {
                artistRepository.getFollowedArtists().collect { artists ->
                    _followedArtists.value = LocalResource.Success(artists)
                }
            }
        }

        // Keep database collectors running continuously in background to support hot state flows and instant navigation
        viewModelScope.launch {
            commonRepository.getAllRecentData().collectLatest { data ->
                val temp: MutableList<RecentlyType> = mutableListOf()
                temp.addAll(data)
                temp.find {
                    it is PlaylistEntity && (it.id.contains("RDEM") || it.id.contains("RDAMVM"))
                }.let {
                    temp.remove(it)
                }
                temp.removeIf { it is SongEntity && it.inLibrary == Config.REMOVED_SONG_DATE_TIME }
                // Strip pinned playlist stubs so they are only injected once below
                temp.removeIf { it is PlaylistEntity && (it.id == "LM" || it.id == LOCAL_PLAYLIST_ID_LIKED) }

                if (dataStoreManager.loggedIn.first() == DataStoreManager.TRUE) {
                    temp.add(
                        PlaylistEntity(
                            title = getString(Res.string.youtube_liked_music),
                            author = "YouTube Music",
                            id = "LM",
                            description = "PIN",
                            thumbnails = "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-songs-delhi-1200.png"
                        )
                    )
                    temp.add(
                        PlaylistEntity(
                            title = getString(Res.string.your_youtube_playlists),
                            author = "YouTube Music",
                            id = Config.PIN_YT_PLAYLISTS,
                            description = "PIN",
                            thumbnails = "https://www.google.com/url?sa=i&url=https%3A%2F%2Fwww.iconfinder.com%2Ficons%2F183618%2Fplaylist_icon&psig=AOvVaw2gXU9z_E_Y_M-s_-s_Y_M-&ust=1712239123456000&source=images&cd=vfe&opi=89978449&ved=0CBIQjRxqFwoTCKC_y_S_Y_MDFQAAAAAdAAAAABAE"
                        )
                    )
                    temp.add(
                        PlaylistEntity(
                            title = getString(Res.string.youtube_albums),
                            author = "YouTube Music",
                            id = Config.PIN_YT_ALBUMS,
                            description = "PIN",
                            thumbnails = "https://www.google.com/url?sa=i&url=https%3A%2F%2Fwww.flaticon.com%2Ffree-icon%2Falbum_1143890&psig=AOvVaw2gXU9z_E_Y_M-s_-s_Y_M-&ust=1712239123456000&source=images&cd=vfe&opi=89978449&ved=0CBIQjRxqFwoTCKC_y_S_Y_MDFQAAAAAdAAAAABAE"
                        )
                    )
                    temp.add(
                        PlaylistEntity(
                            title = getString(Res.string.mix_for_you),
                            author = "YouTube Music",
                            id = Config.PIN_YT_MIX,
                            description = "PIN",
                            thumbnails = "https://www.google.com/url?sa=i&url=https%3A%2F%2Fwww.iconfinder.com%2Ficons%2F216345%2Fmix_icon&psig=AOvVaw2gXU9z_E_Y_M-s_-s_Y_M-&ust=1712239123456000&source=images&cd=vfe&opi=89978449&ved=0CBIQjRxqFwoTCKC_y_S_Y_MDFQAAAAAdAAAAABAE"
                        )
                    )
                }
                val pinned = temp.filterIsInstance<PlaylistEntity>().filter { it.description == "PIN" }.toMutableList()
                _pinnedItems.value = pinned

                val realRecent = temp.filter { it is SongEntity || it is AlbumEntity }
                val limited = realRecent.take(6)
                _recentlyAdded.value = LocalResource.Success(limited.toImmutableList())
            }
        }

        viewModelScope.launch {
            localPlaylistRepository.getAllLocalPlaylists().collect { values ->
                _yourLocalPlaylist.value = LocalResource.Success(values.reversed())
                val currentPinned = _pinnedItems.value.filter { it.description == "PIN" }.toMutableList()
                values.reversed().forEach { local ->
                    currentPinned.add(
                        PlaylistEntity(
                            id = local.id.toString(),
                            title = local.title,
                            author = "Local Playlist",
                            description = "LOCAL_PIN",
                            thumbnails = local.thumbnail ?: ""
                        )
                    )
                }
                _pinnedItems.value = currentPinned
            }
        }

        viewModelScope.launch {
            val likedSongsTitle = getString(Res.string.liked_songs)
            val soniqueLyricsAuthor = getString(Res.string.sonique_lyrics)
            combine(
                albumRepository.getLikedAlbums(),
                playlistRepository.getLikedPlaylists()
            ) { album, playlist ->
                val temp: MutableList<PlaylistType> = mutableListOf()
                temp.addAll(album)
                temp.addAll(playlist)
                temp.sortedWith<PlaylistType>(
                    Comparator { p0, p1 ->
                        val timeP0: LocalDateTime? =
                            when (p0) {
                                is AlbumEntity -> p0.favoriteAt ?: p0.inLibrary
                                is PlaylistEntity -> p0.favoriteAt ?: p0.inLibrary
                                else -> null
                            }
                        val timeP1: LocalDateTime? =
                            when (p1) {
                                is AlbumEntity -> p1.favoriteAt ?: p1.inLibrary
                                is PlaylistEntity -> p1.favoriteAt ?: p1.inLibrary
                                else -> null
                            }
                        if (timeP0 == null || timeP1 == null) {
                            return@Comparator if (timeP0 == null && timeP1 == null) {
                                0
                            } else if (timeP0 == null) {
                                -1
                            } else {
                                1
                            }
                        }
                        timeP0.compareTo(timeP1)
                    },
                )
            }.collect { sortedList ->
                _favoritePlaylist.value = LocalResource.Success(sortedList)
            }
        }

        viewModelScope.launch {
            podcastRepository.getFavoritePodcasts().collectLatest { podcasts ->
                val sortedList = podcasts.sortedByDescending { it.favoriteTime }
                _favoritePodcasts.value = LocalResource.Success(sortedList)
            }
        }

        viewModelScope.launch {
            songRepository.getCanvasSong(max = 5).collect { data ->
                _listCanvasSong.value = LocalResource.Success(data)
            }
        }

        viewModelScope.launch {
            playlistRepository.getAllDownloadedPlaylist().collect { values ->
                val temp = values.toMutableList()
                temp.add(
                    0,
                    PlaylistEntity(
                        id = LOCAL_PLAYLIST_ID_DOWNLOADED,
                        title = getString(Res.string.downloaded_songs),
                        author = getString(Res.string.sonique_lyrics),
                        thumbnails = "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-songs-delhi-1200.png",
                        favoriteAt = now(),
                        inLibrary = now(),
                        downloadState = 0,
                    )
                )
                _downloadedPlaylist.value = LocalResource.Success(temp)
            }
        }
    }

    fun setCurrentScreen(chipType: LibraryChipType) {
        _currentScreen.value = chipType
        viewModelScope.launch {
            dataStoreManager.putString("library_current_screen", chipType.toStringValue())
        }
    }

    fun getRecentlyAdded() {}

    fun getYouTubePlaylist() {
        _youTubePlaylist.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getLibraryPlaylist().collect { data ->
                _youTubePlaylist.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun getYouTubeMixedForYou() {
        _youTubeMixForYou.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getMixedForYou().collect { data ->
                _youTubeMixForYou.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun getYouTubeAlbums() {
        _youTubeAlbums.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getLibraryAlbums().collect { data ->
                _youTubeAlbums.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun syncFollowedArtists() {
        val currentList = (_followedArtists.value as? LocalResource.Success)?.data ?: emptyList()
        if (currentList.isEmpty()) {
            _followedArtists.value = LocalResource.Loading()
        }
        viewModelScope.launch {
            artistRepository.syncFollowedArtistsFromYouTube()
            // Ensure we transition out of loading even if the database didn't change
            val savedList = artistRepository.getFollowedArtists().first()
            _followedArtists.value = LocalResource.Success(savedList)
        }
    }




    fun getPlaylistFavorite() {}

    fun getFavoritePodcasts() {}

    fun getCanvasSong() {}

    fun getLocalPlaylist() {}

    fun getDownloadedPlaylist() {}

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            val localPlaylistEntity = LocalPlaylistEntity(title = title)
            localPlaylistRepository
                .insertLocalPlaylist(
                    localPlaylistEntity,
                    getString(Res.string.added_local_playlist),
                ).lastOrNull()
                ?.let {
                    log("Created playlist with id: $it")
                }
            getLocalPlaylist()
        }
    }

    fun deleteSong(videoId: String) {
        _recentlyAdded.value = LocalResource.Loading()
        viewModelScope.launch {
            songRepository.setInLibrary(videoId, Config.REMOVED_SONG_DATE_TIME)
            songRepository.resetTotalPlayTime(videoId)
            delay(500)  
            getRecentlyAdded()
        }
    }

    fun deleteLocalPlaylist(id: Long) {
        viewModelScope.launch {
            localPlaylistRepository.deleteLocalPlaylist(id, "Deleted").collect {}
        }
    }


}

