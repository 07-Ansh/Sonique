package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sonique.app.extension.displayString
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.ui.navigation.destination.login.SpotifyLoginDestination
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.common.SponsorBlockType
import com.sonique.domain.manager.DataStoreManager.Values.TRUE
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.*

@Composable
internal fun SpotifySettingsContent(viewModel: SettingsViewModel, navController: NavController) {
    val spotifyLoggedIn by viewModel.spotifyLogIn.collectAsStateWithLifecycle()
    val spotifyLyrics by viewModel.spotifyLyrics.collectAsStateWithLifecycle()
    val spotifyCanvas by viewModel.spotifyCanvas.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "Spotify Account",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.log_in_to_spotify)) },
                        description = {
                            Text(
                                if (spotifyLoggedIn) stringResource(Res.string.logged_in)
                                else stringResource(Res.string.intro_login_to_spotify)
                            )
                        },
                        onClick = {
                            if (spotifyLoggedIn) {
                                viewModel.setSpotifyLogIn(false)
                            } else {
                                navController.navigate(SpotifyLoginDestination)
                            }
                        }
                    )
                )
            )
        }

        item {
            Material3SettingsGroup(
                title = "Metadata Enhancements",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.enable_spotify_lyrics)) },
                        description = { Text(stringResource(Res.string.spotify_lyrics_info)) },
                        enabled = spotifyLoggedIn,
                        isSwitch = true,
                        checked = spotifyLyrics && spotifyLoggedIn,
                        onCheckedChange = { viewModel.setSpotifyLyrics(it) }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.enable_canvas)) },
                        description = { Text(stringResource(Res.string.canvas_info)) },
                        enabled = spotifyLoggedIn,
                        isSwitch = true,
                        checked = spotifyCanvas && spotifyLoggedIn,
                        onCheckedChange = { viewModel.setSpotifyCanvas(it) }
                    )
                )
            )
        }
    }
}

@Composable
internal fun SponsorBlockSettingsContent(viewModel: SettingsViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val enableSponsorBlockFlow = remember(viewModel.sponsorBlockEnabled) {
        viewModel.sponsorBlockEnabled.map { it == TRUE }
    }
    val enableSponsorBlock by enableSponsorBlockFlow.collectAsStateWithLifecycle(initialValue = false)
    val skipSegments by viewModel.sponsorBlockCategories.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "Skip Configuration",
                items = buildList {
                    add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.enable_sponsor_block)) },
                            description = { Text(stringResource(Res.string.skip_sponsor_part_of_video)) },
                            isSwitch = true,
                            checked = enableSponsorBlock,
                            onCheckedChange = { viewModel.setSponsorBlockEnabled(it) }
                        )
                    )
                    if (enableSponsorBlock) {
                        val listName = SponsorBlockType.toList().map { it.displayString() }
                        add(
                            Material3SettingsItem(
                                title = { Text(stringResource(Res.string.categories_sponsor_block)) },
                                description = { Text(stringResource(Res.string.what_segments_will_be_skipped)) },
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.setAlertData(
                                            SettingAlertState(
                                                title = getString(Res.string.categories_sponsor_block),
                                                multipleSelect = SettingAlertState.SelectData(
                                                    listSelect = listName.mapIndexed { index, item ->
                                                        (skipSegments?.contains(SponsorBlockType.toList().getOrNull(index)?.value) == true) to item
                                                    },
                                                ),
                                                confirm = getString(Res.string.save) to { state ->
                                                    viewModel.setSponsorBlockCategories(
                                                        state.multipleSelect?.getListSelected()?.map { selected ->
                                                            listName.indexOf(selected)
                                                        }?.mapNotNull { s ->
                                                            SponsorBlockType.toList().getOrNull(s)?.value
                                                        }?.toCollection(ArrayList()) ?: arrayListOf()
                                                    )
                                                },
                                                dismiss = getString(Res.string.cancel),
                                            )
                                        )
                                    }
                                }
                            )
                        )
                    }
                }
            )
        }
    }
}
