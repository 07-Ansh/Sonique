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
                        is ToastType.PlayerError ->
                            getString(Res.string.time_out_check_internet_connection_or_change_piped_instance_in_settings, type.error)
                    }
                )
            }
        }
        viewModel.isServiceRunning = true
        shouldUnbind = true
        Logger.d("Service", "Service started")
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

