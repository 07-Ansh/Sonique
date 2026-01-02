package com.sonique.data.di.loader

import com.sonique.data.di.databaseModule
import com.sonique.data.di.mediaHandlerModule
import com.sonique.data.di.repositoryModule
import org.koin.core.context.loadKoinModules

fun loadAllModules() {
    loadKoinModules(
        listOf(
            databaseModule,
            repositoryModule,
        ),
    )
    loadKoinModules(mediaHandlerModule)
    loadMediaService()
}

expect fun loadMediaService()

