package com.sonique.data.di

import DatabaseDao
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sonique.data.dataStore.DataStoreManagerImpl
import com.sonique.data.dataStore.createDataStoreInstance
import com.sonique.data.db.Converters
import com.sonique.data.db.LocalDataSource
import com.sonique.data.db.MusicDatabase
import com.sonique.data.db.getDatabaseBuilder
import com.sonique.domain.manager.DataStoreManager
import com.sonique.kotlinytmusicscraper.YouTube
import com.sonique.spotify.Spotify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import com.sonique.lyrics.SoniqueLyricsClient
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
val databaseModule =
    module {
        single(createdAtStart = true) {
            Converters()
        }
         
        single(createdAtStart = true) {
            getDatabaseBuilder(
                get<Converters>()
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
         
        single(createdAtStart = true) {
            get<MusicDatabase>().getDatabaseDao()
        }
         
        single(createdAtStart = true) {
            LocalDataSource(get<DatabaseDao>())
        }
         
        single(createdAtStart = true) {
            createDataStoreInstance()
        }
         
        single<DataStoreManager>(createdAtStart = true) {
            DataStoreManagerImpl(get<DataStore<Preferences>>())
        }

         
        single(createdAtStart = true) {
            YouTube()
        }

        single(createdAtStart = true) {
            Spotify()
        }

        single(createdAtStart = true) {
            SoniqueLyricsClient()
        }
    }


