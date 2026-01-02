package com.sonique.media_jvm.download

import com.sonique.common.MERGING_DATA_TYPE
import com.sonique.domain.data.entities.DownloadState
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.mediaservice.handler.DownloadHandler
import com.sonique.domain.repository.SongRepository
import com.sonique.domain.repository.StreamRepository
import com.sonique.domain.utils.toTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch
import java.io.File

internal class DownloadUtils(
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
    private val songRepository: SongRepository,
) : DownloadHandler {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var _downloads = MutableStateFlow<Map<String, Pair<DownloadHandler.Download?, DownloadHandler.Download?>>>(emptyMap())

    // Audio / Video
    override val downloads: StateFlow<Map<String, Pair<DownloadHandler.Download?, DownloadHandler.Download?>>>
        get() = _downloads
    private val _downloadTask = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val downloadTask: StateFlow<Map<String, Int>> get() = _downloadTask

    val downloadingVideoIds = MutableStateFlow<MutableSet<String>>(mutableSetOf())

    init {
    }

    override suspend fun downloadTrack(
        videoId: String,
        title: String,
        thumbnail: String,
    ) {
        val song = songRepository.getSongById(videoId).lastOrNull()
        if (song != null) {
            songRepository.updateDownloadState(
                videoId,
                DownloadState.STATE_DOWNLOADING,
            )
            if (!File(getDownloadPath()).exists()) {
                File(getDownloadPath()).mkdirs()
            }
            songRepository
                .downloadToFile(
                    song.toTrack(),
                    path = getDownloadPath() + File.separator + videoId,
                    videoId = videoId,
                    isVideo = false,
                ).collect { state ->
                    if (state.isError) {
                        songRepository.updateDownloadState(
                            videoId,
                            DownloadState.STATE_NOT_DOWNLOADED,
                        )
                    } else if (state.isDone) {
                        songRepository.updateDownloadState(
                            videoId,
                            DownloadState.STATE_DOWNLOADED,
                        )
                    }
                }
        }
    }

    override fun removeDownload(videoId: String) {
        File(getDownloadPath())
            .listFiles()
            .filter {
                it.name.contains(videoId)
            }.forEach {
                it.delete()
                coroutineScope.launch {
                    songRepository.updateDownloadState(
                        videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                }
            }
    }

    override fun removeAllDownloads() {
        File(getDownloadPath()).listFiles().forEach {
            it.delete()
            coroutineScope.launch {
                songRepository.updateDownloadState(
                    it.name.split(".").first().removePrefix(
                        MERGING_DATA_TYPE.VIDEO,
                    ),
                    DownloadState.STATE_NOT_DOWNLOADED,
                )
            }
        }
    }
}

fun getDownloadPath(): String = System.getProperty("user.home") + File.separator + ".com.sonique.com.sonique.app" + File.separator + "downloads"

