package com.sonique.app.expect.ui

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sonique.logger.Logger
import kotlinx.coroutines.runBlocking
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.no_equalizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun openEqResult(audioSessionId: Int): OpenEqLauncher {
    val context = LocalContext.current
    val currentSessionId by rememberUpdatedState(audioSessionId)
    val resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    return object : OpenEqLauncher {
        override fun launch() {
            val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, currentSessionId)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            val packageManager = context.packageManager
            val resolveInfo: List<*> = packageManager.queryIntentActivities(eqIntent, 0)
            Logger.d("EQ", resolveInfo.toString())
            if (resolveInfo.isEmpty()) {
                showToast(runBlocking { getString(Res.string.no_equalizer) }, ToastGravity.Bottom)
            } else {
                try {
                    resultLauncher.launch(eqIntent)
                } catch (e: Exception) {
                    Logger.e("EQ", "Failed to launch equalizer: ${e.message}")
                    showToast(runBlocking { getString(Res.string.no_equalizer) }, ToastGravity.Bottom)
                }
            }
        }
    }
}

