package com.sonique.app.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sonique.app.ui.component.SettingsSectionHeader
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.liquidGlass
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
    showYouTubeAccount: Boolean = false,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    val language by viewModel.language.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val sendDataFlow = remember(viewModel.sendBackToGoogle) {
        viewModel.sendBackToGoogle.map { it == TRUE }
    }
    val sendData by sendDataFlow.collectAsStateWithLifecycle(initialValue = false)
    val explicitContentEnabled by viewModel.explicitContentEnabled.collectAsStateWithLifecycle()
    val keepYoutubePlaylistOffline by viewModel.keepYouTubePlaylistOffline.collectAsStateWithLifecycle()
    val showMostPlayed by sharedViewModel.showMostPlayed.collectAsStateWithLifecycle()
    val usingProxy by viewModel.usingProxy.collectAsStateWithLifecycle()
    val proxyType by viewModel.proxyType.collectAsStateWithLifecycle()
    val proxyHost by viewModel.proxyHost.collectAsStateWithLifecycle()
    val proxyPort by viewModel.proxyPort.collectAsStateWithLifecycle()

    var showYouTubeAccountDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val alertData by viewModel.alertData.collectAsStateWithLifecycle()
    val basicAlertData by viewModel.basicAlertData.collectAsStateWithLifecycle()

    alertData?.let { alert ->
        SettingDialog(
            alert = alert,
            onDismiss = { viewModel.setAlertData(null) }
        )
    }
    basicAlertData?.let { alert ->
        SettingBasicDialog(
            alert = alert,
            onDismiss = { viewModel.setBasicAlertData(null) }
        )
    }

    LaunchedEffect(true) {
        viewModel.getData()
        viewModel.getAllGoogleAccount()
    }

    LaunchedEffect(showYouTubeAccount) {
        if (showYouTubeAccount) {
            viewModel.getAllGoogleAccount()
            showYouTubeAccountDialog = true
        }
    }

    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("General", style = MaterialTheme.typography.titleSmall) },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .then(
                            if (enableLiquidGlass) {
                                Modifier.liquidGlass(backdrop, shape = CircleShape, interactive = true)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Back")
                }
            }
        )
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item {
                SettingsSectionHeader("Regional & Account")
            }
            item {
                SettingItem(
                    title = stringResource(Res.string.youtube_account),
                    subtitle = stringResource(Res.string.manage_your_youtube_accounts),
                    onClick = {
                        viewModel.getAllGoogleAccount()
                        showYouTubeAccountDialog = true
                    },
                )
            }
            item {
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
            }
            item {
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
            }

            item {
                SettingsSectionHeader("Content & Sync")
            }
            item {
                SettingItem(
                    title = stringResource(Res.string.send_back_listening_data_to_google),
                    subtitle = stringResource(Res.string.upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in),
                    smallSubtitle = true,
                    switch = (sendData to { viewModel.setSendBackToGoogle(it) }),
                )
            }
            item {
                SettingItem(
                    title = stringResource(Res.string.play_explicit_content),
                    subtitle = stringResource(Res.string.play_explicit_content_description),
                    switch = (explicitContentEnabled to { viewModel.setExplicitContentEnabled(it) }),
                )
            }
            item {
                SettingItem(
                    title = stringResource(Res.string.keep_your_youtube_playlist_offline),
                    subtitle = stringResource(Res.string.keep_your_youtube_playlist_offline_description),
                    switch = (keepYoutubePlaylistOffline to { viewModel.setKeepYouTubePlaylistOffline(it) }),
                )
            }
            item {
                SettingItem(
                    title = "Show Most Played section",
                    subtitle = "Show the most played tracks section on the Library screen.",
                    switch = (showMostPlayed to { sharedViewModel.setShowMostPlayed(it) }),
                )
            }
            item {
                SettingsSectionHeader("Network & Connection")
            }
            item {
                SettingItem(
                    title = stringResource(Res.string.proxy),
                    subtitle = stringResource(Res.string.proxy_description),
                    switch = (usingProxy to { viewModel.setUsingProxy(it) }),
                )
            }
            item {
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
            onDismissRequest = { showYouTubeAccountDialog = false },
            modifier = Modifier.widthIn(min = 360.dp, max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                val googleAccounts by viewModel.googleAccounts.collectAsStateWithLifecycle(
                    minActiveState = Lifecycle.State.RESUMED,
                )
                
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth()
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(Res.string.youtube_account),
                            style = typo().titleLarge,
                            color = white
                        )
                        IconButton(
                            onClick = { showYouTubeAccountDialog = false },
                            colors = IconButtonDefaults.iconButtonColors().copy(contentColor = white),
                        ) {
                            Icon(Icons.Outlined.Close, null)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Accounts List / Active Profile
                    if (googleAccounts is LocalResource.Success) {
                        val data = googleAccounts.data
                        if (data.isNullOrEmpty()) {
                            Text(
                                stringResource(Res.string.no_account),
                                style = typo().bodyMedium,
                                color = white.copy(0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                data.forEach { account ->
                                    val isSelected = account.isUsed
                                    val cardBg = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(0.15f)
                                    } else {
                                        backgroundCard
                                    }
                                    val cardBorderColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(0.5f)
                                    } else {
                                        white.copy(0.08f)
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(cardBg)
                                            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                            .clickable { viewModel.setUsedAccount(account) }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(modifier = Modifier.size(48.dp)) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                                    .data(account.thumbnailUrl)
                                                    .crossfade(550)
                                                    .build(),
                                                placeholder = painterResource(Res.drawable.baseline_people_alt_24),
                                                error = painterResource(Res.drawable.baseline_people_alt_24),
                                                contentDescription = account.name,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .border(1.dp, white.copy(0.15f), CircleShape),
                                            )
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                account.name,
                                                style = typo().labelLarge,
                                                color = white
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                account.email,
                                                style = typo().bodySmall,
                                                color = white.copy(0.6f)
                                            )
                                        }
                                        if (isSelected) {
                                            Spacer(Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(0.2f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    stringResource(Res.string.signed_in),
                                                    style = typo().labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        CenterLoadingBox(Modifier.fillMaxWidth().height(80.dp))
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = white.copy(0.08f))
                    Spacer(Modifier.height(12.dp))
                    
                    // Actions Menu (Guest, Log out, Add account)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Guest Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setUsedAccount(null)
                                    showYouTubeAccountDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.baseline_people_alt_24),
                                contentDescription = null,
                                tint = white.copy(0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                stringResource(Res.string.guest),
                                style = typo().bodyMedium,
                                color = white.copy(0.85f)
                            )
                        }
                        
                        // Add an Account Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showYouTubeAccountDialog = false
                                    navController.navigate(LoginDestination)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.baseline_playlist_add_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                stringResource(Res.string.add_an_account),
                                style = typo().bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Log out Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
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
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.baseline_close_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                stringResource(Res.string.log_out),
                                style = typo().bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
