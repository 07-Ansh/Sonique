package com.sonique.app.expect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

@Composable
actual fun rememberVolumeController(): VolumeController {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val maxVol = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat().coerceAtLeast(1f)
    }

    var volState by remember(audioManager) {
        mutableFloatStateOf(
            (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVol).coerceIn(0f, 1f)
        )
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                volState = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVol).coerceIn(0f, 1f)
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        try {
            context.registerReceiver(receiver, filter)
        } catch (_: Exception) {}
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    return remember(audioManager, volState) {
        object : VolumeController {
            override val currentVolume: Float
                get() = volState

            override fun setVolume(volume: Float) {
                val clamped = volume.coerceIn(0f, 1f)
                val target = (clamped * maxVol).roundToInt().coerceIn(0, maxVol.toInt())
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                volState = clamped
            }
        }
    }
}
