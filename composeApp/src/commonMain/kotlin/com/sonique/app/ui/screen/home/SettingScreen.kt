package com.sonique.app.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sonique.app.Platform
import com.sonique.app.getPlatform
import com.sonique.app.ui.navigation.destination.home.*
import com.sonique.app.ui.theme.backgroundCard
import com.sonique.app.viewModel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*

@Composable
fun SettingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settingsCategories = rememberSettingsCategories()

    Column(
        modifier = Modifier
            .padding(top = innerPadding.calculateTopPadding())
            .padding(horizontal = 16.dp)
            .fillMaxSize()
    ) {
        // Profile Header could be kept or moved to General. 
        // Plan said "Dashboard", so maybe keep it at top as before.
        // Or maybe inside General? The original code had it at top.
        // Let's keep it here for now as a nice header.
        
        Text(
            text = stringResource(Res.string.settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset(y = (-12).dp)
                .padding(bottom = 16.dp)
        )
        
        LazyColumn(
            contentPadding = PaddingValues(
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(settingsCategories) { category ->
                SettingsCard(
                    title = category.title,
                    icon = category.icon,
                    onClick = { navController.navigate(category.destination) }
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

data class SettingsCategory(
    val title: String,
    val icon: ImageVector,
    val destination: Any
)

@Composable
fun rememberSettingsCategories(): List<SettingsCategory> {
    val categories = mutableListOf(
        SettingsCategory(
            title = stringResource(Res.string.general), // Mapping "Google" and general stuff here
            icon = Icons.Default.Settings,
            destination = SettingsGeneralDestination
        ),
        SettingsCategory(
            title = stringResource(Res.string.audio),
            icon = Icons.Default.Audiotrack,
            destination = SettingsAudioDestination
        ),
        SettingsCategory(
            title = "Video", // No string resource for just "Video" maybe? Use string literal for now or find closest.
            icon = Icons.Default.Videocam,
            destination = SettingsVideoDestination
        ),
        SettingsCategory(
            title = stringResource(Res.string.playback),
            icon = Icons.Default.PlayCircle,
            destination = SettingsPlaybackDestination
        ),
        SettingsCategory(
            title = stringResource(Res.string.spotify),
            icon = Icons.Default.MusicNote,
            destination = SettingsSpotifyDestination
        ),
        SettingsCategory(
            title = stringResource(Res.string.sponsorBlock),
            icon = Icons.Default.Block,
            destination = SettingsSponsorBlockDestination
        ),
        SettingsCategory(
            title = stringResource(Res.string.backup),
            icon = Icons.Default.Backup,
            destination = SettingsBackupDestination
        ),
        SettingsCategory(
            title = stringResource(Res.string.about_us),
            icon = Icons.Default.Info,
            destination = SettingsAboutDestination
        )
    )
    
    if (getPlatform() == Platform.Android) {
        categories.add(
            6, // Insert storage before backup or after? Order doesn't matter much.
            SettingsCategory(
                title = stringResource(Res.string.storage),
                icon = Icons.Default.Storage,
                destination = SettingsStorageDestination
            )
        )
    }
    
    return categories
}
