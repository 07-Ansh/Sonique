package com.sonique.app.ui.navigation.graph

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.sonique.app.ui.navigation.destination.home.CreditDestination
import com.sonique.app.ui.navigation.destination.home.MoodDestination
import com.sonique.app.ui.navigation.destination.home.NotificationDestination
import com.sonique.app.ui.navigation.destination.home.RecentlySongsDestination
import com.sonique.app.ui.navigation.destination.home.SettingsDestination
import com.sonique.app.ui.screen.home.MoodScreen
import com.sonique.app.ui.screen.home.NotificationScreen
import com.sonique.app.ui.screen.home.RecentlySongsScreen
import com.sonique.app.ui.screen.home.SettingScreen
import com.sonique.app.ui.screen.other.CreditScreen

fun NavGraphBuilder.homeScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
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
            slideInVertically(
                initialOffsetY = { it },  
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutVertically(
                targetOffsetY = { it },  
                animationSpec = tween(300)
            )
        }
    ) {
        SettingScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
}

