package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudSync
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
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sonique.app.ui.component.CenterLoadingBox
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.ui.navigation.destination.login.LoginDestination
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GeneralSettingsContent(
    viewModel: SettingsViewModel,
    sharedViewModel: SharedViewModel,
    navController: NavController,
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
    val useAITranslation by viewModel.useAITranslation.collectAsStateWithLifecycle()
    val aiProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val aiApiKey by viewModel.aiApiKey.collectAsStateWithLifecycle()
    val customModelId by viewModel.customModelId.collectAsStateWithLifecycle()
    val customOpenAIBaseUrl by viewModel.customOpenAIBaseUrl.collectAsStateWithLifecycle()
    val customOpenAIHeaders by viewModel.customOpenAIHeaders.collectAsStateWithLifecycle()
    val aiConnectionStatus by viewModel.aiConnectionStatus.collectAsStateWithLifecycle()

    var showYouTubeAccountDialog by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.getAllGoogleAccount()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val langIndex = SUPPORTED_LANGUAGE.codes.indexOf(language)
                val langName = if (langIndex != -1) SUPPORTED_LANGUAGE.items[langIndex].toString() else ""

                Material3SettingsGroup(
                    title = "Regional & Content",
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.youtube_account)) },
                            description = { Text(stringResource(Res.string.manage_your_youtube_accounts)) },
                            onClick = {
                                viewModel.getAllGoogleAccount()
                                showYouTubeAccountDialog = true
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.language)) },
                            description = { Text(langName) },
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setAlertData(
                                        SettingAlertState(
                                            title = getString(Res.string.language),
                                            selectOne = SettingAlertState.SelectData(
                                                listSelect = SUPPORTED_LANGUAGE.codes.mapIndexed { index, code ->
                                                    (code == language) to SUPPORTED_LANGUAGE.items[index].toString()
                                                },
                                            ),
                                            confirm = getString(Res.string.change) to { state ->
                                                val selectedName = state.selectOne?.getSelected()
                                                val index = SUPPORTED_LANGUAGE.items.indexOfFirst { it.toString() == selectedName }
                                                if (index != -1) {
                                                    val code = SUPPORTED_LANGUAGE.codes[index]
                                                    viewModel.changeLanguage(code)
                                                }
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        )
                                    )
                                }
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.content_country)) },
                            description = { Text(location ?: "") },
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
                                                val selectedName = state.selectOne?.getSelected()
                                                if (selectedName != null) {
                                                    viewModel.changeLocation(selectedName)
                                                }
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        )
                                    )
                                }
                            }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = "Playback Preferences",
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.play_explicit_content)) },
                            description = { Text(stringResource(Res.string.play_explicit_content_description)) },
                            isSwitch = true,
                            checked = explicitContentEnabled,
                            onCheckedChange = { viewModel.setExplicitContentEnabled(it) }
                        ),
                        Material3SettingsItem(
                            title = { Text("Offline Playlist Cache") },
                            description = { Text("Keep your YouTube playlists synchronized offline") },
                            isSwitch = true,
                            checked = keepYoutubePlaylistOffline,
                            onCheckedChange = { viewModel.setKeepYouTubePlaylistOffline(it) }
                        ),
                        Material3SettingsItem(
                            title = { Text("Show Most Played") },
                            description = { Text("Show most played tracks under library suggestions") },
                            isSwitch = true,
                            checked = showMostPlayed,
                            onCheckedChange = { sharedViewModel.setShowMostPlayed(it) }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.send_back_listening_data_to_google)) },
                            description = { Text(stringResource(Res.string.upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in)) },
                            isSwitch = true,
                            checked = sendData,
                            onCheckedChange = { viewModel.setSendBackToGoogle(it) }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(Res.string.ai_translation),
                    items = buildList {
                        add(
                            Material3SettingsItem(
                                title = { Text(stringResource(Res.string.ai_translation_enable)) },
                                description = { Text("Translate lyrics using an AI language model") },
                                isSwitch = true,
                                checked = useAITranslation,
                                onCheckedChange = { viewModel.setUseAITranslation(it) }
                            )
                        )
                        if (useAITranslation) {
                            add(
                                Material3SettingsItem(
                                    title = { Text(stringResource(Res.string.ai_provider)) },
                                    description = {
                                        val label = when (aiProvider) {
                                            DataStoreManager.AI_PROVIDER_GEMINI -> stringResource(Res.string.ai_provider_gemini)
                                            DataStoreManager.AI_PROVIDER_OPENAI -> stringResource(Res.string.ai_provider_openai)
                                            else -> stringResource(Res.string.ai_provider_custom)
                                        }
                                        Text(label)
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            val labelGemini = getString(Res.string.ai_provider_gemini)
                                            val labelOpenAI = getString(Res.string.ai_provider_openai)
                                            val labelCustom = getString(Res.string.ai_provider_custom)
                                            viewModel.setAlertData(
                                                SettingAlertState(
                                                    title = getString(Res.string.ai_provider),
                                                    selectOne = SettingAlertState.SelectData(
                                                        listSelect = listOf(
                                                            (aiProvider == DataStoreManager.AI_PROVIDER_GEMINI) to labelGemini,
                                                            (aiProvider == DataStoreManager.AI_PROVIDER_OPENAI) to labelOpenAI,
                                                            (aiProvider == DataStoreManager.AI_PROVIDER_CUSTOM_OPENAI) to labelCustom,
                                                        )
                                                    ),
                                                    confirm = getString(Res.string.change) to { state ->
                                                        val sel = state.selectOne?.getSelected()
                                                        val provider = when (sel) {
                                                            labelOpenAI -> DataStoreManager.AI_PROVIDER_OPENAI
                                                            labelCustom -> DataStoreManager.AI_PROVIDER_CUSTOM_OPENAI
                                                            else -> DataStoreManager.AI_PROVIDER_GEMINI
                                                        }
                                                        viewModel.setAIProvider(provider)
                                                    },
                                                    dismiss = getString(Res.string.cancel),
                                                )
                                            )
                                        }
                                    }
                                )
                            )
                            add(
                                Material3SettingsItem(
                                    title = { Text(stringResource(Res.string.ai_api_key)) },
                                    description = {
                                        val masked = if (aiApiKey.isBlank()) stringResource(Res.string.ai_api_key_placeholder)
                                        else "•".repeat(minOf(aiApiKey.length, 12))
                                        Text(masked)
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.setAlertData(
                                                SettingAlertState(
                                                    title = getString(Res.string.ai_api_key),
                                                    textField = SettingAlertState.TextFieldData(
                                                        label = getString(Res.string.ai_api_key),
                                                        value = aiApiKey,
                                                    ),
                                                    confirm = getString(Res.string.change) to { state ->
                                                        val newKey = state.textField?.value ?: ""
                                                        viewModel.setAIApiKey(newKey)
                                                    },
                                                    dismiss = getString(Res.string.cancel),
                                                )
                                            )
                                        }
                                    }
                                )
                            )
                            add(
                                Material3SettingsItem(
                                    title = { Text(stringResource(Res.string.ai_model_id)) },
                                    description = {
                                        Text(customModelId.ifBlank { stringResource(Res.string.ai_model_id_placeholder) })
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.setAlertData(
                                                SettingAlertState(
                                                    title = getString(Res.string.ai_model_id),
                                                    textField = SettingAlertState.TextFieldData(
                                                        label = getString(Res.string.ai_model_id),
                                                        value = customModelId,
                                                    ),
                                                    confirm = getString(Res.string.change) to { state ->
                                                        viewModel.setCustomModelId(state.textField?.value ?: "")
                                                    },
                                                    dismiss = getString(Res.string.cancel),
                                                )
                                            )
                                        }
                                    }
                                )
                            )
                            if (aiProvider == DataStoreManager.AI_PROVIDER_CUSTOM_OPENAI) {
                                add(
                                    Material3SettingsItem(
                                        title = { Text(stringResource(Res.string.ai_custom_base_url)) },
                                        description = {
                                            Text(customOpenAIBaseUrl.ifBlank { stringResource(Res.string.ai_custom_base_url_placeholder) })
                                        },
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.setAlertData(
                                                    SettingAlertState(
                                                        title = getString(Res.string.ai_custom_base_url),
                                                        textField = SettingAlertState.TextFieldData(
                                                            label = getString(Res.string.ai_custom_base_url),
                                                            value = customOpenAIBaseUrl,
                                                        ),
                                                        confirm = getString(Res.string.change) to { state ->
                                                            viewModel.setCustomOpenAIBaseUrl(state.textField?.value ?: "")
                                                        },
                                                        dismiss = getString(Res.string.cancel),
                                                    )
                                                )
                                            }
                                        }
                                    )
                                )
                                add(
                                    Material3SettingsItem(
                                        title = { Text(stringResource(Res.string.ai_custom_headers)) },
                                        description = {
                                            Text(customOpenAIHeaders.ifBlank { stringResource(Res.string.ai_custom_headers_placeholder) })
                                        },
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.setAlertData(
                                                    SettingAlertState(
                                                        title = getString(Res.string.ai_custom_headers),
                                                        textField = SettingAlertState.TextFieldData(
                                                            label = getString(Res.string.ai_custom_headers),
                                                            value = customOpenAIHeaders,
                                                        ),
                                                        confirm = getString(Res.string.change) to { state ->
                                                            viewModel.setCustomOpenAIHeaders(state.textField?.value ?: "")
                                                        },
                                                        dismiss = getString(Res.string.cancel),
                                                    )
                                                )
                                            }
                                        }
                                    )
                                )
                            }
                            add(
                                Material3SettingsItem(
                                    title = { Text("Test Connection") },
                                    description = {
                                        when (val status = aiConnectionStatus) {
                                            is SettingsViewModel.AIConnectionStatus.Idle -> {
                                                Text("Verify API key and model connectivity")
                                            }
                                            is SettingsViewModel.AIConnectionStatus.Testing -> {
                                                Text("Testing API connection...", color = MaterialTheme.colorScheme.primary)
                                            }
                                            is SettingsViewModel.AIConnectionStatus.Success -> {
                                                Text(status.message, color = Color(0xFF4CAF50))
                                            }
                                            is SettingsViewModel.AIConnectionStatus.Error -> {
                                                Text(status.message, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        when (aiConnectionStatus) {
                                            is SettingsViewModel.AIConnectionStatus.Testing -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            is SettingsViewModel.AIConnectionStatus.Success -> {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Success",
                                                    tint = Color(0xFF4CAF50),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            is SettingsViewModel.AIConnectionStatus.Error -> {
                                                Icon(
                                                    imageVector = Icons.Default.ErrorOutline,
                                                    contentDescription = "Error",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            is SettingsViewModel.AIConnectionStatus.Idle -> {
                                                Icon(
                                                    imageVector = Icons.Outlined.CloudSync,
                                                    contentDescription = "Test",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.testAIConnection()
                                    }
                                )
                            )
                        }
                    }
                )
            }
        }

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
                        if (googleAccounts is LocalResource.Success) {
                            val data = (googleAccounts as LocalResource.Success).data
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
                                                    model = account.thumbnailUrl,
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
}
