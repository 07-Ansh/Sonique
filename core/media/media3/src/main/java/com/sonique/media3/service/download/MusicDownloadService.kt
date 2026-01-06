package com.sonique.media3.service.download

import android.app.Notification
import android.content.Context
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.sonique.media3.R
import com.sonique.domain.data.entities.DownloadState

import com.sonique.domain.mediaservice.handler.DownloadHandler
import org.koin.android.ext.android.inject

@UnstableApi
internal class MusicDownloadService :
    DownloadService(
        NOTIFICATION_ID,
        1000L,
        CHANNEL_ID,
        R.string.download,
        0,
    ) {
    private val downloadUtil: DownloadHandler by inject<DownloadHandler>()

    override fun getDownloadManager() = (downloadUtil as DownloadUtils).downloadManager

    override fun getScheduler(): Scheduler = PlatformScheduler(this, JOB_ID)

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification {
        val message = if (downloads.size == 1) {
            Util.fromUtf8Bytes(downloads[0].request.data)
        } else {
             val remainingCount = downloads.size
             "$remainingCount left"
        }
        
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            data = android.net.Uri.parse("com.sonique.com.sonique.app://downloads")
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = if (intent != null) {
            android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else null

        return (downloadUtil as DownloadUtils).downloadNotificationHelper.buildProgressNotification(
            this,
            R.drawable.ic_stat_noti,
            pendingIntent,
            message,
            downloads,
            notMetRequirements,
        )
    }

    class TerminalStateNotificationHelper(
        private val context: Context,
        private val notificationHelper: DownloadNotificationHelper,
        private var nextNotificationId: Int,
    ) : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) {
            if (download.state == Download.STATE_FAILED) {
                val notification =
                    notificationHelper.buildDownloadFailedNotification(
                        context,
                        R.drawable.baseline_error_outline_24,
                        null,
                        Util.fromUtf8Bytes(download.request.data),
                    )
                NotificationUtil.setNotification(context, nextNotificationId++, notification)
            } else if (download.state == Download.STATE_COMPLETED) {
                val notification =
                    notificationHelper.buildDownloadCompletedNotification(
                        context,
                        R.drawable.baseline_downloaded,
                        null,
                        Util.fromUtf8Bytes(download.request.data),
                    )
                NotificationUtil.setNotification(context, nextNotificationId++, notification)
            }
        }

        override fun onDownloadsPausedChanged(
            downloadManager: DownloadManager,
            downloadsPaused: Boolean,
        ) {
            if (downloadsPaused) {
                downloadManager.resumeDownloads()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "download"
        const val NOTIFICATION_ID = 1000
        const val JOB_ID = 1000
    }
}

