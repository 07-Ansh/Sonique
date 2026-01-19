package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.ui.navigation.destination.login.SpotifyLoginDestination
import com.sonique.app.viewModel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSpotifyScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val spotifyLoggedIn by viewModel.spotifyLogIn.collectAsStateWithLifecycle()
    val spotifyLyrics by viewModel.spotifyLyrics.collectAsStateWithLifecycle()
    val spotifyCanvas by viewModel.spotifyCanvas.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spotify") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            item {
                SettingItem(
                    title = stringResource(Res.string.log_in_to_spotify),
                    subtitle = if (spotifyLoggedIn) {
                        stringResource(Res.string.logged_in)
                    } else {
                        stringResource(Res.string.intro_login_to_spotify)
                    },
                    onClick = {
                        if (spotifyLoggedIn) {
                            viewModel.setSpotifyLogIn(false)
                        } else {
                            navController.navigate(SpotifyLoginDestination)
                        }
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.enable_spotify_lyrics),
                    subtitle = stringResource(Res.string.spotify_lyrics_info),
                    switch = (spotifyLyrics to { viewModel.setSpotifyLyrics(it) }),
                    isEnable = spotifyLoggedIn,
                    onDisable = {
                        if (spotifyLyrics) {
                            viewModel.setSpotifyLyrics(false)
                        }
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.enable_canvas),
                    subtitle = stringResource(Res.string.canvas_info),
                    switch = (spotifyCanvas to { viewModel.setSpotifyCanvas(it) }),
                    isEnable = spotifyLoggedIn,
                    onDisable = {
                        if (spotifyCanvas) {
                            viewModel.setSpotifyCanvas(false)
                        }
                    },
                )
            }
        }
    }
}
