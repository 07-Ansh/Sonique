package com.liskovsoft.youtubeapi.next.v2

import com.liskovsoft.youtubeapi.common.helpers.AppClient
import com.liskovsoft.youtubeapi.common.helpers.PostDataType
import com.liskovsoft.youtubeapi.common.helpers.QueryBuilder

internal object WatchNextApiHelper {
    fun getWatchNextQuery(client: AppClient, videoId: String): String {
        return getWatchNextQuery(client, videoId, null, 0)
    }

    fun getWatchNextQuery(appClient: AppClient, videoId: String?, playlistId: String?, playlistIndex: Int): String {
        return getWatchNextQuery(appClient, videoId, playlistId, playlistIndex, null)
    }

     
    fun getWatchNextQuery(client: AppClient, videoId: String?, playlistId: String?, playlistIndex: Int, playlistParams: String?): String {
        return QueryBuilder(client)
            .setType(PostDataType.Browse)
            .setVideoId(videoId ?: "")  
            .setPlaylistId(playlistId)
            .setPlaylistIndex(playlistIndex)
            .setParams(playlistParams)  
            .build()
    }

    fun getUnlocalizedTitleQuery(videoId: String): String {
        return "https://www.youtube.com/watch?v=$videoId"
    }
}

