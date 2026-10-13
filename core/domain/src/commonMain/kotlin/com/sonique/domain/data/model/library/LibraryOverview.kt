package com.sonique.domain.data.model.library

/**
 * What one card on the Your library tab shows: how many items the collection holds, and the
 * artwork of the newest few, newest first. Thumbnails can be fewer than asked for, or none.
 */
data class LibraryCollectionPreview(
    val count: Int,
    val thumbnails: List<String>,
)

/** The four collections the Your library tab opens onto. */
data class LibraryOverview(
    val favorite: LibraryCollectionPreview,
    val followed: LibraryCollectionPreview,
    val mostPlayed: LibraryCollectionPreview,
    val downloaded: LibraryCollectionPreview,
)
