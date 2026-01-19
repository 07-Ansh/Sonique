package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.extension.displayString
import com.sonique.app.ui.component.SettingDialog
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.ui.theme.md_theme_dark_primary
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.common.SponsorBlockType
import com.sonique.domain.manager.DataStoreManager
import com.sonique.logger.Logger
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSponsorBlockScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val enableSponsorBlock by viewModel.sponsorBlockEnabled.map { it == DataStoreManager.Values.TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val skipSegments by viewModel.sponsorBlockCategories.collectAsStateWithLifecycle()
    val alertData by viewModel.alertData.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getData()
    }

    if (alertData != null) {
        SettingDialog(
            alert = alertData!!,
            onDismiss = { viewModel.setAlertData(null) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SponsorBlock") },
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
                    title = stringResource(Res.string.enable_sponsor_block),
                    subtitle = stringResource(Res.string.skip_sponsor_part_of_video),
                    switch = (enableSponsorBlock to { viewModel.setSponsorBlockEnabled(it) }),
                )
                val listName = SponsorBlockType.toList().map { it.displayString() }
                SettingItem(
                    title = stringResource(Res.string.categories_sponsor_block),
                    subtitle = stringResource(Res.string.what_segments_will_be_skipped),
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = getString(Res.string.categories_sponsor_block),
                                    multipleSelect = SettingAlertState.SelectData(
                                        listSelect = listName.mapIndexed { index, item ->
                                            (skipSegments?.contains(SponsorBlockType.toList().getOrNull(index)?.value) == true) to item
                                        }.also {
                                            Logger.w("SettingScreen", "SettingAlertState: $skipSegments")
                                            Logger.w("SettingScreen", "SettingAlertState: $it")
                                        },
                                    ),
                                    confirm = getString(Res.string.save) to { state ->
                                        viewModel.setSponsorBlockCategories(
                                            state.multipleSelect?.getListSelected()?.map { selected ->
                                                listName.indexOf(selected)
                                            }?.mapNotNull { s ->
                                                SponsorBlockType.toList().getOrNull(s)?.value
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
}
