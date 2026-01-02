package com.sonique.data.di

import com.sonique.common.Config.SERVICE_SCOPE
import com.sonique.data.io.fileDir
import com.sonique.data.repository.AccountRepositoryImpl
import com.sonique.data.repository.AlbumRepositoryImpl
import com.sonique.data.repository.ArtistRepositoryImpl
import com.sonique.data.repository.CommonRepositoryImpl
import com.sonique.data.repository.HomeRepositoryImpl
import com.sonique.data.repository.LocalPlaylistRepositoryImpl
import com.sonique.data.repository.LyricsCanvasRepositoryImpl
import com.sonique.data.repository.PlaylistRepositoryImpl
import com.sonique.data.repository.PodcastRepositoryImpl
import com.sonique.data.repository.SearchRepositoryImpl
import com.sonique.data.repository.SongRepositoryImpl
import com.sonique.data.repository.StreamRepositoryImpl
import com.sonique.data.repository.UpdateRepositoryImpl
import com.sonique.domain.repository.AccountRepository
import com.sonique.domain.repository.AlbumRepository
import com.sonique.domain.repository.ArtistRepository
import com.sonique.domain.repository.CommonRepository
import com.sonique.domain.repository.HomeRepository
import com.sonique.domain.repository.LocalPlaylistRepository
import com.sonique.domain.repository.LyricsCanvasRepository
import com.sonique.domain.repository.PlaylistRepository
import com.sonique.domain.repository.PodcastRepository
import com.sonique.domain.repository.SearchRepository
import com.sonique.domain.repository.SongRepository
import com.sonique.domain.repository.StreamRepository
import com.sonique.domain.repository.UpdateRepository
import com.sonique.data.db.LocalDataSource
import com.sonique.data.db.MusicDatabase
import com.sonique.domain.manager.DataStoreManager
import com.sonique.kotlinytmusicscraper.YouTube
import com.sonique.spotify.Spotify
import kotlinx.coroutines.CoroutineScope
import org.koin.core.qualifier.named
import org.koin.dsl.module
import com.sonique.lyrics.SoniqueLyricsClient

val repositoryModule =
    module {
        single<AccountRepository>(createdAtStart = true) {
            AccountRepositoryImpl(get(), get())
        }

        single<AlbumRepository>(createdAtStart = true) {
            AlbumRepositoryImpl(get(), get())
        }

        single<ArtistRepository>(createdAtStart = true) {
            ArtistRepositoryImpl(get(), get())
        }

        single<CommonRepository>(createdAtStart = true) {
            CommonRepositoryImpl(
                get<CoroutineScope>(named(SERVICE_SCOPE)),
                get<MusicDatabase>(),
                get<LocalDataSource>(),
                get<YouTube>(),
                get<Spotify>()
            ).apply {
                this.init("${fileDir()}/ytdlp-cookie.txt", get<DataStoreManager>())
            }
        }

        single<HomeRepository>(createdAtStart = true) {
            HomeRepositoryImpl(get(), get())
        }

        single<LocalPlaylistRepository>(createdAtStart = true) {
            LocalPlaylistRepositoryImpl(get(), get())
        }

        single<LyricsCanvasRepository>(createdAtStart = true) {
            LyricsCanvasRepositoryImpl(
                get<LocalDataSource>(),
                get<YouTube>(),
                get<Spotify>(),
                get<SoniqueLyricsClient>(),
            )
        }

        single<PlaylistRepository>(createdAtStart = true) {
            PlaylistRepositoryImpl(get(), get(), get())
        }

        single<PodcastRepository>(createdAtStart = true) {
            PodcastRepositoryImpl(get(), get())
        }

        single<SearchRepository>(createdAtStart = true) {
            SearchRepositoryImpl(get(), get())
        }

        single<SongRepository>(createdAtStart = true) {
            SongRepositoryImpl(get(), get(), get())
        }

        single<StreamRepository>(createdAtStart = true) {
            StreamRepositoryImpl(get(), get())
        }

        single<UpdateRepository>(createdAtStart = true) {
            UpdateRepositoryImpl(get())
        }
    }


