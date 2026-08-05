package com.sonique.kotlinytmusicscraper.extractor

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.sonique.kotlinytmusicscraper.models.SongItem
import com.sonique.kotlinytmusicscraper.models.response.DownloadProgress
import com.sonique.logger.Logger
import dev.maxrave.pipepipe.extractor.NewPipe
import dev.maxrave.pipepipe.extractor.ServiceList
import dev.maxrave.pipepipe.extractor.stream.StreamInfo
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import org.schabi.newpipe.extractor.NewPipe as BraveNewPipe
import org.schabi.newpipe.extractor.ServiceList as BraveServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo as BraveStreamInfo

private const val TAG = "Extractor"

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

    actual fun newPipePlayer(videoId: String): List<Pair<Int, String>> {
        try {
            val streamInfo =
                StreamInfo.getInfo(ServiceList.YouTube, "https://music.youtube.com/watch?v=$videoId")
            val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
            val pipeResult =
                streamsList.mapNotNull {
                    (it.itagItem?.id ?: return@mapNotNull null) to it.content
                }
            if (!pipeResult.hasRequiredItags()) {
                Logger.d(
                    TAG,
                    "PipePipe missing required itags for $videoId (got=${pipeResult.map { it.first }}), falling back to BravePipe",
                )
            } else if (!pipeResult.headCheckRandomStream()) {
                Logger.d(
                    TAG,
                    "PipePipe stream URL HEAD check failed (non 2xx) for $videoId, falling back to BravePipe",
                )
            } else {
                return pipeResult
            }
        } catch (e: Throwable) {
            Logger.w(TAG, "PipePipe extractor failed for $videoId: ${e.message}, falling back to BravePipe")
        }

        return runCatching {
            val streamInfo =
                BraveStreamInfo.getInfo(BraveServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
            val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
            streamsList.mapNotNull {
                (it.itagItem?.id ?: return@mapNotNull null) to it.content
            }
        }.onFailure {
            Logger.w(TAG, "BravePipe extractor failed for $videoId: ${it.message}")
        }.getOrElse { emptyList() }
    }

    actual fun mergeAudioVideoDownload(filePath: String): DownloadProgress {
        val command =
            listOf(
                "-i",
                ("$filePath.mp4"),
                "-i",
                ("$filePath.webm"),
                "-c:v",
                "copy",
                "-c:a",
                "aac",
                "-map",
                "0:v:0",
                "-map",
                "1:a:0",
                "-shortest",
                "$filePath-Sonique.mp4",
            ).joinToString(" ")

        if (FileSystem.SYSTEM.exists("$filePath-Sonique.mp4".toPath())) {
            FileSystem.SYSTEM.delete("$filePath-Sonique.mp4".toPath())
        }

        val session =
            FFmpegKit.execute(
                command,
            )
        if (ReturnCode.isSuccess(session.returnCode)) {
            // SUCCESS
            Logger.d(TAG, "Command succeeded ${session.state}, ${session.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                FileSystem.SYSTEM.delete("$filePath.mp4".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.VIDEO_DONE)
        } else if (ReturnCode.isCancel(session.returnCode)) {
            // CANCEL
            Logger.d(TAG, "Command cancelled ${session.state}, ${session.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                FileSystem.SYSTEM.delete("$filePath.mp4".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed(session.failStackTrace ?: "Command cancelled"))
        } else {
            // FAILURE
            Logger.d(TAG, "Command failed ${session.state}, ${session.returnCode}, ${session.failStackTrace}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                FileSystem.SYSTEM.delete("$filePath.mp4".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed(session.failStackTrace ?: "FFmpeg command failed"))
        }
    }

    actual fun saveAudioWithThumbnail(
        filePath: String,
        track: SongItem,
    ): DownloadProgress {
        val command =
            listOf(
                "-i",
                ("$filePath.webm"),
                "-q:a 0",
                "$filePath.mp3",
            ).joinToString(" ")

        try {
            if (FileSystem.SYSTEM.exists("$filePath.mp3".toPath())) {
                FileSystem.SYSTEM.delete("$filePath.mp3".toPath())
            }
            if (FileSystem.SYSTEM.exists("$filePath-sonique.mp3".toPath())) {
                FileSystem.SYSTEM.delete("$filePath-sonique.mp3".toPath())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val session =
            FFmpegKit.execute(
                command,
            )
        if (ReturnCode.isSuccess(session.returnCode)) {
            // SUCCESS
            Logger.d(TAG, "Command succeeded ${session.state}, ${session.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else if (ReturnCode.isCancel(session.returnCode)) {
            // CANCEL
            Logger.d(TAG, "Command cancelled ${session.state}, ${session.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed("Error"))
        } else {
            // FAILURE
            Logger.d(TAG, "Command failed ${session.state}, ${session.returnCode}, ${session.failStackTrace}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed("Error"))
        }

        val commandInject =
            listOf(
                "-i",
                "$filePath.mp3",
                "-i $filePath.jpg",
                "-map 0:a",
                "-map 1:v",
                "-c copy",
                "-id3v2_version 3",
                "-metadata",
                "title=\"${track.title}\"",
                "-metadata",
                "artist=\"${track.artists.joinToString(", ") { it.name }}\"",
                "-metadata",
                "album=\"${track.album?.name ?: track.title}\"",
                "-disposition:v:0 attached_pic",
                "$filePath-sonique.mp3",
            ).joinToString(" ")
        val sessionInject =
            FFmpegKit.execute(
                commandInject,
            )
        if (ReturnCode.isSuccess(sessionInject.returnCode)) {
            // SUCCESS
            Logger.d(TAG, "Command succeeded ${sessionInject.state}, ${sessionInject.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.mp3".toPath())
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.AUDIO_DONE)
        } else if (ReturnCode.isCancel(sessionInject.returnCode)) {
            // CANCEL
            Logger.d(TAG, "Command cancelled ${sessionInject.state}, ${sessionInject.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                FileSystem.SYSTEM.delete("$filePath-sonique.mp3".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed("Error"))
        } else {
            // FAILURE
            Logger.d(TAG, "Command failed ${sessionInject.state}, ${sessionInject.returnCode}, ${sessionInject.failStackTrace}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                FileSystem.SYSTEM.delete("$filePath-sonique.mp3".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed("Error"))
        }
    }
}
