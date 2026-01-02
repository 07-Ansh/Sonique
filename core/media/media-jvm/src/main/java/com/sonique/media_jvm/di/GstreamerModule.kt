package com.sonique.media_jvm.di

import com.sonique.common.Config.SERVICE_SCOPE
import com.sonique.domain.mediaservice.handler.DownloadHandler
import com.sonique.domain.mediaservice.player.MediaPlayerInterface
import com.sonique.domain.repository.CacheRepository
import com.sonique.media_jvm.GstreamerPlayerAdapter
import com.sonique.media_jvm.download.DownloadUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val gstreamerModule =
    module {
        single<CoroutineScope>(qualifier = named(SERVICE_SCOPE)) {
            CoroutineScope(Dispatchers.Main + SupervisorJob())
        }

        single<GstreamerPlayerAdapter> {
            GstreamerPlayerAdapter(
                coroutineScope = get(named(SERVICE_SCOPE)),
                dataStoreManager = get(),
                streamRepository = get(),
            )
        }

        single<MediaPlayerInterface> {
            get<GstreamerPlayerAdapter>()
        }
        single<CacheRepository> {
            object : CacheRepository {
                override suspend fun getCacheSize(cacheName: String): Long = 0L

                override fun clearCache(cacheName: String) {}

                override suspend fun getAllCacheKeys(cacheName: String): List<String> = emptyList()
            }
        }
        single<DownloadHandler> {
            DownloadUtils(
                dataStoreManager = get(),
                streamRepository = get(),
                songRepository = get(),
            )
        }
    }

fun loadGstreamerModule() = loadKoinModules(gstreamerModule)

