package com.sonique.app.ui.navigation.graph

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sonique.app.ui.navigation.destination.home.HomeDestination
import androidx.navigation.toRoute
import com.sonique.app.ui.navigation.destination.library.LibraryDestination
import com.sonique.app.ui.navigation.destination.player.FullscreenDestination
import com.sonique.app.ui.navigation.destination.search.SearchDestination
import com.sonique.app.ui.screen.home.HomeScreen
import com.sonique.app.ui.screen.library.LibraryScreen
import com.sonique.app.ui.screen.other.SearchScreen
import com.sonique.app.ui.navigation.destination.library.DownloadsDestination
import com.sonique.app.ui.navigation.destination.library.LikedDestination
import com.sonique.app.ui.screen.other.PlaylistScreen
import com.sonique.common.LOCAL_PLAYLIST_ID_LIKED
import com.sonique.common.LOCAL_PLAYLIST_ID_DOWNLOADED
import com.sonique.app.ui.screen.player.FullscreenPlayer

@Composable
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
fun AppNavigationGraph(
    innerPadding: PaddingValues,
    navController: NavHostController,
    startDestination: Any = HomeDestination,
    hideNavBar: () -> Unit = { },
    showNavBar: (shouldShowNowPlayingSheet: Boolean) -> Unit = { },
    showNowPlayingSheet: () -> Unit = {},
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    NavHost(
        navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn()
        },
        exitTransition = {
            fadeOut()
        },
        popEnterTransition = {
            fadeIn()
        },
        popExitTransition = {
            fadeOut()
        },
    ) {
         
        composable<HomeDestination> {
            HomeScreen(
                onScrolling = onScrolling,
                navController = navController,
            )
        }
        composable<SearchDestination> {
            SearchScreen(
                navController = navController,
            )
        }
        composable<LibraryDestination> { backStackEntry ->
            val destination = backStackEntry.toRoute<LibraryDestination>()
            LibraryScreen(
                innerPadding = innerPadding,
                navController = navController,
                onScrolling = onScrolling,
                openDownloads = destination.openDownloads,
            )
        }

        composable<DownloadsDestination> {
            PlaylistScreen(
                playlistId = LOCAL_PLAYLIST_ID_DOWNLOADED,
                isYourYouTubePlaylist = false,
                navController = navController,
            )
        }
        composable<FullscreenDestination> {
            FullscreenPlayer(
                navController,
                hideNavBar = hideNavBar,
                showNavBar = {
                    showNavBar.invoke(true)
                    showNowPlayingSheet.invoke()
                },
            )
        }
         
        homeScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            hideNavBar = hideNavBar,
            showNavBar = { showNavBar(true) }
        )
         
        libraryScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
        )
         
        listScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
        )
         
        loginScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomBar = hideNavBar,
            showBottomBar = {
                showNavBar(false)
            },
        )
    }
}

