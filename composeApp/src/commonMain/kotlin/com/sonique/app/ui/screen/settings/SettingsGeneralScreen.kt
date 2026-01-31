package com.sonique.app.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sonique.app.extension.displayString
import com.sonique.app.extension.isValidProxyHost
import com.sonique.app.ui.component.ActionButton
import com.sonique.app.ui.component.CenterLoadingBox
import com.sonique.app.ui.component.SettingBasicDialog
import com.sonique.app.ui.component.SettingDialog
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.ui.navigation.destination.login.LoginDestination
import com.sonique.app.ui.theme.DarkColors
import com.sonique.app.ui.theme.backgroundCard
import com.sonique.app.ui.theme.typo
import com.sonique.app.ui.theme.white
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingBasicAlertState
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.common.SUPPORTED_LANGUAGE
import com.sonique.common.SUPPORTED_LOCATION
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.manager.DataStoreManager.Values.TRUE
import com.sonique.domain.utils.LocalResource
import com.sonique.logger.Logger
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGeneralScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    val language by viewModel.language.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val sendData by viewModel.sendBackToGoogle.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val explicitContentEnabled by viewModel.explicitContentEnabled.collectAsStateWithLifecycle()
    val keepYoutubePlaylistOffline by viewModel.keepYouTubePlaylistOffline.collectAsStateWithLifecycle()
    val usingProxy by viewModel.usingProxy.collectAsStateWithLifecycle()
    val proxyType by viewModel.proxyType.collectAsStateWithLifecycle()
    val proxyHost by viewModel.proxyHost.collectAsStateWithLifecycle()
    val proxyPort by viewModel.proxyPort.collectAsStateWithLifecycle()

    var showYouTubeAccountDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val alertData by viewModel.alertData.collectAsStateWithLifecycle()
    val basicAlertData by viewModel.basicAlertData.collectAsStateWithLifecycle()

    if (alertData != null) {
        SettingDialog(
            alert = alertData!!,
            onDismiss = { viewModel.setAlertData(null) }
        )
    }
    if (basicAlertData != null) {
        SettingBasicDialog(
            alert = basicAlertData!!,
            onDismiss = { viewModel.setBasicAlertData(null) }
        )
    }

    LaunchedEffect(true) {
        viewModel.getData()
        viewModel.getAllGoogleAccount()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General") },
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
                                    selectOne = SettingAlertState.SelectData(
                                        listSelect = SUPPORTED_LANGUAGE.items.map {
                                            (it.toString() == SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US")) to it.toString()
                                        },
                                    ),
                                    confirm = getString(Res.string.change) to { state ->
                                        val code = SUPPORTED_LANGUAGE.getCodeFromLanguage(state.selectOne?.getSelected() ?: "English")
                                        viewModel.setBasicAlertData(
                                            SettingBasicAlertState(
                                                title = warningStr,
                                                message = changeLangWarningStr,
                                                confirm = changeStr to {
                                                    sharedViewModel.activityRecreate()
                                                    viewModel.setBasicAlertData(null)
                                                    viewModel.changeLanguage(code)
                                                },
                                                dismiss = cancelStr,
                                            ),
                                        )
                                    },
                                    dismiss = getString(Res.string.cancel),
                                )
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
                                    selectOne = SettingAlertState.SelectData(
                                        listSelect = SUPPORTED_LOCATION.items.map { item ->
                                            (item.toString() == location) to item.toString()
                                        },
                                    ),
                                    confirm = getString(Res.string.change) to { state ->
                                        viewModel.changeLocation(
                                            state.selectOne?.getSelected() ?: "US",
                                        )
                                    },
                                    dismiss = getString(Res.string.cancel),
                                )
                            )
                        }
                    },
                )

                SettingItem(
                    title = stringResource(Res.string.send_back_listening_data_to_google),
                    subtitle = stringResource(Res.string.upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in),
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
                                subtitle = when (proxyType) {
                                    DataStoreManager.ProxyType.PROXY_TYPE_HTTP -> stringResource(Res.string.http)
                                    DataStoreManager.ProxyType.PROXY_TYPE_SOCKS -> stringResource(Res.string.socks)
                                },
                                onClick = {
                                    coroutineScope.launch {
                                        val strSocks = getString(Res.string.socks)
                                        viewModel.setAlertData(
                                            SettingAlertState(
                                                title = getString(Res.string.proxy_type),
                                                selectOne = SettingAlertState.SelectData(
                                                    listSelect = listOf(
                                                        (proxyType == DataStoreManager.ProxyType.PROXY_TYPE_HTTP) to getString(Res.string.http),
                                                        (proxyType == DataStoreManager.ProxyType.PROXY_TYPE_SOCKS) to strSocks,
                                                    ),
                                                ),
                                                confirm = getString(Res.string.change) to { state ->
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
                                                textField = SettingAlertState.TextFieldData(
                                                    label = getString(Res.string.proxy_host),
                                                    value = proxyHost,
                                                    verifyCodeBlock = {
                                                        isValidProxyHost(it) to invalidHostMsg
                                                    },
                                                ),
                                                confirm = getString(Res.string.change) to { state ->
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
                                                textField = SettingAlertState.TextFieldData(
                                                    label = getString(Res.string.proxy_port),
                                                    value = proxyPort.toString(),
                                                    verifyCodeBlock = {
                                                        (it.toIntOrNull() != null) to invalidPortMsg
                                                    },
                                                ),
                                                confirm = getString(Res.string.change) to { state ->
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
    
    // Copying the Dialog logic from SettingScreen.kt
    if (showYouTubeAccountDialog) {
        BasicAlertDialog(
            onDismissRequest = { },
            modifier = Modifier.wrapContentSize(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = backgroundCard,
                tonalElevation = AlertDialogDefaults.TonalElevation,
                shadowElevation = 1.dp,
            ) {
                val googleAccounts by viewModel.googleAccounts.collectAsStateWithLifecycle(
                    minActiveState = Lifecycle.State.RESUMED,
                )
                LaunchedEffect(googleAccounts) {
                   // Logger logic omitted for brevity
                }
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            IconButton(
                                onClick = { showYouTubeAccountDialog = false },
                                colors = IconButtonDefaults.iconButtonColors().copy(contentColor = Color.White),
                                modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight(),
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = Color.White)
                            }
                            Text(
                                stringResource(Res.string.youtube_account),
                                style = typo().titleMedium,
                                modifier = Modifier.align(Alignment.Center)
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
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                )
                            }
                        } else {
                            items(data!!) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp).clickable { viewModel.setUsedAccount(it) },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(Modifier.width(24.dp))
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalPlatformContext.current)
                                            .data(it.thumbnailUrl)
                                            .crossfade(550)
                                            .build(),
                                        placeholder = painterResource(Res.drawable.baseline_people_alt_24),
                                        error = painterResource(Res.drawable.baseline_people_alt_24),
                                        contentDescription = it.name,
                                        modifier = Modifier.size(48.dp).clip(CircleShape),
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
                            CenterLoadingBox(Modifier.fillMaxWidth().height(80.dp))
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
                                            confirm = getString(Res.string.log_out) to {
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
}
