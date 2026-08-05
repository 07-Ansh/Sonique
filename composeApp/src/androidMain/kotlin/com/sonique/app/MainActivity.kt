package com.sonique.app

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.eygraber.uri.toKmpUriOrNull
import com.sonique.common.FIRST_TIME_MIGRATION
import com.sonique.common.SELECTED_LANGUAGE
import com.sonique.common.STATUS_DONE
import com.sonique.common.SUPPORTED_LANGUAGE
import com.sonique.common.SUPPORTED_LOCATION
import com.sonique.domain.data.model.intent.GenericIntent
import com.sonique.domain.manager.DataStoreManager
import kotlinx.coroutines.flow.collectLatest
import com.sonique.domain.mediaservice.handler.MediaPlayerHandler
import com.sonique.domain.mediaservice.handler.ToastType
import com.sonique.logger.Logger
import com.sonique.media3.di.setServiceActivitySession
import com.sonique.app.di.viewModelModule
import com.sonique.app.utils.VersionManager
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UIEvent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.android.ext.android.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.explicit_content_blocked
import sonique.composeapp.generated.resources.time_out_check_internet_connection_or_change_piped_instance_in_settings
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    val viewModel: SharedViewModel by inject()
    val mediaPlayerHandler by inject<MediaPlayerHandler>()
    val dataStoreManager: DataStoreManager by inject()

    private var mBound = false
    private var shouldUnbind = false
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
 
                setServiceActivitySession(this@MainActivity, MainActivity::class.java, service)
                Logger.w("MainActivity", "onServiceConnected: ")
                mBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Logger.w("MainActivity", "onServiceDisconnected: ")
                mBound = false
            }
        }

    override fun onStart() {
        super.onStart()
        startMusicService()
    }

    override fun onStop() {
        super.onStop()
        if (shouldUnbind) {
            unbindService(serviceConnection)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d("MainActivity", "onNewIntent: $intent")
        viewModel.setIntent(
            GenericIntent(
                action = intent.action,
                data = (intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri())?.toKmpUriOrNull(),
                type = intent.type,
            )
        )
    }

    @ExperimentalMaterial3Api
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen with animation
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        loadKoinModules(
            module {
                single { this@MainActivity }
            }
        )
         
        VersionManager.initialize()

        if (viewModel.recreateActivity.value || viewModel.isServiceRunning) {
            viewModel.activityRecreateDone()
        } else {
            startMusicService()
        }
        Logger.d("MainActivity", "onCreate: ")
        val data = (intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri())?.toKmpUriOrNull()
        if (data != null) {
            viewModel.setIntent(           
                GenericIntent(
                    action = intent.action,
                    data = data,
                    type = intent.type,
                )
            )
        }
        Logger.d("Italy", "Key: ${Locale.ITALY.toLanguageTag()}")

         
         
        lifecycleScope.launch {
            if (getString(FIRST_TIME_MIGRATION) != STATUS_DONE) {
                val localeTag = Locale.getDefault().toLanguageTag()
                Logger.d("Locale Key", "onCreate: $localeTag")
                val matchedCode = SUPPORTED_LANGUAGE.getBestMatchingCode(localeTag)
                if (matchedCode != null) {
                    Logger.d("Contains", "onCreate matched: $matchedCode")
                    putString(SELECTED_LANGUAGE, matchedCode)
                    if (SUPPORTED_LOCATION.items.contains(Locale.getDefault().country)) {
                        putString("location", Locale.getDefault().country)
                    } else {
                        putString("location", "US")
                    }
                } else {
                    putString(SELECTED_LANGUAGE, "en-US")
                }
                 
                getString(SELECTED_LANGUAGE)?.let {
                    Logger.d("Locale Key", "getString: $it")
                     
                    val localeList = LocaleListCompat.forLanguageTags(it)
                    AppCompatDelegate.setApplicationLocales(localeList)
                     
                    putString(FIRST_TIME_MIGRATION, STATUS_DONE)
                }
            }
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() !=
                getString(
                    SELECTED_LANGUAGE,
                )
            ) {
                Logger.d(
                    "Locale Key",
                    "onCreate: ${AppCompatDelegate.getApplicationLocales().toLanguageTags()}",
                )
                putString(SELECTED_LANGUAGE, AppCompatDelegate.getApplicationLocales().toLanguageTags())
            }
        }

        lifecycleScope.launch {
            dataStoreManager.getString(SELECTED_LANGUAGE).collectLatest { language ->
                val localeCode = language ?: "en-US"
                val localeList = LocaleListCompat.forLanguageTags(localeCode)
                if (AppCompatDelegate.getApplicationLocales() != localeList) {
                    AppCompatDelegate.setApplicationLocales(localeList)
                }
            }
        }

        enableEdgeToEdge(
            navigationBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.Transparent.toArgb(),
                ),
            statusBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.Transparent.toArgb(),
                ),
        )
        viewModel.checkIsRestoring()

        viewModel.getLocation()

        lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is UIEvent.DownloadUpdate -> {
                        downloadAppUpdate(event.url, event.title)
                    }
                    is UIEvent.CancelDownload -> {
                        currentDownloadId?.let { id ->
                            val downloadManager = getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                            downloadManager.remove(id)
                            currentDownloadId = null
                        }
                    }
                    is UIEvent.InstallUpdate -> {
                        installPackage(event.path)
                    }
                    else -> {}
                }
            }
        }

        setContent {
            App(viewModel)
        }
    }

    override fun onDestroy() {
        val shouldStopMusicService = viewModel.shouldStopMusicService()
        Logger.w("MainActivity", "onDestroy: Should stop service $shouldStopMusicService")

         
        if (shouldStopMusicService && shouldUnbind && isFinishing) {
            viewModel.isServiceRunning = false
        }
        super.onDestroy()
        Logger.d("MainActivity", "onDestroy: ")
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.activityRecreate()
    }

    private fun startMusicService() {
 
        com.sonique.media3.di
            .startService(this@MainActivity, serviceConnection)
        mediaPlayerHandler.pushPlayerError = { it ->
             
        }
        mediaPlayerHandler.showToast = { type ->
            lifecycleScope.launch {
                viewModel.makeToast(
                    when (type) {
                        ToastType.ExplicitContent -> getString(Res.string.explicit_content_blocked)
                        is ToastType.PlayerError -> {
                            if (type.error == "ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED") {
                                "Playback failed. Try re-downloading the track."
                            } else {
                                getString(
                                    Res.string.time_out_check_internet_connection_or_change_piped_instance_in_settings,
                                    type.error
                                )
                            }
                        }
                    }
                )
            }
        }
        viewModel.isServiceRunning = true
        shouldUnbind = true
        Logger.d("Service", "Service started")
    }

    private var currentDownloadId: Long? = null

    private fun downloadAppUpdate(url: String, title: String) {
        try {
            viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Downloading(0.01f))

            // 1. Clean up any previous update APK files to prevent collision or permission errors
            try {
                val publicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val oldPublicFile = File(publicDir, "SoniqueUpdate.apk")
                if (oldPublicFile.exists()) oldPublicFile.delete()
            } catch (e: Exception) {
                Logger.e("Update", "Could not delete old public APK: ${e.message}")
            }

            try {
                val privateDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                val oldPrivateFile = File(privateDir, "SoniqueUpdate.apk")
                if (oldPrivateFile?.exists() == true) oldPrivateFile.delete()
            } catch (e: Exception) {
                Logger.e("Update", "Could not delete old private APK: ${e.message}")
            }

            val downloadManager = getSystemService(android.content.Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
            if (downloadManager == null) {
                Logger.e("Update", "DownloadManager service null, starting fallback direct download")
                startFallbackDirectDownload(url, title)
                return
            }

            val uri = android.net.Uri.parse(url)
            val request = android.app.DownloadManager.Request(uri)
                .setTitle(title)
                .setDescription("Downloading app update...")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, android.os.Environment.DIRECTORY_DOWNLOADS, "SoniqueUpdate.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = try {
                downloadManager.enqueue(request)
            } catch (e: Exception) {
                Logger.e("Update", "DownloadManager enqueue failed: ${e.message}, falling back to direct download")
                startFallbackDirectDownload(url, title)
                return
            }

            currentDownloadId = downloadId
            Logger.d("Update", "Download started with ID: $downloadId")
            
            // Start progress polling
            lifecycleScope.launch {
                var downloading = true
                while (downloading && currentDownloadId == downloadId) {
                    try {
                        val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)
                        if (cursor != null && cursor.moveToFirst()) {
                            val bytesDownloadedIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val bytesTotalIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            
                            if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                                val downloaded = cursor.getInt(bytesDownloadedIndex)
                                val total = cursor.getInt(bytesTotalIndex)
                                if (total > 0) {
                                    if (downloaded >= total) {
                                        viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Verifying)
                                    } else {
                                        val progress = (downloaded.toFloat() / total.toFloat()).coerceIn(0.01f, 0.99f)
                                        viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Downloading(progress))
                                    }
                                }
                            }
                            
                            val statusIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                            val status = cursor.getInt(statusIndex)
                            if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                                downloading = false
                                val targetFile = File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "SoniqueUpdate.apk")
                                val finalPath = if (targetFile.exists()) targetFile.absolutePath else (downloadManager.getUriForDownloadedFile(downloadId)?.toString() ?: "")
                                viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Downloaded(finalPath))
                            } else if (status == android.app.DownloadManager.STATUS_FAILED) {
                                downloading = false
                                Logger.e("Update", "DownloadManager failed status, falling back to direct download")
                                startFallbackDirectDownload(url, title)
                                return@launch
                            }
                            cursor.close()
                        } else {
                            cursor?.close()
                            downloading = false
                        }
                    } catch (e: Exception) {
                        Logger.e("Update", "Polling error: ${e.message}")
                    }
                    kotlinx.coroutines.delay(500)
                }
            }

            val onComplete = object : android.content.BroadcastReceiver() {
                override fun onReceive(ctxt: android.content.Context?, intent: Intent?) {
                    if (intent?.action == android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                        val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadId) {
                            Logger.d("Update", "Download complete: $id")
                            val targetFile = File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "SoniqueUpdate.apk")
                            val finalPath = if (targetFile.exists()) targetFile.absolutePath else ""
                            if (finalPath.isNotEmpty()) {
                                viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Downloaded(finalPath))
                                viewModel.insertNotification(
                                    "System Update",
                                    "New version downloaded. Ready to install.",
                                    "SYSTEM_UPDATE"
                                )
                            }
                            try { unregisterReceiver(this) } catch (e: Exception) {}
                        }
                    }
                }
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(onComplete, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    android.content.Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(onComplete, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
        } catch (e: Exception) {
            Logger.e("Update", "Download process error: ${e.message}, falling back to direct download")
            startFallbackDirectDownload(url, title)
        }
    }

    private fun startFallbackDirectDownload(url: String, title: String) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Downloading(0.05f))
                val targetFile = File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "SoniqueUpdate.apk")
                if (targetFile.exists()) targetFile.delete()

                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                var responseCode = connection.responseCode
                if (responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM || responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP) {
                    val redirectUrl = connection.getHeaderField("Location")
                    if (!redirectUrl.isNullOrEmpty()) {
                        val redirectConn = java.net.URL(redirectUrl).openConnection() as java.net.HttpURLConnection
                        redirectConn.connectTimeout = 15000
                        redirectConn.readTimeout = 15000
                        redirectConn.connect()
                        streamToFile(redirectConn, targetFile)
                        return@launch
                    }
                }
                streamToFile(connection, targetFile)
            } catch (e: Exception) {
                Logger.e("Update", "Direct download failed: ${e.message}")
                e.printStackTrace()
                viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Idle)
            }
        }
    }

    private suspend fun streamToFile(connection: java.net.HttpURLConnection, targetFile: File) {
        val totalLength = connection.contentLength
        val inputStream = connection.inputStream
        val outputStream = FileOutputStream(targetFile)
        val buffer = ByteArray(8192)
        var bytesRead: Int
        var totalRead = 0L

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalRead += bytesRead
            if (totalLength > 0) {
                val progress = (totalRead.toFloat() / totalLength.toFloat()).coerceIn(0.05f, 0.99f)
                viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Downloading(progress))
            }
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()

        if (targetFile.exists() && targetFile.length() > 0) {
            viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Downloaded(targetFile.absolutePath))
            viewModel.insertNotification(
                "System Update",
                "New version downloaded. Ready to install.",
                "SYSTEM_UPDATE"
            )
        } else {
            viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Idle)
        }
    }

    private fun isValidApk(file: File): Boolean {
        if (!file.exists() || file.length() < 100) return false
        return try {
            java.io.FileInputStream(file).use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun installPackage(uriString: String) {
        try {
            // Check Android 8.0+ unknown apps installation permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (!packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = android.net.Uri.parse("package:$packageName")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    android.widget.Toast.makeText(this, "Please allow 'Install from this source' and click Install again.", android.widget.Toast.LENGTH_LONG).show()
                    return
                }
            }

            // Determine target file cleanly from private downloads or passed path
            val privateFile = File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "SoniqueUpdate.apk")
            val file = when {
                uriString.isNotEmpty() && File(uriString).exists() && File(uriString).length() > 0 -> File(uriString)
                privateFile.exists() && privateFile.length() > 0 -> privateFile
                else -> {
                    val uri = android.net.Uri.parse(uriString)
                    val path = if (uri.scheme == "file") uri.path else uriString
                    if (path != null) File(path) else privateFile
                }
            }

            if (!isValidApk(file)) {
                Logger.e("Update", "File is not a valid APK package: ${file.absolutePath} (size: ${file.length()})")
                android.widget.Toast.makeText(this, "Downloaded file is invalid. Opening GitHub Release...", android.widget.Toast.LENGTH_LONG).show()
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/07-Ansh/Sonique/releases/latest"))
                    startActivity(browserIntent)
                } catch (e: Exception) {}
                return
            }

            val contentUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.update_provider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resInfoList = packageManager.queryIntentActivities(installIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val pkgName = resolveInfo.activityInfo.packageName
                grantUriPermission(pkgName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(installIntent)
        } catch (e: Exception) {
            Logger.e("Update", "Install failed: ${e.message}")
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Failed to start installer: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun putString(
        key: String,
        value: String,
    ) {
        viewModel.putString(key, value)
    }

    private suspend fun getString(key: String): String? = viewModel.getString(key)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.activityRecreate()
    }
}

