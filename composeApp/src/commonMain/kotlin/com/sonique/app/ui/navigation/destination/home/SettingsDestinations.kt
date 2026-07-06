package com.sonique.app.ui.navigation.destination.home

import kotlinx.serialization.Serializable

@Serializable
data class SettingsGeneralDestination(
    val showYouTubeAccount: Boolean = false
)

@Serializable
data object SettingsAudioDestination



@Serializable
data object SettingsPlaybackDestination

@Serializable
data object SettingsSpotifyDestination

@Serializable
data object SettingsSponsorBlockDestination

@Serializable
data object SettingsStorageDestination

@Serializable
data object SettingsBackupDestination

@Serializable
data object SettingsAboutDestination

@Serializable
data object SettingsUpdateDestination

@Serializable
data object SettingsUiDestination
