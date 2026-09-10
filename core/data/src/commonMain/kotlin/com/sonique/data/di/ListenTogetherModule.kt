package com.sonique.data.di

import com.sonique.common.Config
import com.sonique.data.listentogether.ListenTogetherPlaybackBridge
import com.sonique.data.listentogether.ListenTogetherPrefs
import com.sonique.data.listentogether.ListenTogetherRepositoryImpl
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.repository.ListenTogetherRepository
import com.sonique.listentogether.ListenTogetherClient
import com.sonique.listentogether.ListenTogetherSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.named
import org.koin.dsl.module

val listenTogetherModule =
    module {
        single {
            ListenTogetherClient(
                clientVersion = "Sonique",
                serverUrl = {
                    runBlocking { get<DataStoreManager>().getString(ListenTogetherPrefs.SERVER_URL).first().orEmpty() }
                },
            )
        }

        single { ListenTogetherSession(client = get()) }

        single<ListenTogetherRepository> {
            ListenTogetherRepositoryImpl(
                session = get(),
                scope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
            )
        }

        single(createdAtStart = true) {
            ListenTogetherPlaybackBridge(
                repository = get(),
                session = get(),
                handler = get(),
                scope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
            ).also { it.start() }
        }
    }
