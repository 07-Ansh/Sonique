package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.liquidGlass
import com.sonique.app.utils.VersionManager
import com.sonique.app.viewModel.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.about_us
import sonique.composeapp.generated.resources.app_icon
import sonique.composeapp.generated.resources.baseline_info_24
import sonique.composeapp.generated.resources.baseline_music_note_24
import sonique.composeapp.generated.resources.buy_ansh_a_coffee
import sonique.composeapp.generated.resources.community_and_info
import sonique.composeapp.generated.resources.credits_lead_developer
import sonique.composeapp.generated.resources.credits_license_desc
import sonique.composeapp.generated.resources.credits_license_name
import sonique.composeapp.generated.resources.credits_source_code
import sonique.composeapp.generated.resources.credits_view_repo
import sonique.composeapp.generated.resources.baseline_arrow_outward_24
import sonique.composeapp.generated.resources.metro_link
import sonique.composeapp.generated.resources.wanna_play_favorite_song
import sonique.composeapp.generated.resources.yeah

// ── Data models ───────────────────────────────────────────────────────────────

private data class Contributor(
    val name: String,
    val roleRes: StringResource,
    val githubHandle: String,
    val avatarUrl: String = "https://github.com/$githubHandle.png",
    val githubUrl: String = "https://github.com/$githubHandle",
    val sponsorUrl: String? = null,
    val favoriteSongVideoId: String? = null,
)

private data class CommunityLink(
    val labelRes: StringResource,
    val descriptionRes: StringResource? = null,
    val iconRes: DrawableResource,
    val url: String,
)

private val leadDeveloper = Contributor(
    name = "Ansh Sharma",
    roleRes = Res.string.credits_lead_developer,
    githubHandle = "07-Ansh",
    favoriteSongVideoId = "dQw4w9WgXcQ",
)

private val communityLinks = listOf(
    CommunityLink(
        labelRes = Res.string.credits_view_repo,
        iconRes = Res.drawable.baseline_arrow_outward_24,
        url = "https://github.com/07-Ansh/Sonique",
    ),
    CommunityLink(
        labelRes = Res.string.credits_source_code,
        iconRes = Res.drawable.metro_link,
        url = "https://github.com/07-Ansh/Sonique",
    ),
    CommunityLink(
        labelRes = Res.string.credits_license_name,
        descriptionRes = Res.string.credits_license_desc,
        iconRes = Res.drawable.baseline_info_24,
        url = "https://github.com/07-Ansh/Sonique/blob/main/LICENSE",
    ),
)

// ── Easter egg ────────────────────────────────────────────────────────────────

private fun handleEasterEggClick(
    clickCount: Int,
    favoriteSongVideoId: String?,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    sharedViewModel: SharedViewModel,
    wannaPlayStr: String,
    yeahStr: String,
    onCountUpdate: (Int) -> Unit,
) {
    if (favoriteSongVideoId != null) {
        val newCount = clickCount + 1
        onCountUpdate(newCount)
        if (newCount >= 3) {
            onCountUpdate(0)
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = wannaPlayStr,
                    actionLabel = yeahStr,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    sharedViewModel.loadSharedMediaItem(favoriteSongVideoId)
                }
            }
        }
    }
}

// ── Avatar ────────────────────────────────────────────────────────────────────

@Composable
private fun ContributorAvatar(
    avatarUrl: String,
    sizeDp: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val fallback = painterResource(Res.drawable.app_icon)
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier.size(sizeDp.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp,
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = fallback,
            fallback = fallback,
            error = fallback,
        )
    }
}

// ── Developer socials ─────────────────────────────────────────────────────────

@Composable
private fun DeveloperSocials(
    githubHandle: String,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilledTonalButton(
            onClick = { uriHandler.openUri("https://github.com/$githubHandle") },
            modifier = Modifier.weight(1f).height(48.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.baseline_arrow_outward_24),
                contentDescription = "GitHub",
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("GitHub", style = MaterialTheme.typography.labelMedium)
        }
        FilledTonalButton(
            onClick = { uriHandler.openUri("https://github.com/$githubHandle/Sonique") },
            modifier = Modifier.weight(1f).height(48.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.metro_link),
                contentDescription = "Repo",
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Repo", style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ── Settings group ────────────────────────────────────────────────────────────

@Composable
private fun AboutSettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            content()
        }
    }
}

@Composable
private fun AboutSettingsItem(
    iconRes: DrawableResource,
    title: String,
    description: String? = null,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (description != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAboutScreen(
    navController: NavController,
    onBack: () -> Unit,
) {
    val sharedViewModel: SharedViewModel = koinInject()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val wannaPlayStr = stringResource(Res.string.wanna_play_favorite_song)
    val yeahStr = stringResource(Res.string.yeah)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.about_us),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(
                                if (enableLiquidGlass) {
                                    Modifier.liquidGlass(backdrop, shape = CircleShape, interactive = true)
                                } else Modifier,
                            ),
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // ── App Header Card ───────────────────────────────────────────
            ElevatedCard(
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_icon),
                        contentDescription = "Sonique",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )

                    Spacer(Modifier.width(20.dp))

                    Column {
                        Text(
                            text = "Sonique",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Version pill badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = "v${VersionManager.getVersionName()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                            // Build badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            ) {
                                Text(
                                    text = "Build ${VersionManager.getVersionCode()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Lead Developer Hero Card ──────────────────────────────────
            ElevatedCard(
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        var leadClickCount by remember(leadDeveloper.name) { mutableIntStateOf(0) }

                        ContributorAvatar(
                            avatarUrl = leadDeveloper.avatarUrl,
                            sizeDp = 110,
                            contentDescription = leadDeveloper.name,
                            onClick = {
                                handleEasterEggClick(
                                    clickCount = leadClickCount,
                                    favoriteSongVideoId = leadDeveloper.favoriteSongVideoId,
                                    coroutineScope = coroutineScope,
                                    snackbarHostState = snackbarHostState,
                                    sharedViewModel = sharedViewModel,
                                    wannaPlayStr = wannaPlayStr,
                                    yeahStr = yeahStr,
                                    onCountUpdate = { leadClickCount = it },
                                )
                            },
                        )

                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = leadDeveloper.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 34.sp,
                                letterSpacing = (-0.5).sp,
                            )
                            Text(
                                text = stringResource(leadDeveloper.roleRes),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    DeveloperSocials(
                        githubHandle = leadDeveloper.githubHandle,
                        uriHandler = uriHandler,
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { uriHandler.openUri("https://buymeacoffee.com/07ansh") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.baseline_music_note_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(Res.string.buy_ansh_a_coffee),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Community & Info Group ────────────────────────────────────
            AboutSettingsGroup(title = stringResource(Res.string.community_and_info)) {
                communityLinks.forEachIndexed { index, link ->
                    AboutSettingsItem(
                        iconRes = link.iconRes,
                        title = stringResource(link.labelRes),
                        description = link.descriptionRes?.let { stringResource(it) },
                        onClick = { uriHandler.openUri(link.url) },
                        showDivider = index < communityLinks.size - 1,
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // ── Footer ────────────────────────────────────────────────────
            Text(
                text = "Made with ❤️ and Kotlin",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "© 2025 Sonique",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            )

            Spacer(Modifier.height(48.dp))
        }
    }
}
