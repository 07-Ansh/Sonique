package com.sonique.kotlinytmusicscraper.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote app config fetched on launch.
 */
@Serializable
data class RemoteConfig(
    @SerialName("tidalClientId")
    val tidalClientId: String? = null,
    @SerialName("tidalClientSecret")
    val tidalClientSecret: String? = null,
)
