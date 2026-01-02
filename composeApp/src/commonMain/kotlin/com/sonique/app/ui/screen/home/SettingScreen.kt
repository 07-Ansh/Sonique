package com.sonique.app.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eygraber.uri.toKmpUri
import com.sonique.common.LIMIT_CACHE_SIZE
import com.sonique.common.QUALITY
import com.sonique.common.SUPPORTED_LANGUAGE
import com.sonique.common.SUPPORTED_LOCATION
import com.sonique.common.SponsorBlockType
import com.sonique.common.VIDEO_QUALITY
import com.sonique.domain.extension.now
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.manager.DataStoreManager.Values.TRUE
import com.sonique.domain.utils.LocalResource
import com.sonique.logger.Logger
import com.sonique.app.Platform
import com.sonique.app.expect.ui.fileSaverResult
import com.sonique.app.expect.ui.openEqResult
import com.sonique.app.extension.bytesToMB
import com.sonique.app.extension.displayString
import com.sonique.app.extension.isValidProxyHost
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.ActionButton
import com.sonique.app.ui.component.CenterLoadingBox
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.ui.component.ExpandableSettingCategory
import com.sonique.app.ui.navigation.destination.home.CreditDestination
import com.sonique.app.ui.navigation.destination.login.LoginDestination
import com.sonique.app.ui.navigation.destination.login.SpotifyLoginDestination
import com.sonique.app.ui.theme.DarkColors
import com.sonique.app.ui.theme.md_theme_dark_primary
import com.sonique.app.ui.theme.musica_accent
import com.sonique.app.ui.theme.typo
import com.sonique.app.ui.theme.white
import com.sonique.app.utils.VersionManager
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingBasicAlertState
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.app.viewModel.SharedViewModel

import com.mohamedrejeb.calf.core.ExperimentalCalfApi
import com.mohamedrejeb.calf.io.getPath
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.about_us
import sonique.composeapp.generated.resources.ansh_sharma
import sonique.composeapp.generated.resources.add_an_account
import sonique.composeapp.generated.resources.app_name
import sonique.composeapp.generated.resources.audio
import sonique.composeapp.generated.resources.backup
import sonique.composeapp.generated.resources.backup_downloaded
import sonique.composeapp.generated.resources.backup_downloaded_description
import sonique.composeapp.generated.resources.balance_media_loudness
import sonique.composeapp.generated.resources.baseline_close_24
import sonique.composeapp.generated.resources.baseline_people_alt_24
import sonique.composeapp.generated.resources.baseline_playlist_add_24
import sonique.composeapp.generated.resources.cancel
import sonique.composeapp.generated.resources.canvas_info
import sonique.composeapp.generated.resources.categories_sponsor_block
import sonique.composeapp.generated.resources.change
import sonique.composeapp.generated.resources.change_language_warning
import sonique.composeapp.generated.resources.clear
import sonique.composeapp.generated.resources.clear_canvas_cache
import sonique.composeapp.generated.resources.clear_downloaded_cache
import sonique.composeapp.generated.resources.clear_player_cache
import sonique.composeapp.generated.resources.clear_thumbnail_cache
import sonique.composeapp.generated.resources.content_country
import sonique.composeapp.generated.resources.database


import sonique.composeapp.generated.resources.download_quality
import sonique.composeapp.generated.resources.downloaded_cache
import sonique.composeapp.generated.resources.enable_canvas

import sonique.composeapp.generated.resources.enable_sponsor_block
import sonique.composeapp.generated.resources.enable_spotify_lyrics
import sonique.composeapp.generated.resources.free_space
import sonique.composeapp.generated.resources.guest

import sonique.composeapp.generated.resources.http

import sonique.composeapp.generated.resources.intro_login_to_spotify
import sonique.composeapp.generated.resources.invalid_host
import sonique.composeapp.generated.resources.invalid_port
import sonique.composeapp.generated.resources.keep_service_alive
import sonique.composeapp.generated.resources.keep_service_alive_description
import sonique.composeapp.generated.resources.keep_your_youtube_playlist_offline
import sonique.composeapp.generated.resources.keep_your_youtube_playlist_offline_description
import sonique.composeapp.generated.resources.kill_service_on_exit
import sonique.composeapp.generated.resources.kill_service_on_exit_description
import sonique.composeapp.generated.resources.language
import sonique.composeapp.generated.resources.limit_player_cache

import sonique.composeapp.generated.resources.log_in_to_spotify
import sonique.composeapp.generated.resources.log_out
import sonique.composeapp.generated.resources.log_out_warning
import sonique.composeapp.generated.resources.logged_in

import sonique.composeapp.generated.resources.manage_your_youtube_accounts
import sonique.composeapp.generated.resources.maxrave_dev
import sonique.composeapp.generated.resources.no_account
import sonique.composeapp.generated.resources.normalize_volume
import sonique.composeapp.generated.resources.open_system_equalizer
import sonique.composeapp.generated.resources.other_app
import sonique.composeapp.generated.resources.play_explicit_content
import sonique.composeapp.generated.resources.play_explicit_content_description
import sonique.composeapp.generated.resources.play_video_for_video_track_instead_of_audio_only
import sonique.composeapp.generated.resources.playback
import sonique.composeapp.generated.resources.player_cache
import sonique.composeapp.generated.resources.proxy
import sonique.composeapp.generated.resources.proxy_description
import sonique.composeapp.generated.resources.proxy_host
import sonique.composeapp.generated.resources.proxy_host_message
import sonique.composeapp.generated.resources.proxy_port
import sonique.composeapp.generated.resources.proxy_port_message
import sonique.composeapp.generated.resources.proxy_type
import sonique.composeapp.generated.resources.quality
import sonique.composeapp.generated.resources.restore_your_data
import sonique.composeapp.generated.resources.restore_your_saved_data

import sonique.composeapp.generated.resources.save
import sonique.composeapp.generated.resources.save_all_your_playlist_data
import sonique.composeapp.generated.resources.save_last_played
import sonique.composeapp.generated.resources.save_last_played_track_and_queue
import sonique.composeapp.generated.resources.save_playback_state
import sonique.composeapp.generated.resources.save_shuffle_and_repeat_mode
import sonique.composeapp.generated.resources.send_back_listening_data_to_google
import sonique.composeapp.generated.resources.signed_in
import sonique.composeapp.generated.resources.skip_no_music_part
import sonique.composeapp.generated.resources.skip_silent
import sonique.composeapp.generated.resources.skip_sponsor_part_of_video
import sonique.composeapp.generated.resources.socks
import sonique.composeapp.generated.resources.sponsorBlock
import sonique.composeapp.generated.resources.sponsor_block_intro
import sonique.composeapp.generated.resources.spotify
import sonique.composeapp.generated.resources.spotify_canvas_cache
import sonique.composeapp.generated.resources.spotify_lyrics_info
import sonique.composeapp.generated.resources.storage
import sonique.composeapp.generated.resources.such_as_music_video_lyrics_video_podcasts_and_more

import sonique.composeapp.generated.resources.thumbnail_cache
import sonique.composeapp.generated.resources.upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in
import sonique.composeapp.generated.resources.use_your_system_equalizer
import sonique.composeapp.generated.resources.version
import sonique.composeapp.generated.resources.version_format
import sonique.composeapp.generated.resources.video_download_quality
import sonique.composeapp.generated.resources.video_quality
import sonique.composeapp.generated.resources.warning
import sonique.composeapp.generated.resources.what_segments_will_be_skipped
import sonique.composeapp.generated.resources.youtube_account

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalCoilApi::class,

    FormatStringsInDatetimeFormats::class,
    ExperimentalCalfApi::class,
)
@Composable
fun SettingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val platformContext = LocalPlatformContext.current
    val pl = com.mohamedrejeb.calf.core.LocalPlatformContext.current
    val localDensity = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    var width by rememberSaveable { mutableIntStateOf(0) }

    // Backup and restore
    val formatter =
        LocalDateTime.Format {
            byUnicodePattern("yyyyMMddHHmmss")
        }
    val appName = stringResource(Res.string.app_name)

    val backupLauncher =
        fileSaverResult(
            "${appName}_${
                now().format(
                    formatter,
                )
            }.backup",
            "application/octet-stream",
        ) { uri ->
            uri?.let {
                viewModel.backup(it.toKmpUri())
            }
        }

    val restoreLauncher =
        rememberFilePickerLauncher(
            type =
                FilePickerFileType.All,
            selectionMode = FilePickerSelectionMode.Single,
        ) { file ->
            file.firstOrNull()?.getPath(pl)?.toKmpUri()?.let {
                viewModel.restore(it)
            }
        }

    // Open equalizer
    val resultLauncher = openEqResult(viewModel.getAudioSessionId())


    val language by viewModel.language.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val quality by viewModel.quality.collectAsStateWithLifecycle()
    val downloadQuality by viewModel.downloadQuality.collectAsStateWithLifecycle()
    val videoDownloadQuality by viewModel.videoDownloadQuality.collectAsStateWithLifecycle()
    val keepYoutubePlaylistOffline by viewModel.keepYouTubePlaylistOffline.collectAsStateWithLifecycle()
    val combineLocalAndYouTubeLiked by viewModel.combineLocalAndYouTubeLiked.collectAsStateWithLifecycle()
    val playVideo by viewModel.playVideoInsteadOfAudio.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val videoQuality by viewModel.videoQuality.collectAsStateWithLifecycle()
    val sendData by viewModel.sendBackToGoogle.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val normalizeVolume by viewModel.normalizeVolume.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val skipSilent by viewModel.skipSilent.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val savePlaybackState by viewModel.savedPlaybackState.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val saveLastPlayed by viewModel.saveRecentSongAndQueue.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val killServiceOnExit by viewModel.killServiceOnExit.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = true)

    val youtubeSubtitleLanguage by viewModel.youtubeSubtitleLanguage.collectAsStateWithLifecycle()
    val spotifyLoggedIn by viewModel.spotifyLogIn.collectAsStateWithLifecycle()
    val spotifyLyrics by viewModel.spotifyLyrics.collectAsStateWithLifecycle()
    val spotifyCanvas by viewModel.spotifyCanvas.collectAsStateWithLifecycle()
    val enableSponsorBlock by viewModel.sponsorBlockEnabled.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val skipSegments by viewModel.sponsorBlockCategories.collectAsStateWithLifecycle()
    val playerCache by viewModel.cacheSize.collectAsStateWithLifecycle()
    val downloadedCache by viewModel.downloadedCacheSize.collectAsStateWithLifecycle()
    val thumbnailCache by viewModel.thumbCacheSize.collectAsStateWithLifecycle()
    val canvasCache by viewModel.canvasCacheSize.collectAsStateWithLifecycle()
    val limitPlayerCache by viewModel.playerCacheLimit.collectAsStateWithLifecycle()
    val fraction by viewModel.fraction.collectAsStateWithLifecycle()

    val explicitContentEnabled by viewModel.explicitContentEnabled.collectAsStateWithLifecycle()
    val usingProxy by viewModel.usingProxy.collectAsStateWithLifecycle()
    val proxyType by viewModel.proxyType.collectAsStateWithLifecycle()
    val proxyHost by viewModel.proxyHost.collectAsStateWithLifecycle()
    val proxyPort by viewModel.proxyPort.collectAsStateWithLifecycle()



    val backupDownloaded by viewModel.backupDownloaded.collectAsStateWithLifecycle()


    val keepServiceAlive by viewModel.keepServiceAlive.collectAsStateWithLifecycle()






    var showYouTubeAccountDialog by rememberSaveable {
        mutableStateOf(false)
    }


    LaunchedEffect(true) {
        viewModel.getAllGoogleAccount()
    }

    LaunchedEffect(true) {
        viewModel.getData()
        viewModel.getThumbCacheSize(platformContext)
    }

    val likedSongsCount by viewModel.likedSongsCount.collectAsStateWithLifecycle()
    val likedPlaylistsCount by viewModel.likedPlaylistsCount.collectAsStateWithLifecycle()
    val followedArtistsCount by viewModel.followedArtistsCount.collectAsStateWithLifecycle()
    val usedAccount by viewModel.usedAccount.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = innerPadding,
        modifier =
            Modifier
                .padding(horizontal = 16.dp),
    ) {
        item {
            // Profile & Stats Section (Redesign)
            ProfileHeader(
                name = usedAccount?.name ?: "Guest", 
                avatarUrl = usedAccount?.thumbnailUrl,
                onEditClick = { showYouTubeAccountDialog = true }
            )

            Spacer(Modifier.height(32.dp))
        }
            // Settings Header
        // Settings Group
        item(key = "settings_group") {
            ExpandableSettingCategory(
                title = "Google",
                initialExpanded = false
            ) {
                Column {
                    SettingItem(
                        title = stringResource(Res.string.youtube_account),
                        subtitle = stringResource(Res.string.manage_your_youtube_accounts),
                        onClick = {
                            viewModel.getAllGoogleAccount()
                            showYouTubeAccountDialog = true
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.language),
                        subtitle = SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US"),
                        onClick = {
                            coroutineScope.launch {
                                    val warningStr = getString(Res.string.warning)
                                    val changeLangWarningStr = getString(Res.string.change_language_warning)
                                    val changeStr = getString(Res.string.change)
                                    val cancelStr = getString(Res.string.cancel)

                                    viewModel.setAlertData(
                                        SettingAlertState(
                                            title = getString(Res.string.language),
                                            selectOne =
                                            SettingAlertState.SelectData(
                                                listSelect =
                                                SUPPORTED_LANGUAGE.items.map {
                                                    (it.toString() == SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US")) to it.toString()
                                                },
                                            ),
                                            confirm =
                                            getString(Res.string.change) to { state ->
                                                val code = SUPPORTED_LANGUAGE.getCodeFromLanguage(state.selectOne?.getSelected() ?: "English")
                                                viewModel.setBasicAlertData(
                                                    SettingBasicAlertState(
                                                        title = warningStr,
                                                        message = changeLangWarningStr,
                                                        confirm =
                                                        changeStr to {
                                                            sharedViewModel.activityRecreate()
                                                            viewModel.setBasicAlertData(null)
                                                            viewModel.changeLanguage(code)
                                                        },
                                                        dismiss = cancelStr,
                                                    ),
                                                )
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        ),
                                    )
                            }
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.content_country),
                        subtitle = location ?: "",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = getString(Res.string.content_country),
                                        selectOne =
                                        SettingAlertState.SelectData(
                                            listSelect =
                                            SUPPORTED_LOCATION.items.map { item ->
                                                (item.toString() == location) to item.toString()
                                            },
                                        ),
                                        confirm =
                                        getString(Res.string.change) to { state ->
                                            viewModel.changeLocation(
                                                state.selectOne?.getSelected() ?: "US",
                                            )
                                        },
                                        dismiss = getString(Res.string.cancel),
                                    ),
                                )
                            }
                        },
                    )

                    SettingItem(
                        title = stringResource(Res.string.send_back_listening_data_to_google),
                        subtitle =
                        stringResource(
                            Res.string
                                .upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in,
                        ),
                        smallSubtitle = true,
                        switch = (sendData to { viewModel.setSendBackToGoogle(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.play_explicit_content),
                        subtitle = stringResource(Res.string.play_explicit_content_description),
                        switch = (explicitContentEnabled to { viewModel.setExplicitContentEnabled(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.keep_your_youtube_playlist_offline),
                        subtitle = stringResource(Res.string.keep_your_youtube_playlist_offline_description),
                        switch = (keepYoutubePlaylistOffline to { viewModel.setKeepYouTubePlaylistOffline(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.proxy),
                        subtitle = stringResource(Res.string.proxy_description),
                        switch = (usingProxy to { viewModel.setUsingProxy(it) }),
                    )
                    Crossfade(usingProxy) { it ->
                        if (it) {
                            Column {
                                SettingItem(
                                    title = stringResource(Res.string.proxy_type),
                                    subtitle =
                                    when (proxyType) {
                                        DataStoreManager.ProxyType.PROXY_TYPE_HTTP -> stringResource(Res.string.http)
                                        DataStoreManager.ProxyType.PROXY_TYPE_SOCKS -> stringResource(Res.string.socks)
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            val strSocks = getString(Res.string.socks)
                                            viewModel.setAlertData(
                                                SettingAlertState(
                                                    title = getString(Res.string.proxy_type),
                                                    selectOne =
                                                    SettingAlertState.SelectData(
                                                        listSelect =
                                                        listOf(
                                                            (proxyType == DataStoreManager.ProxyType.PROXY_TYPE_HTTP) to
                                                                    getString(
                                                                        Res.string.http,
                                                                    ),
                                                            (proxyType == DataStoreManager.ProxyType.PROXY_TYPE_SOCKS) to
                                                                    strSocks,
                                                        ),
                                                    ),
                                                    confirm =
                                                    getString(Res.string.change) to { state ->
                                                        viewModel.setProxy(
                                                            if (state.selectOne?.getSelected() == strSocks) {
                                                                DataStoreManager.ProxyType.PROXY_TYPE_SOCKS
                                                            } else {
                                                                DataStoreManager.ProxyType.PROXY_TYPE_HTTP
                                                            },
                                                            proxyHost,
                                                            proxyPort,
                                                        )
                                                    },
                                                    dismiss = getString(Res.string.cancel),
                                                ),
                                            )
                                        }
                                    },
                                )
                                SettingItem(
                                    title = stringResource(Res.string.proxy_host),
                                    subtitle = proxyHost,
                                    onClick = {
                                        coroutineScope.launch {
                                            val invalidHostMsg = getString(Res.string.invalid_host)
                                            viewModel.setAlertData(
                                                SettingAlertState(
                                                    title = getString(Res.string.proxy_host),
                                                    message = getString(Res.string.proxy_host_message),
                                                    textField =
                                                    SettingAlertState.TextFieldData(
                                                        label = getString(Res.string.proxy_host),
                                                        value = proxyHost,
                                                        verifyCodeBlock = {
                                                            isValidProxyHost(it) to invalidHostMsg
                                                        },
                                                    ),
                                                    confirm =
                                                    getString(Res.string.change) to { state ->
                                                        viewModel.setProxy(
                                                            proxyType,
                                                            state.textField?.value ?: "",
                                                            proxyPort,
                                                        )
                                                    },
                                                    dismiss = getString(Res.string.cancel),
                                                ),
                                            )
                                        }
                                    },
                                )
                                SettingItem(
                                    title = stringResource(Res.string.proxy_port),
                                    subtitle = proxyPort.toString(),
                                    onClick = {
                                        coroutineScope.launch {
                                            val invalidPortMsg = getString(Res.string.invalid_port)
                                            viewModel.setAlertData(
                                                SettingAlertState(
                                                    title = getString(Res.string.proxy_port),
                                                    message = getString(Res.string.proxy_port_message),
                                                    textField =
                                                    // Pre-calculate the string message outside the lambda if needed,
                                                    // or use the pre-calculated one.
                                                    // In this case we need to launch a coroutine to get the string,
                                                    // but we are already in a launch block (from onClick).
                                                    // However, verifyCodeBlock is a lambda stored in data class.
                                                    // It is invoked later.
                                                    // So we must capture the string value now.
                                                    // We can do this by defining the string before creating SettingAlertState.
                                                SettingAlertState.TextFieldData(
                                                    label = getString(Res.string.proxy_port),
                                                    value = proxyPort.toString(),
                                                    verifyCodeBlock = {
                                                        (it.toIntOrNull() != null) to invalidPortMsg
                                                    },
                                                ),
                                                    confirm =
                                                    getString(Res.string.change) to { state ->
                                                        viewModel.setProxy(
                                                            proxyType,
                                                            proxyHost,
                                                            state.textField?.value?.toIntOrNull() ?: 0,
                                                        )
                                                    },
                                                    dismiss = getString(Res.string.cancel),
                                                ),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Audio Group
        item(key = "audio") {
             ExpandableSettingCategory(
                title = stringResource(Res.string.audio),
                initialExpanded = false
             ) {
                 Column {
                    SettingItem(
                        title = stringResource(Res.string.quality),
                        subtitle = quality ?: "",
                        smallSubtitle = true,
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = getString(Res.string.quality),
                                        selectOne =
                                        SettingAlertState.SelectData(
                                            listSelect =
                                            QUALITY.items.map { item ->
                                                (item.toString() == quality) to item.toString()
                                            },
                                        ),
                                        confirm =
                                        getString(Res.string.change) to { state ->
                                            viewModel.changeQuality(state.selectOne?.getSelected())
                                        },
                                        dismiss = getString(Res.string.cancel),
                                    ),
                                )
                            }
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.download_quality),
                        subtitle = downloadQuality ?: "",
                        smallSubtitle = true,
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = getString(Res.string.download_quality),
                                        selectOne =
                                        SettingAlertState.SelectData(
                                            listSelect =
                                            QUALITY.items.map { item ->
                                                (item.toString() == downloadQuality) to item.toString()
                                            },
                                        ),
                                        confirm =
                                        getString(Res.string.change) to { state ->
                                            state.selectOne?.getSelected()?.let { viewModel.setDownloadQuality(it) }
                                        },
                                        dismiss = getString(Res.string.cancel),
                                    ),
                                )
                            }
                        },
                    )
                    if (getPlatform() == Platform.Android) {
                        SettingItem(
                            title = stringResource(Res.string.normalize_volume),
                            subtitle = stringResource(Res.string.balance_media_loudness),
                            switch = (normalizeVolume to { viewModel.setNormalizeVolume(it) }),
                        )
                        SettingItem(
                            title = stringResource(Res.string.skip_silent),
                            subtitle = stringResource(Res.string.skip_no_music_part),
                            switch = (skipSilent to { viewModel.setSkipSilent(it) }),
                        )
                        SettingItem(
                            title = stringResource(Res.string.open_system_equalizer),
                            subtitle = stringResource(Res.string.use_your_system_equalizer),
                            onClick = {
                                coroutineScope.launch {
                                    resultLauncher.launch()
                                }
                            },
                        )
                    }
                 }
             }
        }

        // Video Group
        if (getPlatform() != Platform.Desktop) {
            item(key = "video") {
                 ExpandableSettingCategory(
                    title = "Video", // Reusing this derived title or string resource if available
                    initialExpanded = false
                 ) {
                     Column {
                        SettingItem(
                            title = stringResource(Res.string.play_video_for_video_track_instead_of_audio_only),
                            subtitle = stringResource(Res.string.such_as_music_video_lyrics_video_podcasts_and_more),
                            smallSubtitle = true,
                            switch = (playVideo to { viewModel.setPlayVideoInsteadOfAudio(it) }),
                            isEnable = getPlatform() != Platform.Desktop,
                        )
                        SettingItem(
                            title = stringResource(Res.string.video_quality),
                            subtitle = videoQuality ?: "",
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setAlertData(
                                        SettingAlertState(
                                            title = getString(Res.string.video_quality),
                                            selectOne =
                                            SettingAlertState.SelectData(
                                                listSelect =
                                                VIDEO_QUALITY.items.map { item ->
                                                    (item.toString() == videoQuality) to item.toString()
                                                },
                                            ),
                                            confirm =
                                            getString(Res.string.change) to { state ->
                                                viewModel.changeVideoQuality(state.selectOne?.getSelected() ?: "")
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        ),
                                    )
                                }
                            },
                        )
                        SettingItem(
                            title = stringResource(Res.string.video_download_quality),
                            subtitle = videoDownloadQuality ?: "",
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setAlertData(
                                        SettingAlertState(
                                            title = getString(Res.string.video_download_quality),
                                            selectOne =
                                            SettingAlertState.SelectData(
                                                listSelect =
                                                VIDEO_QUALITY.items.map { item ->
                                                    (item.toString() == videoDownloadQuality) to item.toString()
                                                },
                                            ),
                                            confirm =
                                            getString(Res.string.change) to { state ->
                                                viewModel.setVideoDownloadQuality(state.selectOne?.getSelected() ?: "")
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        ),
                                    )
                                }
                            },
                        )
                     }
                 }
            }
        }
        // Playback Group
        item(key = "playback") {
             ExpandableSettingCategory(
                title = stringResource(Res.string.playback),
                initialExpanded = false
             ) {
                 Column {
                    SettingItem(
                        title = stringResource(Res.string.save_playback_state),
                        subtitle = stringResource(Res.string.save_shuffle_and_repeat_mode),
                        switch = (savePlaybackState to { viewModel.setSavedPlaybackState(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.save_last_played),
                        subtitle = stringResource(Res.string.save_last_played_track_and_queue),
                        switch = (saveLastPlayed to { viewModel.setSaveLastPlayed(it) }),
                    )
                    if (getPlatform() == Platform.Android) {
                        SettingItem(
                            title = stringResource(Res.string.kill_service_on_exit),
                            subtitle = stringResource(Res.string.kill_service_on_exit_description),
                            switch = (killServiceOnExit to { viewModel.setKillServiceOnExit(it) }),
                        )
                        SettingItem(
                            title = stringResource(Res.string.keep_service_alive),
                            subtitle = stringResource(Res.string.keep_service_alive_description),
                            switch = (keepServiceAlive to { viewModel.setKeepServiceAlive(it) }),
                        )
                    }
                 }
             }
        }


        // Spotify Group
        item(key = "spotify") {
             ExpandableSettingCategory(
                title = stringResource(Res.string.spotify),
                initialExpanded = false
             ) {
                 Column {
                    SettingItem(
                        title = stringResource(Res.string.log_in_to_spotify),
                        subtitle =
                            if (spotifyLoggedIn) {
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

        // SponsorBlock Group
        item(key = "sponsor_block") {
             ExpandableSettingCategory(
                title = stringResource(Res.string.sponsorBlock),
                initialExpanded = false
             ) {
                 Column {
                    SettingItem(
                        title = stringResource(Res.string.enable_sponsor_block),
                        subtitle = stringResource(Res.string.skip_sponsor_part_of_video),
                        switch = (enableSponsorBlock to { viewModel.setSponsorBlockEnabled(it) }),
                    )
                    val listName =
                        SponsorBlockType.toList().map { it.displayString() }
                    SettingItem(
                        title = stringResource(Res.string.categories_sponsor_block),
                        subtitle = stringResource(Res.string.what_segments_will_be_skipped),
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = getString(Res.string.categories_sponsor_block),
                                        multipleSelect =
                                        SettingAlertState.SelectData(
                                            listSelect =
                                            listName
                                                .mapIndexed { index, item ->
                                                    (
                                                            skipSegments?.contains(
                                                                SponsorBlockType.toList().getOrNull(index)?.value,
                                                            ) == true
                                                            ) to item
                                                }.also {
                                                    Logger.w("SettingScreen", "SettingAlertState: $skipSegments")
                                                    Logger.w("SettingScreen", "SettingAlertState: $it")
                                                },
                                        ),
                                        confirm =
                                        getString(Res.string.save) to { state ->
                                            viewModel.setSponsorBlockCategories(
                                                state.multipleSelect
                                                    ?.getListSelected()
                                                    ?.map { selected ->
                                                        listName.indexOf(selected)
                                                    }?.mapNotNull { s ->
                                                        SponsorBlockType.toList().getOrNull(s).let {
                                                            it?.value
                                                        }
                                                    }?.toCollection(ArrayList()) ?: arrayListOf(),
                                            )
                                        },
                                        dismiss = getString(Res.string.cancel),
                                    ),
                                )
                            }
                        },
                        isEnable = enableSponsorBlock,
                    )
                    val beforeUrl = stringResource(Res.string.sponsor_block_intro).substringBefore("https://sponsor.ajay.app/")
                    val afterUrl = stringResource(Res.string.sponsor_block_intro).substringAfter("https://sponsor.ajay.app/")
                    Text(
                        buildAnnotatedString {
                            append(beforeUrl)
                            withLink(
                                LinkAnnotation.Url(
                                    "https://sponsor.ajay.app/",
                                    TextLinkStyles(style = SpanStyle(color = md_theme_dark_primary)),
                                ),
                            ) {
                                append("https://sponsor.ajay.app/")
                            }
                            append(afterUrl)
                        },
                        style = typo().bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    )
                 }
             }
        }
        
        // Storage Group (Android Only)
        if (getPlatform() == Platform.Android) {
            item(key = "storage") {
                 ExpandableSettingCategory(
                    title = stringResource(Res.string.storage),
                    initialExpanded = false
                 ) {
                     Column {
                        SettingItem(
                            title = stringResource(Res.string.player_cache),
                            subtitle = "${playerCache.bytesToMB()} MB",
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setBasicAlertData(
                                        SettingBasicAlertState(
                                            title = getString(Res.string.clear_player_cache),
                                            message = null,
                                            confirm =
                                            getString(Res.string.clear) to {
                                                viewModel.clearPlayerCache()
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        ),
                                    )
                                }
                            },
                        )
                        SettingItem(
                            title = stringResource(Res.string.downloaded_cache),
                            subtitle = "${downloadedCache.bytesToMB()} MB",
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setBasicAlertData(
                                        SettingBasicAlertState(
                                            title = getString(Res.string.clear_downloaded_cache),
                                            message = null,
                                            confirm =
                                            getString(Res.string.clear) to {
                                                viewModel.clearDownloadedCache()
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        ),
                                    )
                                }
                            },
                        )
                        SettingItem(
                            title = stringResource(Res.string.thumbnail_cache),
                            subtitle = "${thumbnailCache.bytesToMB()} MB",
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setBasicAlertData(
                                        SettingBasicAlertState(
                                            title = getString(Res.string.clear_thumbnail_cache),
                                            message = null,
                                            confirm =
                                            getString(Res.string.clear) to {
                                                viewModel.clearThumbnailCache(platformContext)
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        ),
                                    )
                                }
                            },
                        )
                        SettingItem(
                            title = stringResource(Res.string.spotify_canvas_cache),
                            subtitle = "${canvasCache.bytesToMB()} MB",
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setBasicAlertData(
                                        SettingBasicAlertState(
                                            title = getString(Res.string.clear_canvas_cache),
                                            message = null,
                                            confirm =
                                            getString(Res.string.clear) to {
                                                viewModel.clearCanvasCache()
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        ),
                                    )
                                }
                            },
                        )
                        SettingItem(
                            title = stringResource(Res.string.limit_player_cache),
                            subtitle = LIMIT_CACHE_SIZE.getItemFromData(limitPlayerCache).toString(),
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setAlertData(
                                        SettingAlertState(
                                            title = getString(Res.string.limit_player_cache),
                                            selectOne =
                                            SettingAlertState.SelectData(
                                                listSelect =
                                                LIMIT_CACHE_SIZE.items.map { item ->
                                                    (item == LIMIT_CACHE_SIZE.getItemFromData(limitPlayerCache)) to item.toString()
                                                },
                                            ),
                                            confirm =
                                            getString(Res.string.change) to { state ->
                                                viewModel.setPlayerCacheLimit(
                                                    LIMIT_CACHE_SIZE.getDataFromItem(state.selectOne?.getSelected()),
                                                )
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        ),
                                    )
                                }
                            },
                        )
                        Box(
                            Modifier.padding(
                                horizontal = 24.dp,
                                vertical = 16.dp,
                            ),
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .onGloballyPositioned { layoutCoordinates ->
                                            with(localDensity) {
                                                width =
                                                    layoutCoordinates.size.width
                                                        .toDp()
                                                        .value
                                                        .toInt()
                                            }
                                        },
                            ) {
                                item {
                                    Box(
                                        modifier =
                                            Modifier
                                                .width(
                                                    (fraction.otherApp * width).dp,
                                                ).background(
                                                    md_theme_dark_primary,
                                                ).fillMaxHeight(),
                                    )
                                }
                                item {
                                    Box(
                                        modifier =
                                            Modifier
                                                .width(
                                                    (fraction.downloadCache * width).dp,
                                                ).background(
                                                    Color(0xD540FF17),
                                                ).fillMaxHeight(),
                                    )
                                }
                                item {
                                    Box(
                                        modifier =
                                            Modifier
                                                .width(
                                                    (fraction.playerCache * width).dp,
                                                ).background(
                                                    Color(0xD5FFFF00),
                                                ).fillMaxHeight(),
                                    )
                                }
                                item {
                                    Box(
                                        modifier =
                                            Modifier
                                                .width(
                                                    (fraction.canvasCache * width).dp,
                                                ).background(
                                                    Color.Cyan,
                                                ).fillMaxHeight(),
                                    )
                                }
                                item {
                                    Box(
                                        modifier =
                                            Modifier
                                                .width(
                                                    (fraction.thumbCache * width).dp,
                                                ).background(
                                                    Color.Magenta,
                                                ).fillMaxHeight(),
                                    )
                                }
                                item {
                                    Box(
                                        modifier =
                                            Modifier
                                                .width(
                                                    (fraction.appDatabase * width).dp,
                                                ).background(
                                                    Color.White,
                                                ),
                                    )
                                }
                                item {
                                    Box(
                                        modifier =
                                            Modifier
                                                .width(
                                                    (fraction.freeSpace * width).dp,
                                                ).background(
                                                    Color.DarkGray,
                                                ).fillMaxHeight(),
                                    )
                                }
                            }
                        }
                        // Legend for Storage
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        md_theme_dark_primary,
                                    ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(Res.string.other_app), style = typo().bodySmall)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.Green,
                                    ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(Res.string.downloaded_cache), style = typo().bodySmall)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        musica_accent,
                                    ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(Res.string.player_cache), style = typo().bodySmall)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.Cyan,
                                    ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(Res.string.spotify_canvas_cache), style = typo().bodySmall)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.Magenta,
                                    ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(Res.string.thumbnail_cache), style = typo().bodySmall)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.White,
                                    ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(Res.string.database), style = typo().bodySmall)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.LightGray,
                                    ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(Res.string.free_space), style = typo().bodySmall)
                        }
                     }
                 }
            }
        }
        
        // Backup Group
        item(key = "backup") {
             ExpandableSettingCategory(
                title = stringResource(Res.string.backup),
                initialExpanded = false
             ) {
                 Column {
                    SettingItem(
                        title = stringResource(Res.string.backup_downloaded),
                        subtitle = stringResource(Res.string.backup_downloaded_description),
                        switch = (backupDownloaded to { viewModel.setBackupDownloaded(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.backup),
                        subtitle = stringResource(Res.string.save_all_your_playlist_data),
                        onClick = {
                            coroutineScope.launch {
                                backupLauncher.launch()
                            }
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.restore_your_data),
                        subtitle = stringResource(Res.string.restore_your_saved_data),
                        onClick = {
                            coroutineScope.launch {
                                restoreLauncher.launch()
                            }
                        },
                    )
                 }
             }
        }
        
        // About Us Group
        item(key = "about_us") {
             ExpandableSettingCategory(
                title = stringResource(Res.string.about_us),
                initialExpanded = false
             ) {
                 Column {
                    SettingItem(
                        title = stringResource(Res.string.version),
                        subtitle = stringResource(Res.string.version_format, VersionManager.getVersionName()),
                        onClick = {
                            navController.navigate(CreditDestination)
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.maxrave_dev),
                        subtitle = stringResource(Res.string.ansh_sharma),
                        onClick = {
                        },
                    )
                 }
             }
        }
        item(key = "end") {
            EndOfPage()
        }
    }
    val basisAlertData by viewModel.basicAlertData.collectAsStateWithLifecycle()
    if (basisAlertData != null) {
        val alertBasicState = basisAlertData ?: return
        AlertDialog(
            onDismissRequest = { viewModel.setBasicAlertData(null) },
            title = {
                Text(
                    text = alertBasicState.title,
                    style = typo().titleSmall,
                )
            },
            text = {
                if (alertBasicState.message != null) {
                    Text(text = alertBasicState.message)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        alertBasicState.confirm.second.invoke()
                        viewModel.setBasicAlertData(null)
                    },
                ) {
                    Text(text = alertBasicState.confirm.first)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setBasicAlertData(null)
                    },
                ) {
                    Text(text = alertBasicState.dismiss)
                }
            },
        )
    }
    if (showYouTubeAccountDialog) {
        BasicAlertDialog(
            onDismissRequest = { },
            modifier = Modifier.wrapContentSize(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = Color(0xFF242424),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                shadowElevation = 1.dp,
            ) {
                val googleAccounts by viewModel.googleAccounts.collectAsStateWithLifecycle(
                    minActiveState = Lifecycle.State.RESUMED,
                )
                LaunchedEffect(googleAccounts) {
                    Logger.w(
                        "SettingScreen",
                        "LaunchedEffect: ${
                            googleAccounts.data?.map {
                                it.name to it.isUsed
                            }
                        }",
                    )
                }
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                        ) {
                            IconButton(
                                onClick = { showYouTubeAccountDialog = false },
                                colors =
                                    IconButtonDefaults.iconButtonColors().copy(
                                        contentColor = Color.White,
                                    ),
                                modifier =
                                    Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight(),
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = Color.White)
                            }
                            Text(
                                stringResource(Res.string.youtube_account),
                                style = typo().titleMedium,
                                modifier =
                                    Modifier
                                        .align(Alignment.Center)
                                        .wrapContentHeight(align = Alignment.CenterVertically)
                                        .wrapContentWidth(),
                            )
                        }
                    }
                    if (googleAccounts is LocalResource.Success) {
                        val data = googleAccounts.data
                        if (data.isNullOrEmpty()) {
                            item {
                                Text(
                                    stringResource(Res.string.no_account),
                                    style = typo().bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier =
                                        Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                )
                            }
                        } else {
                            items(data) {
                                Row(
                                    modifier =
                                        Modifier
                                            .padding(vertical = 8.dp)
                                            .clickable {
                                                viewModel.setUsedAccount(it)
                                            },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(Modifier.width(24.dp))
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(LocalPlatformContext.current)
                                                .data(it.thumbnailUrl)
                                                .crossfade(550)
                                                .build(),
                                        placeholder = painterResource(Res.drawable.baseline_people_alt_24),
                                        error = painterResource(Res.drawable.baseline_people_alt_24),
                                        contentDescription = it.name,
                                        modifier =
                                            Modifier
                                                .size(48.dp)
                                                .clip(CircleShape),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(it.name, style = typo().labelMedium, color = white)
                                        Text(it.email, style = typo().bodySmall)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    AnimatedVisibility(it.isUsed) {
                                        Text(
                                            stringResource(Res.string.signed_in),
                                            style = typo().bodySmall,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.widthIn(0.dp, 64.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(24.dp))
                                }
                            }
                        }
                    } else {
                        item {
                            CenterLoadingBox(
                                Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                            )
                        }
                    }
                    item {
                        Column {
                            ActionButton(
                                icon = painterResource(Res.drawable.baseline_people_alt_24),
                                text = Res.string.guest,
                            ) {
                                viewModel.setUsedAccount(null)
                                showYouTubeAccountDialog = false
                            }
                            ActionButton(
                                icon = painterResource(Res.drawable.baseline_close_24),
                                text = Res.string.log_out,
                            ) {
                                coroutineScope.launch {
                                    viewModel.setBasicAlertData(
                                        SettingBasicAlertState(
                                            title = getString(Res.string.warning),
                                            message = getString(Res.string.log_out_warning),
                                            confirm =
                                            getString(Res.string.log_out) to {
                                                viewModel.logOutAllYouTube()
                                                showYouTubeAccountDialog = false
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        ),
                                    )
                                }
                            }
                            ActionButton(
                                icon = painterResource(Res.drawable.baseline_playlist_add_24),
                                text = Res.string.add_an_account,
                            ) {
                                showYouTubeAccountDialog = false
                                navController.navigate(LoginDestination)
                            }
                        }
                    }
                }
            }
        }
    }
    val alertData by viewModel.alertData.collectAsStateWithLifecycle()
    if (alertData != null) {
        val alertState = alertData ?: return
        // AlertDialog
        AlertDialog(
            onDismissRequest = { viewModel.setAlertData(null) },
            title = {
                Text(
                    text = alertState.title,
                    style = typo().titleSmall,
                )
            },
            text = {
                if (alertState.message != null) {
                    Column {
                        Text(text = alertState.message)
                        if (alertState.textField != null) {
                            val verify =
                                alertState.textField.verifyCodeBlock?.invoke(
                                    alertState.textField.value,
                                ) ?: (true to null)
                            TextField(
                                value = alertState.textField.value,
                                onValueChange = {
                                    viewModel.setAlertData(
                                        alertState.copy(
                                            textField =
                                                alertState.textField.copy(
                                                    value = it,
                                                ),
                                        ),
                                    )
                                },
                                isError = !verify.first,
                                label = { Text(text = alertState.textField.label) },
                                supportingText = {
                                    if (!verify.first) {
                                        Text(
                                            modifier = Modifier.fillMaxWidth(),
                                            text = verify.second ?: "",
                                            color = DarkColors.error,
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (!verify.first) {
                                        Icons.Outlined.Error
                                    }
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 6.dp,
                                        ),
                            )
                        }
                    }
                } else if (alertState.selectOne != null) {
                    LazyColumn(
                        Modifier
                            .padding(vertical = 6.dp)
                            .heightIn(0.dp, 500.dp),
                    ) {
                        items(alertState.selectOne.listSelect) { item ->
                            val onSelect = {
                                viewModel.setAlertData(
                                    alertState.copy(
                                        selectOne =
                                            alertState.selectOne.copy(
                                                listSelect =
                                                    alertState.selectOne.listSelect.toMutableList().map {
                                                        if (it == item) {
                                                            true to it.second
                                                        } else {
                                                            false to it.second
                                                        }
                                                    },
                                            ),
                                    ),
                                )
                            }
                            Row(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onSelect.invoke()
                                    }.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = item.first,
                                    onClick = {
                                        onSelect.invoke()
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = item.second,
                                    style = typo().bodyMedium,
                                    maxLines = 1,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                            .basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            ).focusable(),
                                )
                            }
                        }
                    }
                } else if (alertState.multipleSelect != null) {
                    LazyColumn(
                        Modifier.padding(vertical = 6.dp),
                    ) {
                        items(alertState.multipleSelect.listSelect) { item ->
                            val onCheck = {
                                viewModel.setAlertData(
                                    alertState.copy(
                                        multipleSelect =
                                            alertState.multipleSelect.copy(
                                                listSelect =
                                                    alertState.multipleSelect.listSelect.toMutableList().map {
                                                        if (it == item) {
                                                            !it.first to it.second
                                                        } else {
                                                            it
                                                        }
                                                    },
                                            ),
                                    ),
                                )
                            }
                            Row(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onCheck.invoke()
                                    }.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = item.first,
                                    onCheckedChange = {
                                        onCheck.invoke()
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = item.second, style = typo().bodyMedium, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        alertState.confirm.second.invoke(alertState)
                        viewModel.setAlertData(null)
                    },
                    enabled =
                        if (alertState.textField?.verifyCodeBlock != null) {
                            alertState.textField.verifyCodeBlock
                                .invoke(
                                    alertState.textField.value,
                                ).first
                        } else {
                            true
                        },
                ) {
                    Text(text = alertState.confirm.first)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setAlertData(null)
                    },
                ) {
                    Text(text = alertState.dismiss)
                }
            },
        )
    }




}

