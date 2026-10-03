package com.sonique.app.ui.screen.changelog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.sonique.app.expect.ui.PlatformBackdrop
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.GoogleCircularProgressIndicator
import com.sonique.app.ui.component.liquidGlass
import com.sonique.app.viewModel.ChangelogUiState
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UpdateViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogBottomSheet(
    onDismissRequest: () -> Unit,
    updateViewModel: UpdateViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val changelogState by updateViewModel.changelogState.collectAsStateWithLifecycle()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    LaunchedEffect(Unit) {
        updateViewModel.loadChangelog()
    }

    val sheetBgColor = if (enableLiquidGlass) {
        Color(0xFF0F0F12).copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = sheetBgColor,
        scrimColor = BottomSheetDefaults.ScrimColor,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = 4.dp,
                bottom = navBarPadding + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Changelog",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    textAlign = TextAlign.Center
                )
            }

            when (val state = changelogState) {
                is ChangelogUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GoogleCircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                is ChangelogUiState.Success -> {
                    if (state.isOffline) {
                        item {
                            Text(
                                text = "Showing cached release history (offline)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    item {
                        ReleasesSectionList(
                            releases = state.releases,
                            enableLiquidGlass = enableLiquidGlass,
                            backdrop = backdrop,
                        )
                    }
                }
                is ChangelogUiState.Error -> {
                    item {
                        ReleasesSectionList(
                            releases = state.fallbackReleases,
                            enableLiquidGlass = enableLiquidGlass,
                            backdrop = backdrop,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleasesSectionList(
    releases: List<ChangelogRelease>,
    enableLiquidGlass: Boolean,
    backdrop: PlatformBackdrop,
) {
    var expandedVersions by rememberSaveable {
        mutableStateOf(setOf<String>())
    }

    // Auto-expand the first / latest release when releases load
    LaunchedEffect(releases) {
        if (expandedVersions.isEmpty() && releases.isNotEmpty()) {
            expandedVersions = setOf(releases.first().version)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        releases.forEach { release ->
            val isExpanded = expandedVersions.contains(release.version)
            ReleaseCard(
                release = release,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedVersions = if (isExpanded) {
                        expandedVersions - release.version
                    } else {
                        expandedVersions + release.version
                    }
                },
                enableLiquidGlass = enableLiquidGlass,
                backdrop = backdrop,
            )
        }
    }
}

@Composable
private fun ReleaseCard(
    release: ChangelogRelease,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    enableLiquidGlass: Boolean,
    backdrop: PlatformBackdrop,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val cardShape = RoundedCornerShape(24.dp)

    val cardModifier = if (enableLiquidGlass) {
        Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                BorderStroke(
                    0.5.dp,
                    if (release.isCurrentVersion) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        Color.White.copy(alpha = 0.10f)
                    }
                ),
                cardShape
            )
            .liquidGlass(backdrop, shape = cardShape, interactive = false)
    } else {
        Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                BorderStroke(
                    1.dp,
                    if (release.isCurrentVersion) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    }
                ),
                cardShape
            )
    }

    val cardColors = CardDefaults.cardColors(
        containerColor = if (enableLiquidGlass) {
            Color.Transparent
        } else if (release.isCurrentVersion) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    )

    Card(
        modifier = modifier.then(cardModifier),
        shape = cardShape,
        colors = cardColors,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Interactive Header (tap anywhere on header to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onToggleExpand
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Version Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (enableLiquidGlass) Color.White.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = release.version,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (enableLiquidGlass) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (release.isCurrentVersion) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (enableLiquidGlass) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.secondaryContainer
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Installed",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (enableLiquidGlass) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = release.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (enableLiquidGlass) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = release.releaseDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Animated Chevron
                val rotationDegrees by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(rotationDegrees)
                    )
                }
            }

            // Expandable Markdown Body Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(spring()) + expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                exit = fadeOut(spring()) + shrinkVertically(spring())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        color = if (enableLiquidGlass) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        },
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val markdownContent = release.body.ifBlank {
                        release.highlights.joinToString("\n") { "- $it" }
                    }

                    Markdown(
                        content = markdownContent,
                        colors = markdownColor(
                            text = if (enableLiquidGlass) Color(0xFFE5E5E5) else MaterialTheme.colorScheme.onSurface,
                            codeBackground = if (enableLiquidGlass) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        typography = markdownTypography(
                            text = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            h1 = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            h2 = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            h3 = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary),
                            bullet = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                            code = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!release.htmlUrl.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { uriHandler.openUri(release.htmlUrl) }
                            ) {
                                Text(
                                    text = "View on GitHub",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
