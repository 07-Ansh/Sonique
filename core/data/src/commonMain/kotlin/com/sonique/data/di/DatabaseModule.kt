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
        // Database
        single(createdAtStart = true) {
            getDatabaseBuilder(
                get<Converters>()
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        // DatabaseDao
        single(createdAtStart = true) {
            get<MusicDatabase>().getDatabaseDao()
        }
        // LocalDataSource
        single(createdAtStart = true) {
            LocalDataSource(get<DatabaseDao>())
        }
        // Datastore
        single(createdAtStart = true) {
            createDataStoreInstance()
        }
        // DatastoreManager
        single<DataStoreManager>(createdAtStart = true) {
            DataStoreManagerImpl(get<DataStore<Preferences>>())
        }

        // Move YouTube from Singleton to Koin DI
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


