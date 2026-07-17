package com.sonique.app.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import com.sonique.app.Platform
import com.sonique.app.getPlatform
import com.sonique.app.ui.navigation.destination.home.*
import com.sonique.app.ui.theme.backgroundCard
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.app.viewModel.SharedViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.*
import com.sonique.app.ui.component.liquidGlass
import com.sonique.app.expect.ui.PlatformBackdrop
import com.sonique.app.expect.ui.rememberBackdrop

@Composable
fun SettingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val categories = rememberSettingsCategories()
    val sharedViewModel: SharedViewModel = koinInject()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    LaunchedEffect(Unit) {
        viewModel.getData()
    }

    Column(
        modifier = Modifier
            .padding(top = innerPadding.calculateTopPadding() + 8.dp)
            .padding(horizontal = 16.dp)
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(Res.string.settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        LazyColumn(
            contentPadding = PaddingValues(
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories) { category ->
                val cardModifier = if (enableLiquidGlass) {
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(28.dp))
                        .liquidGlass(backdrop, shape = RoundedCornerShape(28.dp), interactive = false)
                        .clickable { navController.navigate(category.destination) }
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundCard)
                        .clickable { navController.navigate(category.destination) }
                }

                Box(
                    modifier = cardModifier,
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = if (enableLiquidGlass) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (enableLiquidGlass) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = if (enableLiquidGlass) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
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
            title = "Appearance (New)",
            icon = Icons.Default.Palette,
            destination = SettingsUiDestination
        ),
        SettingsCategory(
            title = stringResource(Res.string.general),
            icon = Icons.Default.Settings,
            destination = SettingsGeneralDestination()
        ),
        SettingsCategory(
            title = "App Updates",
            icon = Icons.Default.SystemUpdate,
            destination = SettingsUpdateDestination
        ),
        SettingsCategory(
            title = stringResource(Res.string.audio),
            icon = Icons.Default.Audiotrack,
            destination = SettingsAudioDestination
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
            SettingsCategory(
                title = stringResource(Res.string.storage),
                icon = Icons.Default.Storage,
                destination = SettingsStorageDestination
            )
        )
    }

    return categories
}
