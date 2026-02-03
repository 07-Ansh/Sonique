package com.sonique.domain.mediaservice.handler

import kotlinx.coroutines.flow.StateFlow

interface DownloadHandler {
    suspend fun downloadTrack(
        videoId: String,
        title: String,
        thumbnail: String,
        isVideo: Boolean = false,
    )

    fun removeDownload(videoId: String)

    fun removeAllDownloads()

    val downloads: StateFlow<Map<String, Pair<Download?, Download?>>>

    val downloadTask: StateFlow<Map<String, Int>>

     
    companion object State {
        const val STATE_QUEUED: Int = 0

         
        const val STATE_STOPPED: Int = 1

         
        const val STATE_DOWNLOADING: Int = 2

         
        const val STATE_COMPLETED: Int = 3

         
        const val STATE_FAILED: Int = 4

         
        const val STATE_REMOVING: Int = 5

         
        const val STATE_RESTARTING: Int = 7
    }

    data class Download(
        val state: Int,
    )
}

