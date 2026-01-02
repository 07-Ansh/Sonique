package com.sonique.app.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.sonique.app.ui.navigation.destination.login.LoginDestination
import com.sonique.app.ui.navigation.destination.login.SpotifyLoginDestination
import com.sonique.app.ui.screen.login.LoginScreen
import com.sonique.app.ui.screen.login.SpotifyLoginScreen

fun NavGraphBuilder.loginScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
    hideBottomBar: () -> Unit,
    showBottomBar: () -> Unit,
) {
    composable<LoginDestination> {
        LoginScreen(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomNavigation = hideBottomBar,
            showBottomNavigation = showBottomBar,
        )
    }

    composable<SpotifyLoginDestination> {
        SpotifyLoginScreen(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomNavigation = hideBottomBar,
            showBottomNavigation = showBottomBar,
        )
    }


}

