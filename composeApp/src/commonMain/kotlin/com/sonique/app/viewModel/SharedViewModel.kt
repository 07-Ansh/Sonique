package com.sonique.app.viewModel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import com.sonique.common.Config.ALBUM_CLICK
import com.sonique.common.Config.DOWNLOAD_CACHE
import com.sonique.common.Config.PLAYLIST_CLICK
import com.sonique.common.Config.RECOVER_TRACK_QUEUE
import com.sonique.common.Config.SHARE
import com.sonique.common.Config.SONG_CLICK
import com.sonique.common.Config.VIDEO_CLICK
import com.sonique.common.SELECTED_LANGUAGE
import com.sonique.common.SUPPORTED_LANGUAGE
import com.sonique.common.STATUS_DONE
import com.sonique.domain.data.entities.AlbumEntity
import com.sonique.domain.data.entities.DownloadState
import com.sonique.domain.data.entities.LocalPlaylistEntity
import com.sonique.domain.data.entities.LyricsEntity
import com.sonique.domain.data.entities.NewFormatEntity
import com.sonique.domain.data.entities.PlaylistEntity
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.data.entities.SongInfoEntity
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.domain.data.model.canvas.CanvasResult
import com.sonique.domain.data.model.download.DownloadProgress
import com.sonique.domain.data.model.intent.GenericIntent
import com.sonique.domain.data.model.metadata.Lyrics
import com.sonique.domain.data.model.streams.TimeLine
import com.sonique.domain.extension.isSong
import com.sonique.domain.extension.isVideo
import com.sonique.domain.extension.toGenericMediaItem
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.manager.DataStoreManager.Values.TRUE
import com.sonique.domain.mediaservice.handler.ControlState
import com.sonique.domain.mediaservice.handler.DownloadHandler
import com.sonique.domain.mediaservice.handler.NowPlayingTrackState
import com.sonique.domain.mediaservice.handler.PlayerEvent
import com.sonique.domain.mediaservice.handler.PlaylistType
import com.sonique.domain.mediaservice.handler.QueueData
import com.sonique.domain.mediaservice.handler.RepeatState
import com.sonique.domain.mediaservice.handler.SimpleMediaState
import com.sonique.domain.mediaservice.handler.SleepTimerState
import com.sonique.domain.repository.AlbumRepository
import com.sonique.domain.repository.CacheRepository
import com.sonique.domain.repository.LocalPlaylistRepository
import com.sonique.domain.repository.LyricsCanvasRepository
import com.sonique.domain.repository.PlaylistRepository
import com.sonique.domain.repository.SongRepository
import com.sonique.domain.repository.StreamRepository
import com.sonique.domain.repository.UpdateRepository
import com.sonique.domain.utils.Resource
import com.sonique.domain.utils.toListName
import com.sonique.domain.utils.toLyrics
import com.sonique.domain.utils.toLyricsEntity
import com.sonique.domain.utils.toSongEntity
import com.sonique.domain.utils.toTrack
import com.sonique.logger.LogLevel
import com.sonique.logger.Logger
import com.sonique.app.Platform
import com.sonique.app.expect.getDownloadFolderPath
import com.sonique.app.expect.startWorker
import com.sonique.app.expect.ui.toByteArray
import com.sonique.app.getPlatform
import com.sonique.app.utils.VersionManager
import com.sonique.app.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.added_to_queue
import sonique.composeapp.generated.resources.added_to_youtube_liked
import sonique.composeapp.generated.resources.error
import sonique.composeapp.generated.resources.play_next
import sonique.composeapp.generated.resources.removed_from_youtube_liked
import sonique.composeapp.generated.resources.shared
import sonique.composeapp.generated.resources.updated
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModel(
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
    private val updateRepository: UpdateRepository,
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val lyricsCanvasRepository: LyricsCanvasRepository,
    private val cacheRepository: CacheRepository,
) : BaseViewModel() {
    var isFirstLiked: Boolean = false
    var isFirstMiniplayer: Boolean = false
    var isFirstSuggestions: Boolean = false
    var showedUpdateDialog: Boolean = false

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate

    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked: SharedFlow<Boolean> = _liked.asSharedFlow()

    var isServiceRunning: Boolean = false

    private var _sleepTimerState = MutableStateFlow(SleepTimerState(false, 0))
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState

    private var regionCode: String? = null
    private var language: String? = null
    private var quality: String? = null

    private var _format: MutableStateFlow<NewFormatEntity?> = MutableStateFlow(null)
    val format: SharedFlow<NewFormatEntity?> = _format.asSharedFlow()

    private var _canvas: MutableStateFlow<CanvasResult?> = MutableStateFlow(null)
    val canvas: StateFlow<CanvasResult?> = _canvas

    private var canvasJob: Job? = null

    private val _intent: MutableStateFlow<GenericIntent?> = MutableStateFlow(null)
    val intent: StateFlow<GenericIntent?> = _intent

    private var getFormatFlowJob: Job? = null

    var playlistId: MutableStateFlow<String?> = MutableStateFlow(null)

    var isFullScreen: Boolean = false

    private var _nowPlayingState = MutableStateFlow<NowPlayingTrackState?>(null)
    val nowPlayingState: StateFlow<NowPlayingTrackState?> = _nowPlayingState

    fun getQueueDataState() = mediaPlayerHandler.queueData
    val queueData = mediaPlayerHandler.queueData



    private var _controllerState =
        MutableStateFlow<ControlState>(
            ControlState(
                isPlaying = false,
                isShuffle = false,
                repeatState = RepeatState.None,
                isLiked = false,
                isNextAvailable = false,
                isPreviousAvailable = false,
                isCrossfading = false,
                volume = 1f,
            ),
        )
    val controllerState: StateFlow<ControlState> = _controllerState
    private val _getVideo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val getVideo: StateFlow<Boolean> = _getVideo

    private var _timeline =
        MutableStateFlow<TimeLine>(
            TimeLine(
                current = -1L,
                total = -1L,
                bufferedPercent = 0,
                loading = true,
            ),
        )
    val timeline: StateFlow<TimeLine> = _timeline

    private var _nowPlayingScreenData =
        MutableStateFlow<NowPlayingScreenData>(
            NowPlayingScreenData.initial(),
        )
    val nowPlayingScreenData: StateFlow<NowPlayingScreenData> = _nowPlayingScreenData

    private var _likeStatus = MutableStateFlow<Boolean>(false)
    val likeStatus: StateFlow<Boolean> = _likeStatus

    val openAppTime: StateFlow<Int> = dataStoreManager.openAppTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)
    private val _shareSavedLyrics: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val shareSavedLyrics: StateFlow<Boolean> get() = _shareSavedLyrics

    init {
        viewModelScope.launch {
            log("SharedViewModel init")
            if (dataStoreManager.appVersion.first() != VersionManager.getVersionName()) {
                dataStoreManager.resetOpenAppTime()
                dataStoreManager.setAppVersion(
                    VersionManager.getVersionName(),
                )
            }
            dataStoreManager.openApp()
            if (getPlatform() == Platform.Desktop) {
                dataStoreManager.setWatchVideoInsteadOfPlayingAudio(false)
            }
            dataStoreManager.getString("miniplayer_guide").first().let {
                isFirstMiniplayer = it != STATUS_DONE
            }
            dataStoreManager.getString("suggest_guide").first().let {
                isFirstSuggestions = it != STATUS_DONE
            }
            dataStoreManager.getString("liked_guide").first().let {
                isFirstLiked = it != STATUS_DONE
            }

                    nowPlayingState
                        .filterNotNull()
                        .flatMapLatest { nowPlayingState ->
                            timeline.map { timeLine ->
                                Pair(timeLine, nowPlayingState)
                            }
                        }.distinctUntilChanged { old, new ->
                            (old.first.total.toString() + old.second.songEntity?.videoId).hashCode() ==
                                (new.first.total.toString() + new.second.songEntity?.videoId).hashCode()
                        }.collectLatest {
                            log("Timeline job ${(it.first.total.toString() + it.second.songEntity?.videoId).hashCode()}")
                            val nowPlaying = it.second
                            val timeline = it.first
                            if (timeline.total > 0 && nowPlaying.songEntity != null) {
                                if (nowPlaying.mediaItem.isSong() && nowPlayingScreenData.value.canvasData == null) {
                                    Logger.w(tag, "Duration is ${timeline.total}")
                                    Logger.w(tag, "MediaId is ${nowPlaying.mediaItem.mediaId}")
                                    getCanvas(nowPlaying.mediaItem.mediaId, (timeline.total / 1000).toInt())
                                }
                                nowPlaying.songEntity?.let { song ->
                                    if (nowPlayingScreenData.value.lyricsData == null) {
                                        Logger.w(tag, "Get lyrics from format")
                                        getLyricsFromFormat(nowPlaying.mediaItem.isVideo(), song, (timeline.total / 1000).toInt())
                                    }
                                }
                            }
                        }

            val checkGetVideoJob =
                launch {
                    dataStoreManager.watchVideoInsteadOfPlayingAudio.collectLatest {
                        Logger.w(tag, "GetVideo is $it")
                        _getVideo.value = it == TRUE
                    }
                }
            val lyricsProviderJob =
                launch {
                    dataStoreManager.lyricsProvider.distinctUntilChanged().collectLatest {
                        setLyricsProvider()
                    }
                }

 
 
 
 
 
 
 
 
 
 
            checkGetVideoJob.join()
            lyricsProviderJob.join()

 

 
        }
        viewModelScope.launch {
            mediaPlayerHandler.nowPlayingState
                .distinctUntilChangedBy {
                    it.songEntity?.videoId
                }.collectLatest { state ->
                    Logger.w(tag, "NowPlayingState is $state")
                    canvasJob?.cancel()
                    _nowPlayingState.value = state
                    state.track?.let { track ->
                        _nowPlayingScreenData.value =
                            NowPlayingScreenData(
                                nowPlayingTitle = track.title,
                                artistName =
                                    track
                                        .artists
                                        .toListName()
                                        .joinToString(", "),
                                isVideo = false,
                                thumbnailURL = null,
                                canvasData = null,
                                lyricsData = null,
                                songInfoData = null,
                                playlistName =
                                    mediaPlayerHandler.queueData.value
                                        ?.data
                                        ?.playlistName ?: "",
                            )
                    }
                    state.mediaItem.let { now ->
                        _canvas.value = null
                        getLikeStatus(now.mediaId)
                        getSongInfo(now.mediaId)
                        getFormat(now.mediaId)
                        _nowPlayingScreenData.update {
                            it.copy(
                                isVideo = now.isVideo(),
                            )
                        }
                    }
                    state.songEntity?.let { song ->
                        _liked.value = song.liked == true
                        _nowPlayingScreenData.update {
                            it.copy(
                                thumbnailURL = song.thumbnails,
                                isExplicit = song.isExplicit,
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            val job1 =
                launch {
                    mediaPlayerHandler.simpleMediaState.collect { mediaState ->
                        when (mediaState) {
                            is SimpleMediaState.Buffering -> {
                                _timeline.update {
                                    it.copy(
                                        loading = true,
                                    )
                                }
                            }

                            SimpleMediaState.Initial -> {
                                _timeline.update { it.copy(loading = true) }
                            }

                            SimpleMediaState.Ended -> {
                                _timeline.update {
                                    it.copy(
                                        current = -1L,
                                        total = -1L,
                                        bufferedPercent = 0,
                                        loading = true,
                                    )
                                }
                            }

                            is SimpleMediaState.Progress -> {
                                if (mediaState.progress >= 0L && mediaState.progress != _timeline.value.current) {
                                    if (_timeline.value.total > 0L) {
                                        _timeline.update {
                                            it.copy(
                                                current = mediaState.progress,
                                                loading = false,
                                            )
                                        }
                                    } else {
                                        _timeline.update {
                                            it.copy(
                                                current = mediaState.progress,
                                                loading = true,
                                                total = mediaPlayerHandler.getPlayerDuration(),
                                            )
                                        }
                                    }
                                } else {
                                    _timeline.update {
                                        it.copy(
                                            loading = true,
                                        )
                                    }
                                }
                            }

                            is SimpleMediaState.Loading -> {
                                _timeline.update {
                                    it.copy(
                                        bufferedPercent = mediaState.bufferedPercentage,
                                        total = mediaState.duration,
                                    )
                                }
                            }

                            is SimpleMediaState.Ready -> {
                                _timeline.update {
                                    it.copy(
                                        current = mediaPlayerHandler.getProgress(),
                                        loading = false,
                                        total = mediaState.duration,
                                    )
                                }
                            }
                        }
                    }
                }
            val controllerJob =
                launch {
                    Logger.w(tag, "ControllerJob is running")
                    mediaPlayerHandler.controlState.collectLatest {
                        Logger.w(tag, "ControlState is $it")
                        _controllerState.value = it
                    }
                }
            val sleepTimerJob =
                launch {
                    mediaPlayerHandler.sleepTimerState.collectLatest {
                        _sleepTimerState.value = it
                    }
                }
            val playlistNameJob =
                launch {
                    mediaPlayerHandler.queueData.collectLatest {
                        _nowPlayingScreenData.update {
                            it.copy(playlistName = it.playlistName)
                        }
                    }
                }
            job1.join()
            controllerJob.join()
            sleepTimerJob.join()
            playlistNameJob.join()
        }
         
        checkAllDownloadingSongs()
        checkAllDownloadingPlaylists()
        checkAllDownloadingLocalPlaylists()
    }

    fun setIntent(intent: GenericIntent?) {
        _intent.value = intent
    }



    private fun getLikeStatus(videoId: String?) {
        viewModelScope.launch {
            if (videoId != null) {
                _likeStatus.value = false
                songRepository.getLikeStatus(videoId).collectLatest { status ->
                    _likeStatus.value = status
                }
            }
        }
    }

    private fun getCanvas(
        videoId: String,
        duration: Int,
    ) {
        Logger.w(tag, "Start getCanvas: $videoId $duration")
 
        viewModelScope.launch {
            if (dataStoreManager.spotifyCanvas.first() == TRUE) {
                lyricsCanvasRepository.getCanvas(dataStoreManager, videoId, duration).cancellable().collect { response ->
                    val data = response.data
                    when (response) {
                        is Resource.Success if (data != null && nowPlayingState.value?.mediaItem?.mediaId == videoId) -> {
                            _canvas.value = data
                            _nowPlayingScreenData.update {
                                it.copy(
                                    canvasData =
                                        NowPlayingScreenData.CanvasData(
                                            isVideo = data.isVideo,
                                            url = data.canvasUrl,
                                        ),
                                )
                            }
                             
                            if (data.isVideo) lyricsCanvasRepository.updateCanvasUrl(videoId, data.canvasUrl)
                             
                            data.canvasThumbUrl?.let { lyricsCanvasRepository.updateCanvasThumbUrl(videoId, it) }
                        }

                        else -> {
                            log("Get canvas error: ${response.message}", LogLevel.WARN)
                            nowPlayingState.value?.songEntity?.canvasUrl?.let { url ->
                                _nowPlayingScreenData.update {
                                    it.copy(
                                        canvasData =
                                            NowPlayingScreenData.CanvasData(
                                                isVideo = url.contains(".mp4"),
                                                url = url,
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun getString(key: String): String? = dataStoreManager.getString(key).first()

    fun putString(
        key: String,
        value: String,
    ) {
        viewModelScope.launch { dataStoreManager.putString(key, value) }
    }

    fun setSleepTimer(minutes: Int) {
        mediaPlayerHandler.sleepStart(minutes)
    }

    fun stopSleepTimer() {
        mediaPlayerHandler.sleepStop()
    }

    private var _downloadState: MutableStateFlow<DownloadHandler.Download?> = MutableStateFlow(null)
    var downloadState: StateFlow<DownloadHandler.Download?> = _downloadState.asStateFlow()

    fun checkIsRestoring() {
        viewModelScope.launch {
            val downloadedCacheKeys = cacheRepository.getAllCacheKeys(DOWNLOAD_CACHE)
            songRepository.getDownloadedSongs().first().let { songs ->
                songs?.forEach { song ->
                    if (!downloadedCacheKeys.contains(song.videoId)) {
                        songRepository.updateDownloadState(
                            song.videoId,
                            DownloadState.STATE_NOT_DOWNLOADED,
                        )
                    }
                }
            }
            playlistRepository.getAllDownloadedPlaylist().first().let { list ->
                for (data in list) {
                    when (data) {
                        is AlbumEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                albumRepository.updateAlbumDownloadState(
                                    data.browseId,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is PlaylistEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                playlistRepository.updatePlaylistDownloadState(
                                    data.id,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is LocalPlaylistEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                localPlaylistRepository.updateLocalPlaylistDownloadState(
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                    data.id,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun insertLyrics(lyrics: LyricsEntity) {
        viewModelScope.launch {
            lyricsCanvasRepository.insertLyrics(lyrics)
        }
    }

    private fun getSavedLyrics(track: Track) {
        viewModelScope.launch {
            lyricsCanvasRepository.getSavedLyrics(track.videoId).cancellable().collectLatest { lyrics ->
                if (lyrics != null) {
                    val lyricsData = lyrics.toLyrics()
                    Logger.d(tag, "Saved Lyrics $lyricsData")
                    updateLyrics(
                        track.videoId,
                        track.durationSeconds ?: 0,
                        lyricsData,
                        false,
                        LyricsProvider.OFFLINE,
                    )

                }
            }
        }
    }

    fun loadSharedMediaItem(videoId: String) {
        viewModelScope.launch {
            streamRepository.getFullMetadata(videoId).collectLatest { response ->
                val track = response.data
                when (response) {
                    is Resource.Success if (track != null) -> {
                        mediaPlayerHandler.setQueueData(
                            QueueData.Data(
                                listTracks = arrayListOf(track),
                                firstPlayedTrack = track,
                                playlistId = "RDAMVM$videoId",
                                playlistName = getString(Res.string.shared),
                                playlistType = PlaylistType.RADIO,
                                continuation = null,
                            ),
                        )
                        loadMediaItemFromTrack(track, SONG_CLICK)
                    }

                    else -> {
                        log("Load shared media item error: ${response.message}", LogLevel.WARN)
                        makeToast("${getString(Res.string.error)}: ${response.message}")
                    }
                }
            }
        }
    }

    fun loadMediaItemFromTrack(
        track: Track,
        type: String,
        index: Int? = null,
    ) {
        viewModelScope.launch {
            quality = dataStoreManager.quality.first()
            mediaPlayerHandler.clearMediaItems()
            songRepository.insertSong(track.toSongEntity()).lastOrNull()?.let {
                println("insertSong: $it")
                songRepository
                    .getSongById(track.videoId)
                    .collect { songEntity ->
                        if (songEntity != null) {
                            Logger.w("Check like", "loadMediaItemFromTrack ${songEntity.liked}")
                            _liked.value = songEntity.liked
                        }
                    }
            }
            track.durationSeconds?.let {
                songRepository.updateDurationSeconds(
                    it,
                    track.videoId,
                )
            }
            withContext(Dispatchers.Main) {
                mediaPlayerHandler.addMediaItem(track.toGenericMediaItem(), playWhenReady = type != RECOVER_TRACK_QUEUE)
            }

            when (type) {
                SONG_CLICK -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                VIDEO_CLICK -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                SHARE -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                PLAYLIST_CLICK -> {
                    if (index == null) {
 
                        loadPlaylistOrAlbum(index = 0)
                    } else {
 
                        loadPlaylistOrAlbum(index = index)
                    }
                }

                ALBUM_CLICK -> {
                    if (index == null) {
 
                        loadPlaylistOrAlbum(index = 0)
                    } else {
 
                        loadPlaylistOrAlbum(index = index)
                    }
                }
            }
        }
    }

    fun onUIEvent(uiEvent: UIEvent) =
        viewModelScope.launch {
            when (uiEvent) {
                UIEvent.Backward ->
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Backward,
                    )

                UIEvent.Forward -> mediaPlayerHandler.onPlayerEvent(PlayerEvent.Forward)
                UIEvent.PlayPause ->
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.PlayPause,
                    )

                UIEvent.Next -> mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next)
                UIEvent.Previous ->
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Previous,
                    )

                UIEvent.Stop -> mediaPlayerHandler.onPlayerEvent(PlayerEvent.Stop)
                is UIEvent.UpdateProgress -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.UpdateProgress(
                            uiEvent.newProgress,
                        ),
                    )
                }

                UIEvent.Repeat -> mediaPlayerHandler.onPlayerEvent(PlayerEvent.Repeat)
                UIEvent.Shuffle -> mediaPlayerHandler.onPlayerEvent(PlayerEvent.Shuffle)
                UIEvent.ToggleLike -> {
                    Logger.w(tag, "ToggleLike")
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.ToggleLike)
                }

                is UIEvent.UpdateVolume -> {
                    val newVolume = uiEvent.newVolume
                    dataStoreManager.setPlayerVolume(newVolume)
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.UpdateVolume(newVolume))
                }
            }
        }

    override fun onCleared() {
        Logger.w("Check onCleared", "onCleared")
    }

    fun getLocation() {
        viewModelScope.launch {
            regionCode = dataStoreManager.location.first() ?: ""
            quality = dataStoreManager.quality.first() ?: ""
            language =
                dataStoreManager.getString(SELECTED_LANGUAGE).first()
                    ?: SUPPORTED_LANGUAGE.codes.first()
        }
    }

    private fun checkAllDownloadingLocalPlaylists() {
        viewModelScope.launch {
            localPlaylistRepository.getAllDownloadingLocalPlaylists().collectLatest { playlists ->
                playlists.forEach { playlist ->
                    localPlaylistRepository.updateDownloadState(playlist.id, 0, successMessage = getString(Res.string.updated)).lastOrNull()
                }
            }
        }
    }

    private fun checkAllDownloadingPlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllDownloadingPlaylist().collectLatest { list ->
                list.forEach { data ->
                    when (data) {
                        is AlbumEntity -> {
                            albumRepository.updateAlbumDownloadState(data.browseId, 0)
                        }

                        is PlaylistEntity -> {
                            playlistRepository.updatePlaylistDownloadState(data.id, 0)
                        }

                        else -> {
                             
                        }
                    }
                }
            }
        }
    }

    private fun checkAllDownloadingSongs() {
        viewModelScope.launch {
            songRepository.getDownloadingSongs().collect { songs ->
                songs?.forEach { song ->
                    songRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                }
            }
            songRepository.getPreparingSongs().collect { songs ->
                songs.forEach { song ->
                    songRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                }
            }
        }
    }

    private fun getFormat(mediaId: String?) {
        if (mediaId != _format.value?.videoId && !mediaId.isNullOrEmpty()) {
            _format.value = null
            getFormatFlowJob?.cancel()
            getFormatFlowJob =
                viewModelScope.launch {
                    streamRepository.getFormatFlow(mediaId).cancellable().collectLatest { f ->
                        Logger.w(tag, "Get format for $mediaId: $f")
                        if (f != null) {
                            _format.emit(f)
                        } else {
                            _format.emit(null)
                        }
                    }
                }
        }
    }

    private var songInfoJob: Job? = null

    fun getSongInfo(mediaId: String?) {
        songInfoJob?.cancel()
        songInfoJob =
            viewModelScope.launch {
                if (mediaId != null) {
                    songRepository.getSongInfo(mediaId).collect { song ->
                        _nowPlayingScreenData.update {
                            it.copy(
                                songInfoData = song,
                            )
                        }
                    }
                }
            }
    }





    fun stopPlayer() {
        _nowPlayingScreenData.value = NowPlayingScreenData.initial()
        _nowPlayingState.value = null
        mediaPlayerHandler.resetSongAndQueue()
        onUIEvent(UIEvent.Stop)
    }

    private fun loadPlaylistOrAlbum(index: Int? = null) {
        mediaPlayerHandler.loadPlaylistOrAlbum(index)
    }

    private fun updateLyrics(
        videoId: String,
        duration: Int,  
        lyrics: Lyrics?,
        isTranslatedLyrics: Boolean,
        lyricsProvider: LyricsProvider = LyricsProvider.LRCLIB,
    ) {
        if (lyrics == null) {
            _nowPlayingScreenData.update {
                it.copy(
                    lyricsData = null,
                )
            }
            return
        }

        if (isTranslatedLyrics) {
            val originalLyrics = _nowPlayingScreenData.value.lyricsData?.lyrics
            val originalLines = originalLyrics?.lines
            val lyricsLines = lyrics.lines
            if (originalLyrics != null && originalLines != null && lyricsLines != null) {
                var outOfSyncCount = 0

                originalLines.forEach { originalLine ->
                    val originalTime = originalLine.startTimeMs.toLongOrNull() ?: 0L
                    val closestTranslatedLine =
                        lyricsLines.minByOrNull {
                            abs((it.startTimeMs.toLongOrNull() ?: 0L) - originalTime)
                        }

                    if (closestTranslatedLine != null) {
                        val translatedTime = closestTranslatedLine.startTimeMs.toLongOrNull() ?: 0L
                        val timeDiff = abs(originalTime - translatedTime)

                        if (timeDiff > 1000L) {  
                            outOfSyncCount++
                        }
                        if (closestTranslatedLine.words == originalLine.words) {
                            outOfSyncCount++
                        }
                    }
                }

                if (outOfSyncCount > 5) {
                    Logger.w(tag, "Translated lyrics out of sync: $outOfSyncCount lines with time diff > 1s")

                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = null,
                                ),
                        )
                    }

                    viewModelScope.launch {
                        lyricsCanvasRepository.removeTranslatedLyrics(
                            videoId,
                            dataStoreManager.translationLanguage.first(),
                        )
                        log("Removed out-of-sync translated lyrics for $videoId")
                    }

                    return
                }
            }
        }


        if (_nowPlayingState.value?.songEntity?.videoId == videoId) {
            val track = _nowPlayingState.value?.track
            when (isTranslatedLyrics) {
                true -> {
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = lyrics to lyricsProvider,
                                ),
                        )
                    }

                }

                false -> {
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                NowPlayingScreenData.LyricsData(
                                    lyrics = lyrics,
                                    lyricsProvider = lyricsProvider,
                                ),
                        )
                    }
                     
                    viewModelScope.launch {
                        lyricsCanvasRepository.insertLyrics(
                            LyricsEntity(
                                videoId = videoId,
                                error = false,
                                lines = lyrics.lines,
                                syncType = lyrics.syncType,
                            ),
                        )
                    }

                }
            }
        }
    }

    private fun getLyricsFromFormat(
        isVideo: Boolean,
        song: SongEntity,
        duration: Int,
    ) {
        viewModelScope.launch {
            val videoId = song.videoId
            log("Get Lyrics From Format for $videoId", LogLevel.WARN)
            val artistName = song.artistName
            val artist =
                if (artistName?.firstOrNull() != null &&
                    artistName
                        .firstOrNull()
                        ?.contains("Various Artists") == false
                ) {
                    artistName.firstOrNull()
                } else {
                    mediaPlayerHandler.nowPlaying
                        .first()
                        ?.metadata
                        ?.artist
                        ?: ""
                }
            val lyricsProvider = dataStoreManager.lyricsProvider.first()
            if (isVideo) {
                getYouTubeCaption(
                    videoId,
                    song,
                    (artist ?: "").toString(),
                    duration,
                )
            } else {
                when (lyricsProvider) {
                    DataStoreManager.LRCLIB -> {
                        getLrclibLyrics(
                            song,
                            (artist ?: "").toString(),
                            duration,
                        )
                    }

                    DataStoreManager.YOUTUBE -> {
                    }
                }
            }
        }
    }



    private suspend fun getYouTubeCaption(
        videoId: String,
        song: SongEntity,
        artist: String?,
        duration: Int,
    ) {
        lyricsCanvasRepository
            .getYouTubeCaption(dataStoreManager.youtubeSubtitleLanguage.first(), videoId)
            .cancellable()
            .collect { response ->
                val data = response.data
                when (response) {
                    is Resource.Success if (data != null) -> {
                        val lyrics = data.first
                        val translatedLyrics = data.second
                        insertLyrics(lyrics.toLyricsEntity(videoId))
                        updateLyrics(
                            videoId,
                            duration,
                            lyrics,
                            false,
                            LyricsProvider.YOUTUBE,
                        )
                        if (translatedLyrics != null) {
                            updateLyrics(
                                videoId,
                                duration,
                                translatedLyrics,
                                true,
                                LyricsProvider.YOUTUBE,
                            )
                        } else {

                        }
                    }

                    else -> {
                        getLrclibLyrics(
                            song,
                            (artist ?: ""),
                            duration,
                        )
                    }
                }
            }
    }

    private fun getLrclibLyrics(
        song: SongEntity,
        artist: String,
        duration: Int,
    ) {
        viewModelScope.launch {
            lyricsCanvasRepository
                .getLrclibLyricsData(
                    artist,
                    song.title,
                    duration,
                ).collectLatest { res ->
                    val data = res.data
                    when (res) {
                        is Resource.Success if (data != null) -> {
                            Logger.d(tag, "Get Lyrics Data Success")
                            updateLyrics(
                                song.videoId,
                                duration,
                                res.data,
                                false,
                                LyricsProvider.LRCLIB,
                            )
                            insertLyrics(
                                res.data?.toLyricsEntity(
                                    song.videoId,
                                ) ?: return@collectLatest,
                            )

                        }

                        else -> {
                            getSavedLyrics(
                                song.toTrack().copy(
                                    durationSeconds = duration,
                                ),
                            )
                        }
                    }
                }
        }
    }





    private fun getSpotifyLyrics(
        track: Track,
        query: String,
        duration: Int? = null,
    ) {
        viewModelScope.launch {
            Logger.d("Check SpotifyLyrics", "SpotifyLyrics $query")
            lyricsCanvasRepository.getSpotifyLyrics(dataStoreManager, query, duration).cancellable().collect { response ->
                Logger.d("Check SpotifyLyrics", response.toString())
                val data = response.data
                when (response) {
                    is Resource.Success -> {
                        if (data != null) {
                            insertLyrics(
                                data.toLyricsEntity(
                                    track.videoId,
                                ),
                            )
                            updateLyrics(
                                track.videoId,
                                duration ?: 0,
                                data,
                                false,
                                LyricsProvider.SPOTIFY,
                            )

                        }
                    }

                    else -> {
                        getLrclibLyrics(
                            track.toSongEntity(),
                            track.artists.toListName().firstOrNull() ?: "",
                            duration ?: 0,
                        )
                    }
                }
            }
        }
    }

    fun setLyricsProvider() {
        viewModelScope.launch {
            val songEntity = nowPlayingState.value?.songEntity ?: return@launch
            val isVideo = nowPlayingState.value?.mediaItem?.isVideo() ?: false
            getLyricsFromFormat(isVideo, songEntity, timeline.value.total.toInt() / 1000)
        }
    }

    private var _recreateActivity: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recreateActivity: StateFlow<Boolean> = _recreateActivity

    fun activityRecreate() {
        _recreateActivity.value = true
    }

    fun activityRecreateDone() {
        _recreateActivity.value = false
    }

    fun addListToQueue(listTrack: ArrayList<Track>) {
        viewModelScope.launch {
            if (listTrack.size == 1 && dataStoreManager.endlessQueue.first() == TRUE) {
                mediaPlayerHandler.playNext(listTrack.first())
                makeToast(getString(Res.string.play_next))
            } else {
                mediaPlayerHandler.loadMoreCatalog(listTrack)
                makeToast(getString(Res.string.added_to_queue))
            }
        }
    }

    fun addToYouTubeLiked() {
        viewModelScope.launch {
            val videoId = mediaPlayerHandler.nowPlaying.first()?.mediaId
            if (videoId != null) {
                val like = likeStatus.value
                if (!like) {
                    songRepository
                        .addToYouTubeLiked(
                            mediaPlayerHandler.nowPlaying.first()?.mediaId,
                        ).collect { response ->
                            if (response == 200) {
                                makeToast(getString(Res.string.added_to_youtube_liked))
                                getLikeStatus(videoId)
                            } else {
                                makeToast(getString(Res.string.error))
                            }
                        }
                } else {
                    songRepository
                        .removeFromYouTubeLiked(
                            mediaPlayerHandler.nowPlaying.first()?.mediaId,
                        ).collect {
                            if (it == 200) {
                                makeToast(getString(Res.string.removed_from_youtube_liked))
                                getLikeStatus(videoId)
                            } else {
                                makeToast(getString(Res.string.error))
                            }
                        }
                }
            }
        }
    }

    fun getTranslucentBottomBar() = dataStoreManager.translucentBottomBar

    fun getEnableLiquidGlass() = dataStoreManager.enableLiquidGlass

    private val _reloadDestination: MutableStateFlow<KClass<*>?> = MutableStateFlow(null)
    val reloadDestination: StateFlow<KClass<*>?> = _reloadDestination.asStateFlow()

    fun reloadDestination(destination: KClass<*>) {
        _reloadDestination.value = destination
    }

    fun reloadDestinationDone() {
        _reloadDestination.value = null
    }

    fun shouldCheckForUpdate(): Boolean = runBlocking { dataStoreManager.autoCheckForUpdates.first() == TRUE }

    fun runWorker() {
        Logger.w("Check Worker", "Worker")
        startWorker()
    }

    private var _downloadFileProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.INIT)
    val downloadFileProgress: StateFlow<DownloadProgress> get() = _downloadFileProgress

    fun downloadFile(bitmap: ImageBitmap) {
        val fileName =
            "${nowPlayingScreenData.value.nowPlayingTitle} - ${nowPlayingScreenData.value.artistName}"
                .replace(Regex("""[|\\?*<":>]"""), "")
                .replace(" ", "_")
        val path =
            "${getDownloadFolderPath()}/$fileName"
        viewModelScope.launch {
            nowPlayingState.value?.track?.let { track ->
                val bytesArray = bitmap.toByteArray()
                try {
                    val fileOutputStream = FileOutputStream("$path.jpg")
                    fileOutputStream.write(bytesArray)
                    fileOutputStream.close()
                    Logger.d(tag, "Thumbnail saved to $path.jpg")
                } catch (e: java.lang.Exception) {
                    throw RuntimeException(e)
                }
                songRepository
                    .downloadToFile(
                        track = track,
                        videoId = track.videoId,
                        path = path,
                        isVideo = nowPlayingScreenData.value.isVideo,
                    ).collectLatest {
                        _downloadFileProgress.value = it
                    }
            }
        }
    }

    fun downloadFileDone() {
        _downloadFileProgress.value = DownloadProgress.INIT
    }



    fun setBitmap(bitmap: ImageBitmap?) {
        _nowPlayingScreenData.update {
            it.copy(bitmap = bitmap)
        }
    }

    fun shouldStopMusicService(): Boolean = runBlocking { dataStoreManager.killServiceOnExit.first() == TRUE }

    val isLoggedIn: StateFlow<Boolean> = dataStoreManager.cookie
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false
        )

    suspend fun isUserLoggedInSus(): Boolean = dataStoreManager.cookie.first().isNotEmpty()

    fun isCombineFavoriteAndYTLiked(): Boolean = runBlocking { dataStoreManager.combineLocalAndYouTubeLiked.first() == TRUE }
}

sealed class UIEvent {
    data object PlayPause : UIEvent()

    data object Backward : UIEvent()

    data object Forward : UIEvent()

    data object Next : UIEvent()

    data object Previous : UIEvent()

    data object Stop : UIEvent()

    data object Shuffle : UIEvent()

    data object Repeat : UIEvent()

    data class UpdateProgress(
        val newProgress: Float,
    ) : UIEvent()

    data class UpdateVolume(
        val newVolume: Float,
    ) : UIEvent()

    data object ToggleLike : UIEvent()
}

enum class LyricsProvider {
    YOUTUBE,
    SPOTIFY,
    LRCLIB,
    AI,
    OFFLINE,
}

data class NowPlayingScreenData(
    val playlistName: String,
    val nowPlayingTitle: String,
    val artistName: String,
    val isVideo: Boolean,
    val isExplicit: Boolean = false,
    val thumbnailURL: String?,
    val canvasData: CanvasData? = null,
    val lyricsData: LyricsData? = null,
    val songInfoData: SongInfoEntity? = null,
    val bitmap: ImageBitmap? = null,
) {
    data class CanvasData(
        val isVideo: Boolean,
        val url: String,
    )

    data class LyricsData(
        val lyrics: Lyrics,
        val translatedLyrics: Pair<Lyrics, LyricsProvider>? = null,
        val lyricsProvider: LyricsProvider,
    )

    companion object {
        fun initial(): NowPlayingScreenData =
            NowPlayingScreenData(
                nowPlayingTitle = "",
                artistName = "",
                isVideo = false,
                thumbnailURL = null,
                canvasData = null,
                lyricsData = null,
                songInfoData = null,
                playlistName = "",
            )
    }
}

