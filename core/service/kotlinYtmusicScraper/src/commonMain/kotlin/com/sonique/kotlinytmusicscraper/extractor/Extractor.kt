package com.sonique.kotlinytmusicscraper.extractor

import com.sonique.kotlinytmusicscraper.models.SongItem
import com.sonique.kotlinytmusicscraper.models.response.DownloadProgress

expect class Extractor() {
    fun init()

    fun update()

    fun mergeAudioVideoDownload(filePath: String): DownloadProgress

    fun saveAudioWithThumbnail(
        filePath: String,
        track: SongItem,
    ): DownloadProgress

    fun ytdlpGetStreamUrl(
        videoId: String,
        poToken: String?,
        clientName: String?,
        cookiePath: String?,
    ): String?  

    fun smartTubePlayer(videoId: String): List<Pair<Int, String>>

    fun newPipePlayer(videoId: String): List<Pair<Int, String>>
}

