package com.sonique.app.viewModel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.sonique.common.Config
import com.sonique.domain.data.entities.DownloadState
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.domain.data.model.browse.artist.ResultAlbum
import com.sonique.domain.data.model.searchResult.songs.Artist
import com.sonique.domain.extension.now
import com.sonique.domain.mediaservice.handler.DownloadHandler
import com.sonique.domain.mediaservice.handler.PlaylistType
import com.sonique.domain.mediaservice.handler.QueueData
import com.sonique.domain.repository.AlbumRepository
import com.sonique.domain.repository.SongRepository
import com.sonique.domain.utils.Resource
import com.sonique.domain.utils.toAlbumEntity
import com.sonique.domain.utils.toArrayListTrack
import com.sonique.domain.utils.toSongEntity
import com.sonique.logger.LogLevel
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.album
import sonique.composeapp.generated.resources.downloaded
import sonique.composeapp.generated.resources.error
import sonique.composeapp.generated.resources.playlist_is_empty

class AlbumViewModel(
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
) : BaseViewModel() {
    private val downloadUtils: DownloadHandler by inject<DownloadHandler>()
    private val _uiState: MutableStateFlow<AlbumUIState> = MutableStateFlow(AlbumUIState.initial())
    val uiState: StateFlow<AlbumUIState> = _uiState

    private var job: Job? = null
    private var collectDownloadStateJob: Job? = null

    fun updateBrowseId(browseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(browseId = browseId) }
            albumRepository.getAlbumData(browseId).collectLatest { res ->
                when (res) {
                    is Resource.Success -> {
                        val data = res.data
                        if (data != null) {
                            _uiState.update {
                                it.copy(
                                    browseId = browseId,
                                    title = data.title,
                                    thumbnail = data.thumbnails?.lastOrNull()?.url,
                                    artist =
                                        data.artists.firstOrNull() ?: Artist(
                                            id = null,
                                            name = "",
                                        ),
                                    year = data.year ?: now().year.toString(),
                                    trackCount = data.trackCount,
                                    description = data.description,
                                    length = data.duration ?: "",
                                    listTrack = data.tracks,
                                    otherVersion = data.otherVersion,
                                    loadState = LocalPlaylistState.PlaylistLoadState.Success,
                                )
                            }
                            val localAlbum = albumRepository.getAlbum(browseId).lastOrNull()
                            if (localAlbum != null) {
                                _uiState.update {
                                    it.copy(
                                        downloadState = localAlbum.downloadState,
                                        liked = localAlbum.liked,
                                    )
                                }
                                albumRepository.updateAlbumInLibrary(now(), browseId)
                            } else {
                                albumRepository.insertAlbum(data.toAlbumEntity(browseId)).singleOrNull().let {
                                    log("Insert Album $it")
                                    data.tracks.forEach { track ->
                                        songRepository
                                            .insertSong(
                                                track.toSongEntity().copy(
                                                    inLibrary = Config.REMOVED_SONG_DATE_TIME,
                                                ),
                                            ).singleOrNull()
                                            ?.let {
                                                log("Insert Song $it")
                                            }
                                    }
                                }
                            }
                            getAlbumFlow(browseId)
                        } else {
                            makeToast(getString(Res.string.error) + ": Null data")
                            _uiState.update {
                                it.copy(
                                    loadState = LocalPlaylistState.PlaylistLoadState.Error,
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        albumRepository.getAlbum(browseId).singleOrNull().let { albumEntity ->
                            if (albumEntity != null) {
                                _uiState.update {
                                    it.copy(
                                        browseId = browseId,
                                        title = albumEntity.title,
                                        thumbnail = albumEntity.thumbnails,
                                        artist =
                                            Artist(
                                                id = albumEntity.artistId?.firstOrNull(),
                                                name = albumEntity.artistName?.firstOrNull() ?: "",
                                            ),
                                        year = albumEntity.year ?: now().year.toString(),
                                        trackCount = albumEntity.trackCount,
                                        description = albumEntity.description,
                                        length = albumEntity.duration ?: "",
                                        listTrack =
                                            (
                                                songRepository
                                                    .getSongsByListVideoId(albumEntity.tracks ?: emptyList())
                                                    .singleOrNull() ?: emptyList()
                                            ).toArrayListTrack(),
                                        loadState = LocalPlaylistState.PlaylistLoadState.Success,
                                    )
                                }
                            } else {
                                log("Error: ${res.message}", LogLevel.ERROR)
                                makeToast(getString(Res.string.error) + ": ${res.message}")
                                _uiState.update {
                                    it.copy(
                                        loadState = LocalPlaylistState.PlaylistLoadState.Error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun setBrush(brush: List<Color>) {
        _uiState.update {
            it.copy(
                colors = brush,
            )
        }
    }

    fun setAlbumLike() {
        viewModelScope.launch {
            albumRepository.updateAlbumLiked(uiState.value.browseId, if (!uiState.value.liked) 1 else 0)
            _uiState.update {
                it.copy(
                    liked = !it.liked,
                )
            }
        }
    }

    private fun getAlbumFlow(browseId: String) {
        job?.cancel()
        collectDownloadStateJob?.cancel()
        job =
            viewModelScope.launch {
                albumRepository.getAlbumAsFlow(browseId).collectLatest { album ->
                    if (album != null) {
                        _uiState.update {
                            it.copy(
                                downloadState = album.downloadState,
                                liked = album.liked,
                            )
                        }
                    }
                }
            }
        collectDownloadStateJob =
            viewModelScope.launch {
                downloadUtils.downloadTask.collectLatest { downloadTask ->
                    var count = 0
                    uiState.value.listTrack.forEach { track ->
                        if (downloadTask[track.videoId] == DownloadState.STATE_DOWNLOADED) {
                            count++
                        }
                    }
                    if (count == uiState.value.listTrack.size) {
                        albumRepository.updateAlbumDownloadState(uiState.value.browseId, DownloadState.STATE_DOWNLOADED)
                        _uiState.update {
                            it.copy(
                                downloadState = DownloadState.STATE_DOWNLOADED,
                            )
                        }
                    }
                }
            }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            setQueueData(
                QueueData.Data(
                    listTracks = uiState.value.listTrack.toCollection(ArrayList()),
                    firstPlayedTrack = track,
                    playlistId = uiState.value.browseId.replaceFirst("VL", ""),
                    playlistName = "${getString(Res.string.album)} \"${uiState.value.title}\"",
                    playlistType = PlaylistType.PLAYLIST,
                    continuation = null,
                ),
            )
            val index = uiState.value.listTrack.indexOf(track)
            loadMediaItem(track, Config.ALBUM_CLICK, if (index == -1) 0 else index)
        }
    }

    fun shuffle() {
        viewModelScope.launch {
            if (uiState.value.listTrack.isEmpty()) {
                makeToast(getString(Res.string.playlist_is_empty))
                return@launch
            }
            val shuffleList = uiState.value.listTrack.shuffled()
            val randomIndex = shuffleList.indices.random()
            setQueueData(
                QueueData.Data(
                    listTracks = shuffleList.toCollection(ArrayList()),
                    firstPlayedTrack = shuffleList[randomIndex],
                    playlistId = uiState.value.browseId.replaceFirst("VL", ""),
                    playlistName = "${getString(Res.string.album)} \"${uiState.value.title}\"",
                    playlistType = PlaylistType.PLAYLIST,
                    continuation = null,
                ),
            )
            loadMediaItem(shuffleList[randomIndex], Config.ALBUM_CLICK, randomIndex)
        }
    }

    fun downloadFullAlbum() {
        viewModelScope.launch {
            // Insert all song to database
            uiState.value.listTrack.forEach { track ->
                songRepository.insertSong(track.toSongEntity()).singleOrNull()?.let {
                    log("Insert Song $it")
                }
            }
            val fullListSong =
                songRepository
                    .getSongsByListVideoId(uiState.value.listTrack.map { it.videoId })
                    .singleOrNull() ?: emptyList()
            log("Full list song: $fullListSong")
            if (fullListSong.isEmpty()) {
                makeToast(getString(Res.string.playlist_is_empty))
                return@launch
            }
            val listJob = fullListSong.filter { it.downloadState != DownloadState.STATE_DOWNLOADED }
            log("List job: $listJob")
            if (listJob.isEmpty()) {
                makeToast(getString(Res.string.downloaded))
                return@launch
            }
            albumRepository.updateAlbumDownloadState(uiState.value.browseId, DownloadState.STATE_DOWNLOADING)
            listJob.chunked(2).forEach { chunk ->
                chunk.forEach {
                    log("Download: ${it.videoId} ${it.thumbnails}")
                    downloadUtils.downloadTrack(
                        it.videoId,
                        it.title,
                        it.thumbnails ?: "",
                    )
                }
                delay(200)
            }
        }
    }

    fun cancelDownload() {
        viewModelScope.launch {
            albumRepository.updateAlbumDownloadState(uiState.value.browseId, DownloadState.STATE_NOT_DOWNLOADED)
            _uiState.update {
                it.copy(downloadState = DownloadState.STATE_NOT_DOWNLOADED)
            }
            uiState.value.listTrack.forEach {
                downloadUtils.removeDownload(it.videoId)
            }
            makeToast("Download removed")
        }
    }
}

data class AlbumUIState(
    val browseId: String = "",
    val title: String = "",
    val thumbnail: String? = null,
    val colors: List<Color> = listOf(Color.Black, md_theme_dark_background),
    val artist: Artist =
        Artist(
            id = null,
            name = "",
        ),
    val year: String = now().year.toString(),
    val downloadState: Int = DownloadState.STATE_NOT_DOWNLOADED,
    val liked: Boolean = false,
    val trackCount: Int = 0,
    val description: String? = null,
    val length: String = "",
    val listTrack: List<Track> = emptyList(),
    val otherVersion: List<ResultAlbum> = emptyList(),
    val loadState: LocalPlaylistState.PlaylistLoadState = LocalPlaylistState.PlaylistLoadState.Loading,
) {
    companion object {
        fun initial(): AlbumUIState = AlbumUIState()
    }
}

