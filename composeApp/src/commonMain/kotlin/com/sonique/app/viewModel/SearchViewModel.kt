package com.sonique.app.viewModel

import androidx.lifecycle.viewModelScope
import com.sonique.common.SELECTED_LANGUAGE
import com.sonique.domain.data.entities.SearchHistory
import com.sonique.domain.data.model.searchResult.albums.AlbumsResult
import com.sonique.domain.data.model.searchResult.artists.ArtistsResult
import com.sonique.domain.data.model.searchResult.playlists.PlaylistsResult
import com.sonique.domain.data.model.searchResult.songs.SongsResult
import com.sonique.domain.data.model.searchResult.videos.VideosResult
import com.sonique.domain.data.type.SearchResultType
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.repository.SearchRepository
import com.sonique.domain.utils.Resource
import com.sonique.domain.utils.toQueryList
import com.sonique.logger.LogLevel
import com.sonique.logger.Logger
import com.sonique.app.viewModel.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.albums
import sonique.composeapp.generated.resources.all
import sonique.composeapp.generated.resources.artists
import sonique.composeapp.generated.resources.featured_playlists
import sonique.composeapp.generated.resources.playlists
import sonique.composeapp.generated.resources.podcasts
import sonique.composeapp.generated.resources.songs
import sonique.composeapp.generated.resources.videos

 
data class SearchScreenState(
    val searchType: SearchType = SearchType.ALL,
    val searchAllResult: List<SearchResultType> = emptyList(),
    val searchSongsResult: List<SongsResult> = emptyList(),
    val searchVideosResult: List<VideosResult> = emptyList(),
    val searchAlbumsResult: List<AlbumsResult> = emptyList(),
    val searchArtistsResult: List<ArtistsResult> = emptyList(),
    val searchPlaylistsResult: List<PlaylistsResult> = emptyList(),
    val searchFeaturedPlaylistsResult: List<PlaylistsResult> = emptyList(),
    val searchPodcastsResult: List<PlaylistsResult> = emptyList(),
    val suggestQueries: List<String> = emptyList(),
    val suggestYTItems: List<SearchResultType> = emptyList(),
)

 
enum class SearchType {
    ALL,
    SONGS,
    VIDEOS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    FEATURED_PLAYLISTS,
    PODCASTS,
}

fun SearchType.toStringRes(): StringResource =
    when (this) {
        SearchType.ALL -> Res.string.all
        SearchType.SONGS -> Res.string.songs
        SearchType.VIDEOS -> Res.string.videos
        SearchType.ALBUMS -> Res.string.albums
        SearchType.ARTISTS -> Res.string.artists
        SearchType.PLAYLISTS -> Res.string.playlists
        SearchType.FEATURED_PLAYLISTS -> Res.string.featured_playlists
        SearchType.PODCASTS -> Res.string.podcasts
    }

 
sealed class SearchScreenUIState {
    object Empty : SearchScreenUIState()

    object Loading : SearchScreenUIState()

    object Success : SearchScreenUIState()

    object Error : SearchScreenUIState()
}

class SearchViewModel(
    private val dataStoreManager: DataStoreManager,
    private val searchRepository: SearchRepository,
) : BaseViewModel() {
    private val _searchScreenUIState = MutableStateFlow<SearchScreenUIState>(SearchScreenUIState.Empty)
    val searchScreenUIState: StateFlow<SearchScreenUIState> get() = _searchScreenUIState.asStateFlow()

    private val _searchScreenState = MutableStateFlow(SearchScreenState())
    val searchScreenState: StateFlow<SearchScreenState> get() = _searchScreenState.asStateFlow()

    private val _searchHistory: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val searchHistory: StateFlow<List<String>> get() = _searchHistory.asStateFlow()

    var regionCode: String? = null
    var language: String? = null

    init {
        viewModelScope.launch {
            regionCode = dataStoreManager.location.first()
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
            getSearchHistory()
        }
    }

    private fun getSearchHistory() {
        viewModelScope.launch {
            searchRepository.getSearchHistory().collect { values ->
                if (values.isNotEmpty()) {
                    values.toQueryList().reversed().let { list ->
                        _searchHistory.value = list
                        log("Search history updated: $list")
                    }
                } else {
                    _searchHistory.value = emptyList()
                    log("Search history is empty")
                }
            }
        }
    }

    fun insertSearchHistory(query: String) {
        viewModelScope.launch {
            searchRepository.insertSearchHistory(SearchHistory(query = query)).collectLatest {
                Logger.d(tag, "Inserted search history: $query, $it")
                getSearchHistory()
            }
        }
    }

    fun deleteSearchHistory() {
        viewModelScope.launch {
            searchRepository.deleteSearchHistory()
            delay(1000)
            getSearchHistory()
        }
    }

    fun searchSongs(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            searchRepository.getSearchDataSong(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { songsList ->
                            _searchScreenState.update { state ->
                                state.copy(searchSongsResult = songsList)
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchAll(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            var song = ArrayList<SongsResult>()
            val video = ArrayList<VideosResult>()
            var album = ArrayList<AlbumsResult>()
            var artist = ArrayList<ArtistsResult>()
            var playlist = ArrayList<PlaylistsResult>()
            var featuredPlaylist = ArrayList<PlaylistsResult>()
            var podcast = ArrayList<PlaylistsResult>()
            val temp: ArrayList<SearchResultType> = ArrayList()
            
            var errorCount = 0
            val totalRequests = 7

            val job1 =
                launch {
                    searchRepository.getSearchDataSong(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { song = it }
                            is Resource.Error -> errorCount++
                        }
                    }
                }
            val job2 =
                launch {
                    searchRepository.getSearchDataArtist(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { artist = it }
                            is Resource.Error -> errorCount++
                        }
                    }
                }
            val job3 =
                launch {
                    searchRepository
                        .getSearchDataAlbum(query)
                        .collect { values ->
                            when (values) {
                                is Resource.Success -> values.data?.let { album = it }
                                is Resource.Error -> errorCount++
                            }
                        }
                }
            val job4 =
                launch {
                    searchRepository.getSearchDataPlaylist(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { playlist = it }
                            is Resource.Error -> errorCount++
                        }
                    }
                }
            val job5 =
                launch {
                    searchRepository.getSearchDataVideo(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { video.addAll(it) }
                            is Resource.Error -> errorCount++
                        }
                    }
                }
            val job6 =
                launch {
                    searchRepository.getSearchDataFeaturedPlaylist(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { featuredPlaylist = it }
                            is Resource.Error -> errorCount++
                        }
                    }
                }
            val job7 =
                launch {
                    searchRepository.getSearchDataPodcast(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { podcast = it }
                            is Resource.Error -> errorCount++
                        }
                    }
                }
            job1.join()
            job2.join()
            job3.join()
            job4.join()
            job5.join()
            job6.join()
            job7.join()

             
            if (errorCount == totalRequests) {
                _searchScreenUIState.value = SearchScreenUIState.Error
                return@launch
            }

            try {
                if (artist.size >= 3) {
                    for (i in 0..2) {
                        temp += artist[i]
                    }
                    temp.addAll(song)
                    temp.addAll(video)
                    temp.addAll(album)
                    temp.addAll(playlist)
                    temp.addAll(featuredPlaylist)
                    temp.addAll(podcast)
                } else {
                    temp.addAll(artist)
                    temp.addAll(song)
                    temp.addAll(video)
                    temp.addAll(album)
                    temp.addAll(playlist)
                    temp.addAll(featuredPlaylist)
                    temp.addAll(podcast)
                }

                _searchScreenState.update { state ->
                    state.copy(
                        searchType = SearchType.ALL,
                        searchAllResult = temp,
                        searchSongsResult = song,
                        searchArtistsResult = artist,
                        searchAlbumsResult = album,
                        searchPlaylistsResult = playlist,
                        searchVideosResult = video,
                        searchFeaturedPlaylistsResult = featuredPlaylist,
                        searchPodcastsResult = podcast,
                    )
                }
                
                if (temp.isEmpty() && errorCount > 0) {
                      
                     _searchScreenUIState.value = SearchScreenUIState.Error
                } else {
                     _searchScreenUIState.value = SearchScreenUIState.Success
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                _searchScreenUIState.value = SearchScreenUIState.Error
            }
        }
    }

    fun suggestQuery(query: String) {
        viewModelScope.launch {
            searchRepository.getSuggestQuery(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { suggestData ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    suggestQueries = suggestData.queries,
                                    suggestYTItems = suggestData.recommendedItems,
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                         
                        log("Error fetching suggest queries: ${values.message}", LogLevel.ERROR)
                    }
                }
            }
        }
    }

    fun searchAlbums(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            searchRepository.getSearchDataAlbum(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { albumsList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.ALBUMS,
                                    searchAlbumsResult = albumsList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchFeaturedPlaylist(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            searchRepository.getSearchDataFeaturedPlaylist(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { featuredPlaylistList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.FEATURED_PLAYLISTS,
                                    searchFeaturedPlaylistsResult = featuredPlaylistList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchPodcast(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            searchRepository.getSearchDataPodcast(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { podcastList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.PODCASTS,
                                    searchPodcastsResult = podcastList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchArtists(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            searchRepository.getSearchDataArtist(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { artistsList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.ARTISTS,
                                    searchArtistsResult = artistsList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchPlaylists(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            searchRepository.getSearchDataPlaylist(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { playlistsList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.PLAYLISTS,
                                    searchPlaylistsResult = playlistsList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchVideos(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            searchRepository.getSearchDataVideo(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { videosList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.VIDEOS,
                                    searchVideosResult = videosList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun setSearchType(searchType: SearchType) {
        _searchScreenState.update { state ->
            state.copy(searchType = searchType)
        }
    }
}

