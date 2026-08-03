package com.sonique.app.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonique.domain.data.model.home.HomeItem
import com.sonique.domain.repository.HomeRepository
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
) : ViewModel() {

    private val _albumSections = MutableStateFlow<List<HomeItem>>(emptyList())
    val albumSections: StateFlow<List<HomeItem>> = _albumSections.asStateFlow()

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
                )
            ) { homeRes, newReleaseRes ->
                val resultList = mutableListOf<HomeItem>()

                if (homeRes is Resource.Success) {
                    val items = homeRes.data?.second ?: emptyList()
                    val albumHomeItems = items.mapNotNull { item ->
                        val albumContents = item.contents.filter { content ->
                            content?.browseId?.startsWith("MPRE") == true ||
                            (content?.videoId == null && content?.playlistId?.startsWith("OLAK5uy") == true)
                        }
                        if (albumContents.isNotEmpty()) {
                            item.copy(contents = albumContents)
                        } else null
                    }
                    resultList.addAll(albumHomeItems)
                }

                if (newReleaseRes is Resource.Success) {
                    val newReleases = newReleaseRes.data ?: emptyList()
                    val newReleaseAlbums = newReleases.mapNotNull { item ->
                        val albumContents = item.contents.filter { content ->
                            content?.browseId?.startsWith("MPRE") == true ||
                            (content?.videoId == null && content?.playlistId?.startsWith("OLAK5uy") == true)
                        }
                        if (albumContents.isNotEmpty()) {
                            item.copy(contents = albumContents)
                        } else null
                    }
                    resultList.addAll(newReleaseAlbums)
                }

                resultList
            }.collect { filteredSections ->
                _albumSections.value = filteredSections
                _isLoading.value = false
            }
        }
    }
}
