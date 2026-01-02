package com.sonique.app.ui.navigation.destination.library

import kotlinx.serialization.Serializable

@Serializable
data class LibraryDestination(
    val openDownloads: Boolean = false,
)

