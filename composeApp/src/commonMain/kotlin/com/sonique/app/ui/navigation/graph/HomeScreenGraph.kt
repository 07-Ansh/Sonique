package com.sonique.app.ui.navigation.graph

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.sonique.app.ui.navigation.destination.home.*
import com.sonique.app.ui.screen.home.MoodScreen
import com.sonique.app.ui.screen.home.NotificationScreen
import com.sonique.app.ui.screen.home.RecentlySongsScreen
import com.sonique.app.ui.screen.home.SettingScreen
import com.sonique.app.ui.screen.other.CreditScreen
import com.sonique.app.ui.screen.settings.*
import com.sonique.app.viewModel.SharedViewModel
import org.koin.compose.koinInject

fun NavGraphBuilder.homeScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
    hideNavBar: () -> Unit = { },
    showNavBar: () -> Unit = { },
) {
    composable<CreditDestination> {
        CreditScreen(
            paddingValues = innerPadding,
            navController = navController,
        )
    }
    composable<MoodDestination> { entry ->
        val params = entry.toRoute<MoodDestination>().params
        MoodScreen(
            navController = navController,
            params = params,
        )
    }
    composable<NotificationDestination> {
        NotificationScreen(
            navController = navController,
        )
    }
    composable<RecentlySongsDestination> {
        RecentlySongsScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<SettingsDestination>(
        enterTransition = {
            fadeIn(animationSpec = tween(100))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(100))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(100))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(100))
        }
    ) {

        SettingScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }

    composable<SettingsGeneralDestination>(
        enterTransition = { fadeIn(animationSpec = tween(100)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = { fadeIn(animationSpec = tween(100)) },
        popExitTransition = { fadeOut(animationSpec = tween(100)) }
    ) {

        val sharedViewModel = koinInject<SharedViewModel>()
        SettingsGeneralScreen(
            navController = navController,
            sharedViewModel = sharedViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable<SettingsAudioDestination>(
        enterTransition = { fadeIn(animationSpec = tween(100)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = { fadeIn(animationSpec = tween(100)) },
        popExitTransition = { fadeOut(animationSpec = tween(100)) }
    ) {

        SettingsAudioScreen(
            onBack = { navController.popBackStack() }
        )
    }


    composable<SettingsPlaybackDestination>(
        enterTransition = { fadeIn(animationSpec = tween(100)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = { fadeIn(animationSpec = tween(100)) },
        popExitTransition = { fadeOut(animationSpec = tween(100)) }
    ) {

        SettingsPlaybackScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable<SettingsSpotifyDestination>(
        enterTransition = { fadeIn(animationSpec = tween(100)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = { fadeIn(animationSpec = tween(100)) },
        popExitTransition = { fadeOut(animationSpec = tween(100)) }
    ) {

        SettingsSpotifyScreen(
            navController = navController,
            onBack = { navController.popBackStack() }
        )
    }

    composable<SettingsSponsorBlockDestination>(
        enterTransition = { fadeIn(animationSpec = tween(100)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = { fadeIn(animationSpec = tween(100)) },
        popExitTransition = { fadeOut(animationSpec = tween(100)) }
    ) {

        SettingsSponsorBlockScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable<SettingsStorageDestination>(
        enterTransition = { fadeIn(animationSpec = tween(100)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = { fadeIn(animationSpec = tween(100)) },
        popExitTransition = { fadeOut(animationSpec = tween(100)) }
    ) {

        SettingsStorageScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable<SettingsBackupDestination>(
        enterTransition = { fadeIn(animationSpec = tween(100)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = { fadeIn(animationSpec = tween(100)) },
        popExitTransition = { fadeOut(animationSpec = tween(100)) }
    ) {

        SettingsBackupScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable<SettingsAboutDestination>(
        enterTransition = { fadeIn(animationSpec = tween(100)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = { fadeIn(animationSpec = tween(100)) },
        popExitTransition = { fadeOut(animationSpec = tween(100)) }
    ) {

        SettingsAboutScreen(
            navController = navController,
            onBack = { navController.popBackStack() }
        )
    }
}

