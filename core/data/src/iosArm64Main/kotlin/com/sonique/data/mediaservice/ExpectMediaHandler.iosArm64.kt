package com.sonique.data.mediaservice

import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.mediaservice.handler.MediaPlayerHandler
import com.sonique.domain.repository.LocalPlaylistRepository
import com.sonique.domain.repository.SongRepository
import com.sonique.domain.repository.StreamRepository
import kotlinx.coroutines.CoroutineScope

actual fun createMediaServiceHandler(
    dataStoreManager: DataStoreManager,
    songRepository: SongRepository,
    streamRepository: StreamRepository,
    localPlaylistRepository: LocalPlaylistRepository,
    coroutineScope: CoroutineScope,
): MediaPlayerHandler {
    TODO("Not yet implemented")
}

