package com.sonique.kotlinytmusicscraper.models.body

import com.sonique.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class CreatePlaylistBody(
    val context: Context,
    val title: String,
    val description: String? = "Created by Sonique",
    val privacyStatus: String = PrivacyStatus.PRIVATE,
    val videoIds: List<String>? = null,
) {
    object PrivacyStatus {
        const val PRIVATE = "PRIVATE"
        const val PUBLIC = "PUBLIC"
        const val UNLISTED = "UNLISTED"
    }
}

