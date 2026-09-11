package com.sonique.app.expect

import androidx.compose.runtime.Composable

interface VolumeController {
    val currentVolume: Float
    fun setVolume(volume: Float)
}

@Composable
expect fun rememberVolumeController(): VolumeController
