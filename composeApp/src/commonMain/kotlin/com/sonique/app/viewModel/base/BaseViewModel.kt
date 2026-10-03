package com.sonique.app.viewModel.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonique.common.Config.RADIO_CLICK
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.domain.data.model.streams.YouTubeWatchEndpoint
import com.sonique.domain.mediaservice.handler.MediaPlayerHandler
import com.sonique.domain.mediaservice.handler.PlaylistType
import com.sonique.domain.mediaservice.handler.QueueData
import com.sonique.domain.repository.SongRepository
import com.sonique.domain.utils.Resource
import com.sonique.logger.LogLevel
import com.sonique.logger.Logger
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import multiplatform.network.cmptoast.ToastDuration
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.StringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.loading

abstract class BaseViewModel :
    ViewModel(),
    KoinComponent {
    protected val mediaPlayerHandler: MediaPlayerHandler by inject<MediaPlayerHandler>()
    private val baseSongRepository: SongRepository by inject<SongRepository>()
    private val _nowPlayingVideoId: MutableStateFlow<String> = MutableStateFlow("")

     
    val nowPlayingVideoId: StateFlow<String> get() = _nowPlayingVideoId

     
    protected val tag: String = javaClass.simpleName

     
    protected fun log(
        message: String,
        logType: LogLevel = LogLevel.WARN,
    ) {
        when (logType) {
            LogLevel.DEBUG -> Logger.d(tag, message)
            LogLevel.INFO -> Logger.i(tag, message)
            LogLevel.WARN -> Logger.w(tag, message)
            LogLevel.ERROR -> Logger.e(tag, message)
        }
    }

     
    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
        log("ViewModel cleared", LogLevel.WARN)
    }

    init {
        getNowPlayingVideoId()
    }

    fun makeToast(message: String?) {
        if (message.isNullOrBlank()) return
        com.sonique.app.ui.component.SoniqueToastManager.show(message)
    }

    protected suspend fun getString(resId: StringResource): String =
        org.jetbrains.compose.resources
            .getString(resId)

     
    private val _showLoadingDialog: MutableStateFlow<Pair<Boolean, String>> = MutableStateFlow(false to "")
    val showLoadingDialog: StateFlow<Pair<Boolean, String>> get() = _showLoadingDialog

    fun showLoadingDialog(message: String? = null) {
        viewModelScope.launch {
            _showLoadingDialog.value = true to (message ?: getString(Res.string.loading))
        }
    }

    fun hideLoadingDialog() {
        _showLoadingDialog.value = false to ""
    }

    private fun getNowPlayingVideoId() {
        viewModelScope.launch {
            combine(mediaPlayerHandler.nowPlayingState, mediaPlayerHandler.controlState) { nowPlayingState, controlState ->
                Pair(nowPlayingState, controlState)
            }.collect { (nowPlayingState, controlState) ->
                if (controlState.isPlaying) {
                    _nowPlayingVideoId.value = nowPlayingState.songEntity?.videoId ?: ""
                } else {
                    _nowPlayingVideoId.value = ""
                }
            }
        }
    }

     
    fun setQueueData(queueData: QueueData.Data) {
        mediaPlayerHandler.reset()
        mediaPlayerHandler.setQueueData(queueData)
    }

    fun <T> loadMediaItem(
        anyTrack: T,
        type: String,
        index: Int? = null,
    ) {
        viewModelScope.launch {
            mediaPlayerHandler.loadMediaItem(
                anyTrack = anyTrack,
                type = type,
                index = index,
            )
        }
    }

    fun shufflePlaylist(firstPlayIndex: Int = 0) {
        mediaPlayerHandler.shufflePlaylist(firstPlayIndex)
    }

    fun playRadio(
        playlistId: String,
        videoId: String? = null,
        title: String? = null,
    ) {
        viewModelScope.launch {
            baseSongRepository
                .getRadioFromEndpoint(
                    YouTubeWatchEndpoint(
                        videoId = videoId,
                        playlistId = playlistId,
                    ),
                )
                .collectLatest { res ->
                    val result = res.data
                    when (res) {
                        is Resource.Success -> {
                            val tracks = result?.first
                            if (!tracks.isNullOrEmpty()) {
                                val firstTrack = tracks.first()
                                mediaPlayerHandler.reset()
                                mediaPlayerHandler.setQueueData(
                                    QueueData.Data(
                                        listTracks = tracks.toCollection(arrayListOf()),
                                        firstPlayedTrack = firstTrack,
                                        playlistId = playlistId,
                                        playlistName = title ?: firstTrack.title,
                                        playlistType = PlaylistType.RADIO,
                                        continuation = result.second,
                                    ),
                                )
                                mediaPlayerHandler.loadMediaItem(
                                    anyTrack = firstTrack,
                                    type = RADIO_CLICK,
                                    index = 0,
                                )
                            } else {
                                makeToast("No tracks found for this station")
                            }
                        }
                        is Resource.Error -> {
                            Logger.e(tag, "playRadio error: ${res.message}")
                            makeToast(res.message)
                        }
                    }
                }
        }
    }
}

