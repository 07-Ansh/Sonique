package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.ui.navigation.destination.home.CreditDestination
import com.sonique.app.utils.VersionManager
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAboutScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
}
