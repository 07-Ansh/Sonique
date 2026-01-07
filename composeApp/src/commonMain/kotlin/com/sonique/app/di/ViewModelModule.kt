package com.sonique.app.di

import com.sonique.app.viewModel.AlbumViewModel
import com.sonique.app.viewModel.ArtistViewModel
import com.sonique.app.viewModel.HomeViewModel
import com.sonique.app.viewModel.LibraryDynamicPlaylistViewModel
import com.sonique.app.viewModel.LibraryViewModel
import com.sonique.app.viewModel.LocalPlaylistViewModel
import com.sonique.app.viewModel.LogInViewModel
import com.sonique.app.viewModel.MoodViewModel
import com.sonique.app.viewModel.MoreAlbumsViewModel
import com.sonique.app.viewModel.NotificationViewModel
import com.sonique.app.viewModel.NowPlayingBottomSheetViewModel
import com.sonique.app.viewModel.PlaylistViewModel
import com.sonique.app.viewModel.PodcastViewModel
import com.sonique.app.viewModel.RecentlySongsViewModel
import com.sonique.app.viewModel.SearchViewModel
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UpdateViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        single {
            SharedViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single {
            SearchViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            NowPlayingBottomSheetViewModel(
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryDynamicPlaylistViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            AlbumViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            HomeViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            SettingsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            ArtistViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            PlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LogInViewModel(
                get(),
            )
        }
        viewModel {
            PodcastViewModel(
                get(),
            )
        }
        viewModel {
            MoreAlbumsViewModel(
                get(),
            )
        }
        viewModel {
            RecentlySongsViewModel(
                get(),
            )
        }
        viewModel {
            LocalPlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            NotificationViewModel(
                get(),
            )
        }
        viewModel {
            MoodViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            UpdateViewModel(
                get(),
            )
        }
    }

