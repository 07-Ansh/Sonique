package com.sonique.kotlinytmusicscraper.extractor

import com.sonique.kotlinytmusicscraper.models.SongItem
import com.sonique.kotlinytmusicscraper.models.response.DownloadProgress

actual class Extractor {
    actual fun init() {
    }
    actual fun update() {
    }

    actual fun ytdlpGetStreamUrl(
        videoId: String,
        poToken: String?,
        clientName: String,
        cookiePath: String?
    ): String? = null

    actual fun smartTubePlayer(videoId: String): List<Pair<Int, String>> = emptyList()

    actual fun newPipePlayer(videoId: String): List<Pair<Int, String>> = emptyList()
    actual fun mergeAudioVideoDownload(filePath: String): DownloadProgress {
        return DownloadProgress.failed("Not supported on iOS")
    }

    actual fun saveAudioWithThumbnail(filePath: String, track: SongItem): DownloadProgress {
        return DownloadProgress.failed("Not supported on iOS")
    }
}

