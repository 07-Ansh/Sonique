package com.sonique.app.ui.navigation.graph

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sonique.app.ui.navigation.destination.home.HomeDestination
import com.sonique.app.ui.navigation.destination.home.SettingsDestination
import com.sonique.app.ui.screen.home.SettingScreen

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
    enablePageTransitions: Boolean = false,
    hideNavBar: () -> Unit = { },
    showNavBar: (shouldShowNowPlayingSheet: Boolean) -> Unit = { },
    showNowPlayingSheet: () -> Unit = {},
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    NavHost(
        navController,
        startDestination = startDestination,
        enterTransition = {
            if (!enablePageTransitions) {
                fadeIn(animationSpec = tween(300))
            } else {
                val initialIndex = getTabExtensionIndex(initialState.destination.route)
                val targetIndex = getTabExtensionIndex(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                }
            }
        },
        exitTransition = {
            if (!enablePageTransitions) {
                fadeOut(animationSpec = tween(300))
            } else {
                val initialIndex = getTabExtensionIndex(initialState.destination.route)
                val targetIndex = getTabExtensionIndex(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            }
        },
        popEnterTransition = {
            if (!enablePageTransitions) {
                fadeIn(animationSpec = tween(300))
            } else {
                val initialIndex = getTabExtensionIndex(initialState.destination.route)
                val targetIndex = getTabExtensionIndex(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                }
            }
        },
        popExitTransition = {
            if (!enablePageTransitions) {
                fadeOut(animationSpec = tween(300))
            } else {
                val initialIndex = getTabExtensionIndex(initialState.destination.route)
                val targetIndex = getTabExtensionIndex(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            }
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
                onScrolling = onScrolling,
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

import com.sonique.app.ui.navigation.destination.library.PlaylistsDestination

        composable<PlaylistsDestination> {
            LibraryScreen(
                innerPadding = innerPadding,
                navController = navController,
                onScrolling = onScrolling,
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
        composable<SettingsDestination> {
            SettingScreen(
                innerPadding = innerPadding,
                navController = navController,
            )
        }
         
        homeScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            enablePageTransitions = enablePageTransitions,
            hideNavBar = hideNavBar,
            showNavBar = { showNavBar(true) }
        )
         
        libraryScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            onScrolling = onScrolling,
        )
         
        listScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            onScrolling = onScrolling,
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

private fun getTabExtensionIndex(route: String?): Int {
    if (route == null) return 0
    return when {
        route.contains("HomeDestination") -> 0
        route.contains("SearchDestination") -> 1
        route.contains("PlaylistsDestination") -> 2
        route.contains("LibraryDestination") -> 3
        else -> 0
    }
}

