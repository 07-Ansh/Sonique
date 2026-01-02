package com.sonique.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sonique.data.db.Converters
import com.sonique.data.db.LocalDataSource
import com.sonique.domain.data.entities.PairSongLocalPlaylist
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.utils.FilterState
import com.sonique.logger.Logger

internal class LocalPlaylistPagingSource(
    private val playlistId: Long,
    private val filter: FilterState,
    private val localDataSource: LocalDataSource,
) : PagingSource<Int, Pair<SongEntity, PairSongLocalPlaylist>>() {
    override fun getRefreshKey(state: PagingState<Int, Pair<SongEntity, PairSongLocalPlaylist>>): Int? = state.anchorPosition

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Pair<SongEntity, PairSongLocalPlaylist>> {
        return try {
            val currentPage = params.key ?: 0
            val pairs =
                localDataSource.getPlaylistPairSongByOffset(
                    playlistId = playlistId,
                    filterState = filter,
                    offset = currentPage,
                )
            Logger.d("LocalPlaylistPagingSource", "load: $pairs")
            val songs =
                localDataSource
                    .getSongByListVideoIdFull(
                        pairs?.map { it.songId } ?: emptyList(),
                    )
            val idValue = songs.associateBy { it.videoId }
            val sorted =
                (pairs ?: mutableListOf<PairSongLocalPlaylist>()).mapNotNull {
                    idValue[it.songId]?.let { songEntity ->
                        Pair(songEntity, it)
                    }
                }
            Logger.d("LocalPlaylistPagingSource", "load: $songs")
            return LoadResult.Page(
                data = sorted,
                prevKey = if (currentPage == 0) null else currentPage - 1,
                nextKey = if (songs.isEmpty()) null else currentPage + 1,
            )
        } catch (e: Exception) {
            Logger.e("LocalPlaylistPagingSource", "load: ${e.printStackTrace()}")
            LoadResult.Error(e)
        }
    }
}

internal class LocalPlaylistTimeBasedPagingSource(
    private val playlistId: Long,
    private val filter: FilterState,
    private val localDataSource: LocalDataSource,
) : PagingSource<Long, Pair<SongEntity, PairSongLocalPlaylist>>() {
    val converter = Converters()

    override fun getRefreshKey(state: PagingState<Long, Pair<SongEntity, PairSongLocalPlaylist>>): Long? =
        state.anchorPosition?.let { anchor ->
            state.closestItemToPosition(anchor)?.let { (_, pair) ->
                converter.dateToTimestamp(pair.inPlaylist)
            }
        }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Pair<SongEntity, PairSongLocalPlaylist>> {
        return try {
            val currentPage = params.key ?: 0L
            val timestamp =
                if (currentPage == 0L && filter == FilterState.NewerFirst) {
                    val newestPair =
                        localDataSource.getNewestPlaylistPairSong(playlistId = playlistId)
                    newestPair?.inPlaylist
                } else {
                    converter.fromTimestamp(currentPage)
                }
            val pairs =
                localDataSource.getPlaylistPairSongByTime(
                    playlistId = playlistId,
                    filterState = filter,
                    localDateTime = timestamp ?: throw Exception("Invalid timestamp"),
                )
            Logger.d("LocalPlaylistPagingSource", "load: $pairs")
            val songs =
                localDataSource
                    .getSongByListVideoIdFull(
                        pairs?.map { it.songId } ?: emptyList(),
                    )
            val idValue = songs.associateBy { it.videoId }
            val sorted =
                (pairs ?: mutableListOf<PairSongLocalPlaylist>()).mapNotNull {
                    idValue[it.songId]?.let { songEntity ->
                        Pair(songEntity, it)
                    }
                }
            Logger.d("LocalPlaylistPagingSource", "load: $songs")
            return LoadResult.Page(
                data = sorted,
                prevKey = null,
                nextKey =
                    if (songs.isEmpty()) {
                        null
                    } else {
                        pairs?.lastOrNull()?.inPlaylist.let {
                            converter.dateToTimestamp(it)
                        }
                    },
            )
        } catch (e: Exception) {
            Logger.e("LocalPlaylistPagingSource", "load: ${e.printStackTrace()}")
            LoadResult.Error(e)
        }
    }
}

