package com.sonique.data.mediaservice

actual fun createMediaServiceHandler(
    dataStoreManager: com.sonique.domain.manager.DataStoreManager,
    songRepository: com.sonique.domain.repository.SongRepository,
    streamRepository: com.sonique.domain.repository.StreamRepository,
    localPlaylistRepository: com.sonique.domain.repository.LocalPlaylistRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): com.sonique.domain.mediaservice.handler.MediaPlayerHandler =
    MediaServiceHandlerImpl(
        dataStoreManager,
        songRepository,
        streamRepository,
        localPlaylistRepository,
        coroutineScope,
    )

