package com.sonique.domain.repository

import com.sonique.domain.data.entities.ArtistEntity
import com.sonique.domain.data.model.browse.artist.ArtistBrowse
import com.sonique.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface ArtistRepository {
    fun getAllArtists(limit: Int): Flow<List<ArtistEntity>>

    fun getArtistById(id: String): Flow<ArtistEntity>

    suspend fun insertArtist(artistEntity: ArtistEntity)

    suspend fun updateArtistImage(
        channelId: String,
        thumbnail: String,
    )

    suspend fun updateFollowedStatus(
        channelId: String,
        followedStatus: Int,
    )

    fun getFollowedArtists(): Flow<List<ArtistEntity>>

    suspend fun updateArtistInLibrary(
        inLibrary: LocalDateTime,
        channelId: String,
    )

    fun getArtistData(channelId: String): Flow<Resource<ArtistBrowse>>
}

