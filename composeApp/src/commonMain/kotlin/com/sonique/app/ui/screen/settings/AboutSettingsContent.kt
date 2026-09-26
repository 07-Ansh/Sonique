package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.ui.component.liquidGlass
import com.sonique.app.utils.VersionManager
import com.sonique.app.viewModel.SharedViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.*

@Composable
internal fun AboutSettingsContent(navController: NavController) {
    val uriHandler = LocalUriHandler.current
    val sharedViewModel: SharedViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val cookieBlobShape = remember {
        androidx.compose.foundation.shape.GenericShape { size, _ ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.width / 2f
            var first = true
            val points = 360
            for (i in 0..points) {
                val angle = (i * 2.0 * Math.PI / points).toFloat()
                val r = outerR * (0.95f + 0.05f * kotlin.math.cos(12f * angle))
                
                val px = cx + r * kotlin.math.cos(angle)
                val py = cy + r * kotlin.math.sin(angle)
                
                if (first) {
                    moveTo(px, py)
                    first = false
                } else {
                    lineTo(px, py)
                }
            }
            close()
        }
    }

    val devName = "Ansh Sharma"
    val devGitHub = "07-Ansh"
    val devAvatarUrl = "https://github.com/$devGitHub.png"
    val devFavSongVideoId = "dQw4w9WgXcQ"

    val wannaPlay = stringResource(Res.string.wanna_play_favorite_song)
    val yeah = stringResource(Res.string.yeah)

    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    val cardModifier = if (enableLiquidGlass) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(32.dp))
            .liquidGlass(backdrop, shape = RoundedCornerShape(32.dp), interactive = false)
    } else {
        Modifier.fillMaxWidth()
    }

    val cardColors = CardDefaults.elevatedCardColors(
        containerColor = if (enableLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(Modifier.height(16.dp)) }

            item {
                Material3SettingsGroup(
                    items = listOf(
                        Material3SettingsItem(
                            leadingContent = {
                                Image(
                                    painter = painterResource(Res.drawable.app_icon),
                                    contentDescription = "Sonique",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                )
                            },
                            title = {
                                Text(
                                    text = "Sonique",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            description = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    ) {
                                        Text(
                                            text = "v${VersionManager.getVersionName()}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                    ) {
                                        Text(
                                            text = "Open Source",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        )
                    )
                )
            }

            item {
                var leadClickCount by remember { mutableIntStateOf(0) }
                val fallback = painterResource(Res.drawable.app_icon)

                ElevatedCard(
                    shape = RoundedCornerShape(32.dp),
                    modifier = cardModifier,
                    colors = cardColors,
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Surface(
                                onClick = {
                                    val newCount = leadClickCount + 1
                                    leadClickCount = newCount
                                    if (newCount >= 3) {
                                        leadClickCount = 0
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = wannaPlay,
                                                actionLabel = yeah,
                                                duration = SnackbarDuration.Short,
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                sharedViewModel.loadSharedMediaItem(devFavSongVideoId)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(72.dp),
                                shape = cookieBlobShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                tonalElevation = 4.dp,
                            ) {
                                AsyncImage(
                                    model = devAvatarUrl,
                                    contentDescription = devName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    placeholder = fallback,
                                    fallback = fallback,
                                    error = fallback,
                                )
                            }

                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = devName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(Res.string.credits_lead_developer),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            FilledTonalButton(
                                onClick = { uriHandler.openUri("https://github.com/$devGitHub") },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_github),
                                    contentDescription = "GitHub",
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("GitHub", style = MaterialTheme.typography.labelMedium)
                            }
                            FilledTonalButton(
                                onClick = { uriHandler.openUri("https://github.com/$devGitHub/Sonique") },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_github),
                                    contentDescription = "Repo",
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Repo", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { uriHandler.openUri("https://buymeacoffee.com/07ansh") },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.buymeacoffee),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Buy me a coffee",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(Res.string.community_and_info),
                    items = listOf(
                        Material3SettingsItem(
                            iconPainter = painterResource(Res.drawable.ic_github),
                            title = { Text(stringResource(Res.string.credits_source_code)) },
                            onClick = { uriHandler.openUri("https://github.com/07-Ansh/Sonique") }
                        ),
                        Material3SettingsItem(
                            iconPainter = painterResource(Res.drawable.baseline_info_24),
                            title = { Text(stringResource(Res.string.credits_license_name)) },
                            description = { Text(stringResource(Res.string.credits_license_desc)) },
                            onClick = { uriHandler.openUri("https://github.com/07-Ansh/Sonique/blob/main/LICENSE") }
                        )
                    )
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Made with \u2764\uFE0F and Kotlin",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "\u00A9 2025 Sonique",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
internal fun MinimalLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
