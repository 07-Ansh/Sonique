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
         
        unloadKoinModules(viewModelModule)
        loadKoinModules(viewModelModule)
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
                Logger.d("Locale Key", "onCreate: ${Locale.getDefault().toLanguageTag()}")
                if (SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())) {
                    Logger.d(
                        "Contains",
                        "onCreate: ${
                            SUPPORTED_LANGUAGE.codes.contains(
                                Locale.getDefault().toLanguageTag(),
                            )
                        }",
                    )
                    putString(SELECTED_LANGUAGE, Locale.getDefault().toLanguageTag())
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
        unloadKoinModules(viewModelModule)
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
        val downloadManager = getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        val uri = android.net.Uri.parse(url)
        val request = android.app.DownloadManager.Request(uri)
            .setTitle(title)
            .setDescription("Downloading app update...")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "SoniqueUpdate.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)
        currentDownloadId = downloadId
        Logger.d("Update", "Download started with ID: $downloadId")
        
        // Start progress polling
        lifecycleScope.launch {
            var downloading = true
            while (downloading && currentDownloadId == downloadId) {
                val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloadedIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    
                     if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                         val downloaded = cursor.getInt(bytesDownloadedIndex)
                         val total = cursor.getInt(bytesTotalIndex)
                         if (total > 0) {
                             if (downloaded == total) {
                                 viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Verifying)
                             } else {
                                 val progress = downloaded.toFloat() / total.toFloat()
                                 viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Downloading(progress))
                             }
                         }
                    }
                    
                    val statusIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(statusIndex)
                    if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false
                         downloading = false
                         val fileUri = downloadManager.getUriForDownloadedFile(downloadId)
                         if (fileUri != null) {
                             viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Downloaded(fileUri.toString()))
                         }
                    } else if (status == android.app.DownloadManager.STATUS_FAILED) {
                        downloading = false
                        viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Idle)
                    }
                } else {
                    // Cursor empty usually means download cancelled/removed
                    downloading = false
                    viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Idle)
                }
                cursor.close()
                kotlinx.coroutines.delay(500)
            }
        }

        val onComplete = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctxt: android.content.Context?, intent: Intent?) {
                if (intent?.action == android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        Logger.d("Update", "Download complete: $id")
                        val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                            if (android.app.DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(statusIndex)) {
                                val fileUri = downloadManager.getUriForDownloadedFile(id)
                                if (fileUri != null) {
                                    Logger.d("Update", "Downloaded URI: $fileUri")
                                    viewModel.updateDownloadStatus(SharedViewModel.DownloadStatus.Downloaded(fileUri.toString()))
                                    viewModel.insertNotification(
                                        "System Update",
                                        "New version downloaded. Ready to install.",
                                        "SYSTEM_UPDATE"
                                    )
                                }
                            }
                        }
                        cursor.close()
                        unregisterReceiver(this)
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
    }

    private fun installPackage(uriString: String) {
        try {
            val uri = android.net.Uri.parse(uriString)
            val installIntent = Intent(Intent.ACTION_VIEW)
            
            if (uri.scheme == "content") {
                installIntent.setDataAndType(uri, "application/vnd.android.package-archive")
            } else {
                // Determine file path
                val path = if (uri.scheme == "file") uri.path else uriString
                if (path == null) {
                    Logger.e("Update", "Invalid path from URI: $uriString")
                    return
                }
                
                val file = File(path)
                if (!file.exists()) {
                    Logger.e("Update", "File not found: $path")
                    return
                }
                
                val contentUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.update_provider",
                    file
                )
                installIntent.setDataAndType(contentUri, "application/vnd.android.package-archive")
            }
            
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(installIntent)
        } catch (e: Exception) {
            Logger.e("Update", "Install failed: ${e.message}")
            e.printStackTrace()
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

