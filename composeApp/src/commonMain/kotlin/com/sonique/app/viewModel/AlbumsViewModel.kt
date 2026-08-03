package com.sonique.app.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonique.domain.data.model.home.Content
import com.sonique.domain.data.model.home.HomeItem
import com.sonique.domain.data.model.searchResult.songs.Artist
import com.sonique.domain.data.model.searchResult.songs.Thumbnail
import com.sonique.domain.repository.AlbumRepository
import com.sonique.domain.repository.HomeRepository
import com.sonique.domain.repository.PlaylistRepository
import com.sonique.domain.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.music_video
import sonique.composeapp.generated.resources.new_release
import sonique.composeapp.generated.resources.song
import sonique.composeapp.generated.resources.view_count

class AlbumsViewModel(
    private val homeRepository: HomeRepository,
    private val playlistRepository: PlaylistRepository,
    private val albumRepository: AlbumRepository,
) : ViewModel() {

    private val _albumsForYou = MutableStateFlow<List<Content>>(emptyList())
    val albumsForYou: StateFlow<List<Content>> = _albumsForYou.asStateFlow()

    private val _playlistsForYou = MutableStateFlow<List<Content>>(emptyList())
    val playlistsForYou: StateFlow<List<Content>> = _playlistsForYou.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchAlbumsData()
    }

    fun fetchAlbumsData(forceRefresh: Boolean = false) {
        _isLoading.value = true
        viewModelScope.launch {
            combine(
                homeRepository.getHomeData(
                    null,
                    getString(Res.string.view_count),
                    getString(Res.string.song),
                    forceRefresh = forceRefresh
                ),
                homeRepository.getNewRelease(
                    getString(Res.string.new_release),
                    getString(Res.string.music_video),
                    forceRefresh = forceRefresh
                ),
                playlistRepository.getLibraryAlbums(),
                playlistRepository.getMixedForYou(),
                albumRepository.getAllAlbums(100),
            ) { homeRes, newReleaseRes, libraryAlbums, mixedForYou, localAlbums ->
                val albumsList = mutableListOf<Content>()
                val playlistsList = mutableListOf<Content>()

                // 1. YouTube Music Library Albums
                libraryAlbums?.forEach { item ->
                    albumsList.add(
                        Content(
                            album = null,
                            artists = listOf(Artist(name = item.author, id = null)),
                            description = null,
                            isExplicit = false,
                            playlistId = item.browseId,
                            browseId = item.browseId,
                            thumbnails = item.thumbnails,
                            title = item.title,
                            videoId = null,
                            views = null
                        )
                    )
                }

                // 2. Local Database Saved Albums
                localAlbums.forEach { album ->
                    albumsList.add(
                        Content(
                            album = null,
                            artists = album.artistName?.map { Artist(name = it, id = null) },
                            description = album.description,
                            isExplicit = false,
                            playlistId = album.audioPlaylistId.ifEmpty { null },
                            browseId = album.browseId,
                            thumbnails = listOf(
                                Thumbnail(
                                    height = 300,
                                    width = 300,
                                    url = album.thumbnails ?: ""
                                )
                            ),
                            title = album.title,
                            videoId = null,
                            views = null
                        )
                    )
                }

                // 3. New Release Albums
                if (newReleaseRes is Resource.Success) {
                    val newReleases = newReleaseRes.data ?: emptyList()
                    newReleases.forEach { item ->
                        val albumContents = item.contents.filterNotNull().filter { content ->
                            content.videoId.isNullOrEmpty() && (
                                content.browseId?.startsWith("MPRE") == true ||
                                content.playlistId?.startsWith("OLAK5uy") == true ||
                                content.album != null
                            )
                        }
                        albumsList.addAll(albumContents)
                    }
                }

                // 4. Home Feed Data (Categorize Albums vs Playlists)
                if (homeRes is Resource.Success) {
                    val homeItems = homeRes.data?.second ?: emptyList()
                    homeItems.forEach { homeItem ->
                        homeItem.contents.filterNotNull().forEach { content ->
                            if (content.videoId.isNullOrEmpty()) {
                                val isAlbum = content.browseId?.startsWith("MPRE") == true ||
                                        content.playlistId?.startsWith("OLAK5uy") == true ||
                                        content.album != null
                                if (isAlbum) {
                                    albumsList.add(content)
                                } else if (!content.playlistId.isNullOrEmpty() || !content.browseId.isNullOrEmpty()) {
                                    playlistsList.add(content)
                                }
                            }
                        }
                    }
                }

                // 5. Recommended Playlists (Mixed For You)
                mixedForYou?.forEach { item ->
                    playlistsList.add(
                        Content(
                            album = null,
                            artists = listOf(Artist(name = item.author, id = null)),
                            description = null,
                            isExplicit = false,
                            playlistId = item.browseId,
                            browseId = item.browseId,
                            thumbnails = item.thumbnails,
                            title = item.title,
                            videoId = null,
                            views = null
                        )
                    )
                }

                Pair(
                    albumsList.distinctBy { it.browseId ?: it.playlistId ?: it.title },
                    playlistsList.distinctBy { it.playlistId ?: it.browseId ?: it.title }
                )
            }.collect { (albums, playlists) ->
                _albumsForYou.value = albums
                _playlistsForYou.value = playlists
                _isLoading.value = false
            }
        }
    }
}
