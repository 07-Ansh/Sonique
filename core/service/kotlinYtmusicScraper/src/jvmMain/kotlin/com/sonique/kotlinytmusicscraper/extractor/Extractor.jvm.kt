package com.sonique.kotlinytmusicscraper.extractor

import com.sonique.kotlinytmusicscraper.models.SongItem
import com.sonique.kotlinytmusicscraper.models.response.DownloadProgress
import com.sonique.logger.Logger
import dev.maxrave.pipepipe.extractor.NewPipe
import dev.maxrave.pipepipe.extractor.ServiceList
import dev.maxrave.pipepipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.NewPipe as BraveNewPipe
import org.schabi.newpipe.extractor.ServiceList as BraveServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo as BraveStreamInfo
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

actual class Extractor {
    private var newPipeDownloader = NewPipeDownloaderImpl(proxy = null)
    private var braveNewPipeDownloader = BraveNewPipeDownloaderImpl(proxy = null)

    actual fun init() {
        NewPipe.init(newPipeDownloader)
        BraveNewPipe.init(braveNewPipeDownloader)
    }

    actual fun logIn(cookie: String?) {
        ServiceList.YouTube.tokens = cookie ?: ""
    }

    actual fun update() {
        try {
            val processBuilder = ProcessBuilder("yt-dlp", "--update-to", "master")
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val input = BufferedReader(InputStreamReader(process.inputStream))
            var line: String? = null
            while ((input.readLine().also { line = it }) != null) {
                Logger.w("Extractor", line ?: "No line")
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun ytdlpGetStreamUrl(
        videoId: String,
        poToken: String?,
        clientName: String?,
        cookiePath: String?,
    ): String? {
        val processBuilder = ProcessBuilder("yt-dlp")
        if (cookiePath != null) {
            processBuilder.command().add("--cookies")
            processBuilder.command().add(cookiePath)
        }
        if (clientName != null) {
            processBuilder.command().add("--extractor-args")
            processBuilder.command().add(
                "youtube:player_client=$clientName;youtube:webpage_skip;" +
                    if (clientName.contains("web") && poToken != null) "youtube:po_token=$clientName.gvs+$poToken;" else "",
            )
        }
        processBuilder.command().add("--no-warnings")
        processBuilder.command().add("--dump-json")
        processBuilder.command().add("https://www.youtube.com/watch?v=$videoId")
        processBuilder.redirectErrorStream(true)
        val commandString = processBuilder.command().joinToString(" ")
        Logger.w("Extractor", "Command: $commandString")
        val process = processBuilder.start()
        val input = BufferedReader(InputStreamReader(process.inputStream))
        var line: String? = null
        try {
            var response: String = ""
            while ((input.readLine().also { line = it }) != null) {
                response += "\n" + line
            }
            val exitCode = process.waitFor()
            Logger.w("Extractor", "yt-dlp exit code: $exitCode")
            Logger.w("Extractor", "yt-dlp response: $response")
            return response.split("\n").lastOrNull()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    actual fun smartTubePlayer(videoId: String): List<Pair<Int, String>> = emptyList()

    actual fun newPipePlayer(videoId: String): List<Pair<Int, String>> {
        try {
            val streamInfo =
                StreamInfo.getInfo(ServiceList.YouTube, "https://music.youtube.com/watch?v=$videoId")
            val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
            val temp =
                streamsList
                    .mapNotNull {
                        (it.itagItem?.id ?: return@mapNotNull null) to it.content
                    }.toMutableList()
            val manifest = streamInfo.dashMpdUrl.takeIf { !it.isNullOrEmpty() } ?: streamInfo.hlsUrl
            if (!manifest.isNullOrEmpty()) temp.add(96 to manifest)
            val pipeResult = temp.toList()
            if (!pipeResult.hasRequiredItags()) {
                Logger.d(
                    "Extractor",
                    "PipePipe missing required itags for $videoId (got=${pipeResult.map { it.first }}), falling back to BravePipe",
                )
            } else if (!pipeResult.headCheckRandomStream()) {
                Logger.d(
                    "Extractor",
                    "PipePipe stream URL HEAD check failed (non 2xx) for $videoId, falling back to BravePipe",
                )
            } else {
                return pipeResult
            }
        } catch (e: Throwable) {
            Logger.w("Extractor", "PipePipe extractor failed for $videoId: ${e.message}, falling back to BravePipe")
        }

        return runCatching {
            val streamInfo =
                BraveStreamInfo.getInfo(BraveServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
            val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
            val temp =
                streamsList
                    .mapNotNull {
                        (it.itagItem?.id ?: return@mapNotNull null) to it.content
                    }.toMutableList()
            val manifest = streamInfo.dashMpdUrl.takeIf { !it.isNullOrEmpty() } ?: streamInfo.hlsUrl
            if (!manifest.isNullOrEmpty()) temp.add(96 to manifest)
            temp.toList()
        }.onFailure {
            Logger.w("Extractor", "BravePipe extractor failed for $videoId: ${it.message}")
        }.getOrElse { emptyList() }
    }

    actual fun mergeAudioVideoDownload(filePath: String): DownloadProgress = DownloadProgress.failed("Not supported on JVM")

    actual fun saveAudioWithThumbnail(
        filePath: String,
        track: SongItem,
    ): DownloadProgress = DownloadProgress.AUDIO_DONE
}

