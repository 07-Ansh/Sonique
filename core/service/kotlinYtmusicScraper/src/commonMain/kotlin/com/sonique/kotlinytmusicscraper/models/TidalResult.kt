package com.sonique.kotlinytmusicscraper.models

/**
 * Result of a Tidal metadata search (bpm, key, keyScale) from the official Tidal API.
 */
data class TidalMetadataResult(
    val bpm: Int?,
    val musicKey: String?,
    val keyScale: String?,
)
