package com.sonique.data.repository

import com.sonique.data.db.LocalDataSource
import com.sonique.data.parser.parseArtistData
import com.sonique.domain.data.entities.ArtistEntity
import com.sonique.domain.data.model.browse.artist.ArtistBrowse
import com.sonique.domain.repository.ArtistRepository
import com.sonique.domain.utils.Resource
import com.sonique.kotlinytmusicscraper.YouTube
import com.sonique.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime

internal class ArtistRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : ArtistRepository {
    override fun getAllArtists(limit: Int): Flow<List<ArtistEntity>> =
        flow {
            emit(localDataSource.getAllArtists(limit))
        }.flowOn(Dispatchers.IO)

    override fun getArtistById(id: String): Flow<ArtistEntity?> =
        flow {
            emit(localDataSource.getArtist(id))
        }.flowOn(Dispatchers.IO)

    override suspend fun insertArtist(artistEntity: ArtistEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertArtist(artistEntity)
        }

    override suspend fun updateArtistImage(
        channelId: String,
        thumbnail: String,
    ) = withContext(
        Dispatchers.Main,
    ) {
        localDataSource.updateArtistImage(
            channelId,
            thumbnail,
        )
    }

    override suspend fun updateFollowedStatus(
        channelId: String,
        followedStatus: Int,
    ) = withContext(
        Dispatchers.Main,
    ) { localDataSource.updateFollowed(followedStatus, channelId) }

    override fun getFollowedArtists(): Flow<List<ArtistEntity>> =
        localDataSource.getFollowedArtistsAsFlow().flowOn(Dispatchers.IO)

    override suspend fun updateArtistInLibrary(
        inLibrary: LocalDateTime,
        channelId: String,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updateArtistInLibrary(
            inLibrary,
            channelId,
        )
    }

    override fun getArtistData(channelId: String): Flow<Resource<ArtistBrowse>> =
        flow {
            runCatching {
                youTube
                    .artist(channelId)
                    .onSuccess { result ->
                        emit(Resource.Success<ArtistBrowse>(parseArtistData(result)))
                    }.onFailure { e ->
                        Logger.d("Artist", "Error: ${e.message}")
                        emit(Resource.Error<ArtistBrowse>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun syncFollowedArtistsFromYouTube() {
        withContext(Dispatchers.IO) {
            coroutineScope {
                // Sync from Liked Artists
                launch {
                    youTube.getLibraryArtists().onSuccess { data ->
                        val count = parseAndSyncResponse(data)
                        Logger.i("SyncArtists", "Found $count artists from Liked Artists")
                    }.onFailure { e ->
                        Logger.e("SyncArtists", "Error fetching Liked Artists: ${e.message}")
                    }
                }
                
                // Sync from Subscriptions (this is often where "followed" artists actually live)
                launch {
                    youTube.getLibrarySubscriptions().onSuccess { data ->
                        val count = parseAndSyncResponse(data)
                        Logger.i("SyncArtists", "Found $count artists from Subscriptions")
                    }.onFailure { e ->
                        Logger.e("SyncArtists", "Error fetching Subscriptions: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun parseAndSyncResponse(data: com.sonique.kotlinytmusicscraper.models.response.BrowseResponse): Int {
        var count = 0
        val sections = data.contents
            ?.singleColumnBrowseResultsRenderer
            ?.tabs
            ?.flatMap { it.tabRenderer.content?.sectionListRenderer?.contents ?: emptyList() }
            ?: data.contents?.sectionListRenderer?.contents
            ?: emptyList()
        val unwrappedSections = sections.flatMap { 
            val itemSectionRenderer = it.itemSectionRenderer
            if (itemSectionRenderer != null) {
                itemSectionRenderer.contents ?: emptyList()
            } else {
                listOf(it)
            }
        }

        for (section in unwrappedSections) {
            // Check direct Grid Renderer or nested in itemSectionRenderer
            // We can't access itemSectionRenderer directly easily since it's not in the model, but we check gridRenderer
            section.gridRenderer?.items?.forEach { item ->
                item.musicTwoRowItemRenderer?.let { renderer ->
                    val channelId = renderer.navigationEndpoint?.browseEndpoint?.browseId
                        ?: renderer.title?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.browseId
                    val name = renderer.title?.runs?.joinToString("") { it.text }
                    if (channelId != null && name != null) {
                        val thumb = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url
                        syncArtist(channelId, name, thumb)
                        count++
                    }
                }
            }

            // Music Shelf Renderer (List view)
            section.musicShelfRenderer?.contents?.forEach { item ->
                item.musicResponsiveListItemRenderer?.let { renderer ->
                    val channelId = renderer.navigationEndpoint?.browseEndpoint?.browseId
                        ?: renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.browseId
                    val name = renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.joinToString("") { it.text }
                    if (channelId != null && name != null) {
                        val thumb = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url
                        syncArtist(channelId, name, thumb)
                        count++
                    }
                }
            }
        }
        return count
    }

    private suspend fun syncArtist(channelId: String, name: String, thumbnail: String?) {
        val existing = localDataSource.getArtist(channelId)
        if (existing == null) {
            localDataSource.insertArtist(
                ArtistEntity(
                    channelId = channelId,
                    name = name,
                    thumbnails = thumbnail,
                    followed = true,
                )
            )
        } else if (!existing.followed) {
            localDataSource.updateFollowed(1, channelId)
        }
        if (thumbnail != null) {
            localDataSource.updateArtistImage(channelId, thumbnail)
        }
    }
}
