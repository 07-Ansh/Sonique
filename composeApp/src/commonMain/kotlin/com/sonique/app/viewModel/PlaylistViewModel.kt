@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.sonique.app.viewModel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.sonique.common.Config
import com.sonique.domain.data.entities.DownloadState.STATE_DOWNLOADED
import com.sonique.domain.data.entities.DownloadState.STATE_DOWNLOADING
import com.sonique.domain.data.entities.PlaylistEntity
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.domain.data.model.browse.playlist.Author
import com.sonique.domain.data.model.browse.playlist.PlaylistBrowse
import com.sonique.domain.data.model.browse.playlist.PlaylistState
import com.sonique.domain.extension.now
import com.sonique.domain.mediaservice.handler.DownloadHandler
import com.sonique.domain.mediaservice.handler.PlaylistType
import com.sonique.domain.mediaservice.handler.QueueData
import com.sonique.domain.repository.LocalPlaylistRepository
import com.sonique.domain.repository.PlaylistRepository
import com.sonique.domain.repository.SongRepository
import com.sonique.domain.utils.Resource
import com.sonique.domain.utils.collectLatestResource
import com.sonique.domain.utils.toListVideoId
import com.sonique.domain.utils.toPlaylistEntity
import com.sonique.domain.utils.toSongEntity
import com.sonique.domain.utils.toTrack
import com.sonique.logger.Logger
import com.sonique.app.viewModel.PlaylistUIState.Error
import com.sonique.app.viewModel.PlaylistUIState.Loading
import com.sonique.app.viewModel.PlaylistUIState.Success
import com.sonique.app.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.auto_created_by_youtube_music
import sonique.composeapp.generated.resources.downloading
import sonique.composeapp.generated.resources.error
import sonique.composeapp.generated.resources.playlist
import sonique.composeapp.generated.resources.playlist_is_empty
import sonique.composeapp.generated.resources.radio
import sonique.composeapp.generated.resources.radio_not_available
import sonique.composeapp.generated.resources.shuffle
import sonique.composeapp.generated.resources.shuffle_not_available
import sonique.composeapp.generated.resources.synced
import sonique.composeapp.generated.resources.syncing
import sonique.composeapp.generated.resources.view_count
import sonique.composeapp.generated.resources.liked_songs
import sonique.composeapp.generated.resources.downloaded_songs
import sonique.composeapp.generated.resources.sonique_lyrics
import com.sonique.common.LOCAL_PLAYLIST_ID_LIKED
import com.sonique.common.LOCAL_PLAYLIST_ID_DOWNLOADED


class PlaylistViewModel(
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
) : BaseViewModel() {
    val downloadUtils: DownloadHandler by inject<DownloadHandler>()
    private var _uiState = MutableStateFlow<PlaylistUIState>(Loading)
    val uiState: StateFlow<PlaylistUIState> = _uiState

    private var _listColors = MutableStateFlow<List<Color>>(emptyList())
    val listColors: StateFlow<List<Color>> = _listColors

    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private var _playlistEntity: MutableStateFlow<PlaylistEntity?> = MutableStateFlow(null)
    var playlistEntity: StateFlow<PlaylistEntity?> = _playlistEntity

    val downloadState = _playlistEntity.map { it?.downloadState ?: 0 }.stateIn(viewModelScope, WhileSubscribed(1000), 0)
    val liked = _playlistEntity.map { it?.liked == true }.stateIn(viewModelScope, WhileSubscribed(1000), false)

    private var _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks

    private var _tracksListState = MutableStateFlow<ListState>(ListState.IDLE)
    val tracksListState: StateFlow<ListState> = _tracksListState

    private var collectDownloadedJob: Job? = null
    private var _downloadedList = MutableStateFlow<List<String>>(emptyList())
    val downloadedList: StateFlow<List<String>> = _downloadedList

    private var playlistEntityJob: Job? = null
    private var newUpdateJob: Job? = null
    private var checkDownloadedPlaylist: Job? = null

    init {
        viewModelScope.launch {
            val listTrackStringJob =
                launch(Dispatchers.IO) {
                    downloadState
                        .collectLatest { state ->
                            newUpdateJob?.cancel()
                            val id = playlistEntity.value?.id ?: return@collectLatest
                            if (state == STATE_DOWNLOADING || state == STATE_DOWNLOADED) {
                                getFullTracks { tracks ->
                                    newUpdateJob =
                                        launch {
                                            val listSongs = songRepository.getSongsByListVideoId(tracks.toListVideoId()).firstOrNull() ?: emptyList()
                                            if (state == STATE_DOWNLOADED && listSongs.isNotEmpty()) {
                                                listSongs.filter { it.downloadState != STATE_DOWNLOADED }.let { notDownloaded ->
                                                    if (notDownloaded.isNotEmpty()) {
                                                        downloadTracks(notDownloaded.map { it.videoId })
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADING)
                                                    } else {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADED)
                                                    }
                                                }
                                            }
                                            downloadUtils.downloads.collectLatest { downloads ->
                                                var count = 0
                                                tracks.forEachIndexed { index, track ->
                                                    val trackDownloadState = downloads[track.videoId]?.first?.state
                                                    val videoDownloadState =
                                                        downloads[track.videoId]?.second?.state ?: DownloadHandler.State.STATE_COMPLETED
                                                    if (trackDownloadState == DownloadHandler.State.STATE_DOWNLOADING ||
                                                        videoDownloadState == DownloadHandler.State.STATE_DOWNLOADING
                                                    ) {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADING)
                                                    } else if (trackDownloadState == DownloadHandler.State.STATE_COMPLETED &&
                                                        videoDownloadState == DownloadHandler.State.STATE_COMPLETED
                                                    ) {
                                                        count++
                                                    }
                                                    if (count == tracks.size) {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADED)
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                        }
                }
            listTrackStringJob.join()
        }
    }

    private fun updatePlaylistDownloadState(
        id: String,
        state: Int,
        withDelay: Boolean = true,
    ) {
        viewModelScope.launch {
            playlistRepository.updatePlaylistDownloadState(id, state)
            if (withDelay) delay(500)
            _playlistEntity.update {
                it?.copy(downloadState = state)
            }
        }
    }

    private fun downloadTracks(listJob: List<String>) {
        viewModelScope.launch {
            listJob.forEach { videoId ->
                songRepository.getSongById(videoId).singleOrNull()?.let { song ->
                    if (song.downloadState != STATE_DOWNLOADED) {
                        downloadUtils.downloadTrack(videoId, song.title, song.thumbnails ?: "")
                    }
                }
            }
        }
    }

    private fun resetData() {
        _uiState.value = Loading
        _playlistEntity.value = null
        _downloadedList.value = emptyList()
        _listColors.value = emptyList()
        checkDownloadedPlaylist?.cancel()
        checkDownloadedPlaylist = null
    }


    fun getData(id: String) {
        resetData()
        viewModelScope.launch {
            if (id == LOCAL_PLAYLIST_ID_LIKED) {
                playlistEntityJob = launch {
                    songRepository.getLikedSongs().collect { songs ->
                        val playlist = PlaylistEntity(
                            id = id,
                            title = getString(Res.string.liked_songs),
                            author = getString(Res.string.sonique_lyrics),
                            thumbnails = "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-songs-delhi-1200.png",
                            tracks = songs.map { it.videoId },
                            trackCount = songs.size,
                            favoriteAt = now(),
                            inLibrary = now(),
                            downloadState = 0
                        )
                        _playlistEntity.value = playlist
                        _tracks.value = songs.map { it.toTrack() }
                        _uiState.value = Success(
                            data = PlaylistState(
                                id = playlist.id,
                                title = playlist.title,
                                isRadio = false,
                                author = Author(
                                    id = "",
                                    name = playlist.author ?: "",
                                ),
                                thumbnail = playlist.thumbnails,
                                description = playlist.description,
                                trackCount = playlist.trackCount,
                                year = playlist.year ?: now().year.toString(),
                            )
                        )
                        _tracksListState.value = ListState.PAGINATION_EXHAUST
                    }
                }
            } else if (id == LOCAL_PLAYLIST_ID_DOWNLOADED) {
                 playlistEntityJob = launch {
                    songRepository.getDownloadedSongs().collect { songs ->
                        val list = songs ?: emptyList()
                        val playlist = PlaylistEntity(
                            id = id,
                            title = getString(Res.string.downloaded_songs),
                            author = getString(Res.string.sonique_lyrics),
                            thumbnails = "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-songs-delhi-1200.png",
                            tracks = list.map { it.videoId },
                            trackCount = list.size,
                            favoriteAt = now(),
                            inLibrary = now(),
                            downloadState = 0
                        )
                        _playlistEntity.value = playlist
                        _tracks.value = list.map { it.toTrack() }
                        _uiState.value = Success(
                            data = PlaylistState(
                                id = playlist.id,
                                title = playlist.title,
                                isRadio = false,
                                author = Author(
                                    id = "",
                                    name = playlist.author ?: "",
                                ),
                                thumbnail = playlist.thumbnails,
                                description = playlist.description,
                                trackCount = playlist.trackCount,
                                year = playlist.year ?: now().year.toString(),
                            )
                        )
                        _tracksListState.value = ListState.PAGINATION_EXHAUST
                    }
                }
            } else if (id.matches(Regex("(RDAMVM|RDEM|RDAT).*"))) {
                playlistRepository
                    .getRadio(
                        id,
                        radioString = getString(Res.string.radio),
                        defaultDescription = getString(Res.string.auto_created_by_youtube_music),
                        viewString = getString(Res.string.view_count),
                    ).collect { res ->
                        val data = res.data
                        when (res) {
                            is Resource.Success if (data != null) -> {
                                Logger.d(tag, "Radio data: $data")
                                _uiState.value =
                                    Success(
                                        data =
                                            PlaylistState(
                                                id = data.first.id,
                                                title = data.first.title,
                                                isRadio = true,
                                                author = data.first.author,
                                                thumbnail =
                                                    data.first.thumbnails
                                                        .lastOrNull()
                                                        ?.url,
                                                description = data.first.description,
                                                trackCount = data.first.trackCount,
                                                year = data.first.year,
                                            ),
                                    )
                                _tracks.value = data.first.tracks
                                _continuation.value = data.second
                                if (data.second.isNullOrEmpty()) _tracksListState.value = ListState.PAGINATION_EXHAUST
                                playlistRepository.insertRadioPlaylist(data.first.toPlaylistEntity())
                            }

                            else -> {
                                _uiState.value = Error(res.message ?: "Empty response")
                            }
                        }
                    }
            } else {
                 
                playlistRepository
                    .getPlaylistData(id, getString(Res.string.view_count))
                    .collect { res ->
                        val data = res.data
                        when (res) {
                            is Resource.Success if (data != null) -> {
                                Logger.d(tag, "Playlist data: $data")
                                log("Playlist endpoint: ${data.first.shuffleEndpoint}")
                                _uiState.value =
                                    Success(
                                        data =
                                            PlaylistState(
                                                id = data.first.id,
                                                title = data.first.title,
                                                isRadio = false,
                                                author = data.first.author,
                                                thumbnail =
                                                    data.first.thumbnails
                                                        .lastOrNull()
                                                        ?.url,
                                                description = data.first.description,
                                                trackCount = data.first.trackCount,
                                                year = data.first.year,
                                                shuffleEndpoint = data.first.shuffleEndpoint,
                                                radioEndpoint = data.first.radioEndpoint,
                                            ),
                                    )
                                _tracks.value = data.first.tracks
                                _continuation.value = data.second
                                if (data.second.isNullOrEmpty()) _tracksListState.value = ListState.PAGINATION_EXHAUST
                                getPlaylistEntity(id = data.first.id, playlistBrowse = data.first)
                            }

                            else -> {
                                getPlaylistEntity(id)
                            }
                        }
                    }
            }
        }
    }

    fun getContinuationTrack(
        playlistId: String,
        continuation: String?,
    ) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _tracksListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                _tracksListState.value = ListState.PAGINATING
                songRepository
                    .getContinueTrack(
                        playlistId,
                        continuation,
                        fromPlaylist = true,
                    ).collectLatest { res ->
                        res.first?.forEach { track ->
                            songRepository
                                .insertSong(
                                    track
                                        .toSongEntity()
                                        .copy(
                                            inLibrary = Config.REMOVED_SONG_DATE_TIME,
                                        ),
                                ).singleOrNull()
                                ?.let {
                                    log("Insert song: $it")
                                }
                        }
                        _tracks.update {
                            val newList = it.toMutableList()
                            newList.addAll(res.first ?: emptyList())
                            newList
                        }
                        if (res.second.isNullOrEmpty()) {
                            _continuation.value = null
                            _tracksListState.value = ListState.PAGINATION_EXHAUST
                        } else {
                            _continuation.value = res.second
                            _tracksListState.value = ListState.IDLE
                        }
                    }
            }
        }
    }

    private fun getPlaylistEntity(
        id: String,
        playlistBrowse: PlaylistBrowse? = null,
    ) {
        playlistEntityJob?.cancel()
        playlistEntityJob =
            viewModelScope.launch {
                val playlistEntity = playlistRepository.getPlaylist(id).firstOrNull()
                if (playlistBrowse != null) {
                    if (playlistEntity == null) {
                        playlistRepository.insertAndReplacePlaylist(
                            playlistBrowse.toPlaylistEntity(),
                        )
                        delay(500)
                        playlistRepository.getPlaylist(id).collectLatest { playlist ->
                            _playlistEntity.value = playlist
                            playlistRepository.updatePlaylistInLibrary(
                                playlistId = id,
                                inLibrary = now(),
                            )
                        }
                    } else {
                        _playlistEntity.value = playlistEntity
                        playlistRepository.updatePlaylistInLibrary(
                            playlistId = id,
                            inLibrary = now(),
                        )
                    }
                    playlistBrowse.tracks.forEach { tracks ->
                        songRepository
                            .insertSong(
                                tracks.toSongEntity().copy(
                                    inLibrary = Config.REMOVED_SONG_DATE_TIME,
                                ),
                            ).firstOrNull()
                            ?.let {
                                log("Insert song: $it")
                            }
                    }
                } else if (playlistEntity != null) {
                    _playlistEntity.value = playlistEntity
                    playlistRepository.updatePlaylistInLibrary(
                        playlistId = id,
                        inLibrary = now(),
                    )
                    _uiState.value =
                        Success(
                            data =
                                PlaylistState(
                                    id = playlistEntity.id,
                                    title = playlistEntity.title,
                                    isRadio = false,
                                    author =
                                        Author(
                                            id = "",
                                            name = playlistEntity.author ?: "",
                                        ),
                                    thumbnail = playlistEntity.thumbnails,
                                    description = playlistEntity.description,
                                    trackCount = playlistEntity.trackCount,
                                    year = playlistEntity.year ?: now().year.toString(),
                                ),
                        )
                    _tracksListState.value = ListState.LOADING
                    playlistEntity.tracks?.let {
                        songRepository
                            .getSongsByListVideoId(it)
                            .singleOrNull()
                            ?.let { song ->
                                _tracks.value = song.map { it.toTrack() }
                            }
                    }
                    _tracksListState.value = ListState.PAGINATION_EXHAUST
                    if (playlistEntity.downloadState != STATE_DOWNLOADED) {
                        checkDownloadedPlaylist =
                            launch {
                                val listSong =
                                    songRepository
                                        .getSongsByListVideoId(
                                            playlistEntity.tracks ?: emptyList(),
                                        ).firstOrNull()
                                Logger.d(tag, "List song: $listSong")
                                if (!listSong.isNullOrEmpty() && listSong.size == playlistEntity.tracks?.size &&
                                    listSong.all {
                                        it.downloadState == STATE_DOWNLOADED
                                    }
                                ) {
                                    updatePlaylistDownloadState(playlistEntity.id, STATE_DOWNLOADED)
                                }
                            }
                    }
                } else {
                    _uiState.value = Error("Empty response")
                }
            }
    }

    fun setBrush(listColors: List<Color>) {
        _listColors.value = listColors
    }

    fun updatePlaylistLiked(
        liked: Boolean,
        id: String,
    ) {
        viewModelScope.launch {
            val tempLiked = if (liked) 1 else 0
            playlistRepository.updatePlaylistLiked(id, tempLiked)
            _playlistEntity.update {
                it?.copy(
                    liked = tempLiked == 1,
                )
            }
            getFullTracks { }
        }
    }

    fun onUIEvent(event: PlaylistUIEvent) {
        val data = uiState.value.data ?: return
        when (event) {
            is PlaylistUIEvent.ItemClick -> {
                viewModelScope.launch {
                    val videoId = event.videoId
                    val loadedList = tracks.value
                    val clickedSong = loadedList.first { it.videoId == videoId }
                    val index = loadedList.indexOf(clickedSong)
                    setQueueData(
                        QueueData.Data(
                            listTracks = loadedList.toCollection(arrayListOf<Track>()),
                            firstPlayedTrack = clickedSong,
                            playlistId = data.id,
                            playlistName = "${
                                getString(
                                    Res.string.playlist,
                                )
                            } \"${data.title}\"",
                            playlistType = PlaylistType.PLAYLIST,
                            continuation = continuation.value,
                        ),
                    )
                    loadMediaItem(
                        clickedSong,
                        Config.PLAYLIST_CLICK,
                        index,
                    )
                }
            }

            PlaylistUIEvent.PlayAll -> {
                val loadedList = tracks.value
                if (loadedList.isEmpty()) {
                    viewModelScope.launch {
                        makeToast(
                            getString(Res.string.playlist_is_empty),
                        )
                    }
                    return
                }
                val clickedSong = loadedList.first()
                viewModelScope.launch {
                    setQueueData(
                        QueueData.Data(
                            listTracks = loadedList.toCollection(arrayListOf<Track>()),
                            firstPlayedTrack = clickedSong,
                            playlistId = data.id,
                            playlistName = "${
                                getString(
                                    Res.string.playlist,
                                )
                            } \"${data.title}\"",
                            playlistType = PlaylistType.PLAYLIST,
                            continuation = continuation.value,
                        ),
                    )
                    loadMediaItem(
                        clickedSong,
                        Config.PLAYLIST_CLICK,
                        0,
                    )
                }
            }

            PlaylistUIEvent.Shuffle -> {
                val shuffleEndpoint = data.shuffleEndpoint
                if (shuffleEndpoint == null) {
                    viewModelScope.launch {
                        makeToast(
                            getString(Res.string.shuffle_not_available),
                        )
                    }
                    return
                } else {
                    viewModelScope.launch {
                        songRepository.getRadioFromEndpoint(shuffleEndpoint).collectLatest { res ->
                            val result = res.data
                            when (res) {
                                is Resource.Success if (result != null) -> {
                                    Logger.d(tag, "Shuffle data: ${result.first.size}")
                                    setQueueData(
                                        QueueData.Data(
                                            listTracks = result.first.toCollection(arrayListOf<Track>()),
                                            firstPlayedTrack = result.first.firstOrNull() ?: return@collectLatest,
                                            playlistId = shuffleEndpoint.playlistId,
                                            playlistName = "\"${data.title}\" ${getString(Res.string.shuffle)}",
                                            playlistType = PlaylistType.RADIO,
                                            continuation = result.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        result.first.firstOrNull() ?: return@collectLatest,
                                        Config.RADIO_CLICK,
                                        0,
                                    )
                                }

                                else -> {
                                    viewModelScope.launch {
                                        makeToast(
                                            res.message ?: getString(Res.string.error),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PlaylistUIEvent.StartRadio -> {
                val radioEndpoint = data.radioEndpoint
                if (radioEndpoint == null) {
                    viewModelScope.launch {
                        makeToast(
                            getString(Res.string.radio_not_available),
                        )
                    }
                    return
                } else {
                    viewModelScope.launch {
                        songRepository.getRadioFromEndpoint(radioEndpoint).collectLatest { res ->
                            val result = res.data
                            when (res) {
                                is Resource.Success if (result != null) -> {
                                    Logger.d(tag, "Radio data: ${result.first.size}")
                                    setQueueData(
                                        QueueData.Data(
                                            listTracks = result.first.toCollection(arrayListOf<Track>()),
                                            firstPlayedTrack = result.first.firstOrNull() ?: return@collectLatest,
                                            playlistId = radioEndpoint.playlistId,
                                            playlistName = "\"${data.title}\" ${getString(Res.string.radio)}",
                                            playlistType = PlaylistType.RADIO,
                                            continuation = result.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        result.first.firstOrNull() ?: return@collectLatest,
                                        Config.RADIO_CLICK,
                                        0,
                                    )
                                }

                                else -> {
                                    viewModelScope.launch {
                                        makeToast(
                                            res.message ?: getString(Res.string.error),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PlaylistUIEvent.Download -> {
                downloadFullPlaylist()
            }

            PlaylistUIEvent.CancelDownload -> {
                cancelDownload()
            }

            PlaylistUIEvent.Favorite -> {
                updatePlaylistLiked(!liked.value, data.id)
            }
        }
    }

    fun getFullTracks(callback: (List<Track>) -> Unit) {
        viewModelScope.launch {
            if (tracksListState.value == ListState.PAGINATION_EXHAUST) {
                _playlistEntity.value
                    ?.copy(
                        tracks = tracks.value.toListVideoId(),
                        trackCount = tracks.value.size,
                    )?.let {
                        playlistRepository.insertAndReplacePlaylist(it)
                    }
                callback(tracks.value)
            } else {
                val id = uiState.value.data?.id ?: return@launch
                tracksListState.collectLatest { state ->
                    if (state == ListState.PAGINATION_EXHAUST) {
                        _playlistEntity.value
                            ?.copy(
                                tracks = tracks.value.toListVideoId(),
                                trackCount = tracks.value.size,
                            )?.let {
                                playlistRepository.insertAndReplacePlaylist(it)
                            }
                        callback(tracks.value)
                    } else if (state != ListState.PAGINATING) {
                        getContinuationTrack(id, continuation.value)
                    }
                }
            }
        }
    }

    fun saveToLocal(tracks: List<Track>) {
        viewModelScope.launch {
            val data = uiState.value.data ?: return@launch
            localPlaylistRepository
                .syncYouTubePlaylistToLocalPlaylist(
                    data,
                    tracks,
                    getString(Res.string.synced),
                    getString(Res.string.error),
                ).collectLatestResource(
                    onSuccess = {
                        makeToast(it)
                    },
                    onLoading = {
                        viewModelScope.launch {
                            makeToast(getString(Res.string.syncing))
                        }
                    },
                    onError = {
                        makeToast(it)
                    },
                )
        }
    }

    fun downloadFullPlaylist() {
        viewModelScope.launch {
            val id = playlistEntity.value?.id ?: return@launch
            makeToast(getString(Res.string.downloading))
            updatePlaylistDownloadState(id, STATE_DOWNLOADING)
            getFullTracks { tracks ->
                viewModelScope.launch {
                    tracks.chunked(2).forEach { chunk ->
                        chunk.forEach { track ->
                            viewModelScope.launch {
                                downloadUtils.downloadTrack(
                                    track.videoId,
                                    track.title,
                                    track.thumbnails?.lastOrNull()?.url ?: ""
                                )
                            }
                        }
                        delay(200)
                    }
                }
            }
        }
    }

    fun cancelDownload() {
        viewModelScope.launch {
            val id = playlistEntity.value?.id ?: return@launch
            makeToast("Download removed")
            updatePlaylistDownloadState(
                id,
                com.sonique.domain.data.entities.DownloadState.STATE_NOT_DOWNLOADED,
                false
            )
            tracks.value.forEach {
                viewModelScope.launch {
                    downloadUtils.removeDownload(it.videoId)
                }
            }
        }
    }

    fun updatePlaylistTitle(
        title: String,
        id: String,
    ) {
        viewModelScope.launch {
            playlistRepository
                .updateYourYouTubePlaylistTitle(
                    id,
                    title,
                ).collect {
                    when (it) {
                        is Resource.Success -> {
                            getData(id)
                            makeToast(it.data ?: getString(Res.string.synced))
                        }

                        is Resource.Error -> {
                            makeToast(it.message ?: getString(Res.string.error))
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        collectDownloadedJob?.cancel()
        playlistEntityJob?.cancel()
    }
}

sealed class PlaylistUIState(
    val data: PlaylistState? = null,
    val message: String? = null,
) {
    data object Loading : PlaylistUIState()

    class Success(
        data: PlaylistState,
    ) : PlaylistUIState(
            data = data,
        )

    class Error(
        message: String? = null,
    ) : PlaylistUIState(
            message = message,
        )
}

sealed class PlaylistUIEvent {
    data object PlayAll : PlaylistUIEvent()

    data object Shuffle : PlaylistUIEvent()

    data object StartRadio : PlaylistUIEvent()

    data class ItemClick(
        val videoId: String,
    ) : PlaylistUIEvent()

    data object Download : PlaylistUIEvent()

    data object CancelDownload : PlaylistUIEvent()

    data object Favorite : PlaylistUIEvent()
}

enum class ListState {
    IDLE,
    LOADING,
    PAGINATING,
    ERROR,
    PAGINATION_EXHAUST,
}

