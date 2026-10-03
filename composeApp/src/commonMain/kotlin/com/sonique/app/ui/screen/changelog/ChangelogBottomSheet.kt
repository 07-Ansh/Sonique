package com.sonique.app.ui.screen.changelog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.ui.component.GoogleCircularProgressIndicator
import com.sonique.app.viewModel.ChangelogUiState
import com.sonique.app.viewModel.UpdateViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Parses inline markdown (bold **text**, inline `code`, and [links](url))
 * into an AnnotatedString with dynamic Material 3 styling.
 */
fun formatMarkdownText(
    text: String,
    primaryColor: Color,
    onSurfaceColor: Color,
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val pattern = Regex("(\\*\\*(.*?)\\*\\*|`(.*?)`|\\[(.*?)\\]\\((.*?)\\))")
        val matches = pattern.findAll(text)

        for (match in matches) {
            val range = match.range
            if (range.first > cursor) {
                append(text.substring(cursor, range.first))
            }
            when {
                // Bold: **text**
                match.value.startsWith("**") && match.value.endsWith("**") -> {
                    val boldContent = match.groupValues[2]
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                    ) {
                        append(boldContent)
                    }
                }
                // Inline code: `code`
                match.value.startsWith("`") && match.value.endsWith("`") -> {
                    val codeContent = match.groupValues[3]
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor,
                        )
                    ) {
                        append(codeContent)
                    }
                }
                // Markdown link: [text](url)
                match.value.startsWith("[") -> {
                    val linkText = match.groupValues[4]
                    val linkUrl = match.groupValues[5]
                    pushStringAnnotation(tag = "URL", annotation = linkUrl)
                    withStyle(
                        SpanStyle(
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    ) {
                        append(linkText)
                    }
                    pop()
                }
            }
            cursor = range.last + 1
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogBottomSheet(
    onDismissRequest: () -> Unit,
    updateViewModel: UpdateViewModel = koinViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val changelogState by updateViewModel.changelogState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        updateViewModel.loadChangelog()
    }

    // Completely solid and opaque container - zero transparency in all themes
    val sheetBgColor = MaterialTheme.colorScheme.surfaceContainerLowest

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
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

                    items(state.releases, key = { it.version }) { release ->
                        ReleaseSection(release = release)
                    }
                }
                is ChangelogUiState.Error -> {
                    items(state.fallbackReleases, key = { it.version }) { release ->
                        ReleaseSection(release = release)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseSection(
    release: ChangelogRelease,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Version Pill & Release Date Header (Above Card)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Version Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = release.version,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (release.isCurrentVersion) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Installed",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Text(
                text = release.releaseDate,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // What's New Card - Completely solid & opaque with dynamic colors
        val cardShape = RoundedCornerShape(24.dp)
        val cardModifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                BorderStroke(
                    1.dp,
                    if (release.isCurrentVersion) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
                ),
                cardShape
            )

        val cardColors = CardDefaults.cardColors(
            containerColor = if (release.isCurrentVersion) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )

        Card(
            modifier = cardModifier,
            shape = cardShape,
            colors = cardColors,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = release.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )

                val primaryColor = MaterialTheme.colorScheme.primary
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface

                release.highlights.forEachIndexed { index, highlight ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 12.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                        )

                        val formatted = remember(highlight, primaryColor, onSurfaceColor) {
                            formatMarkdownText(
                                text = highlight,
                                primaryColor = primaryColor,
                                onSurfaceColor = onSurfaceColor,
                            )
                        }

                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceColor,
                            lineHeight = 22.sp
                        )
                    }
                }

                if (!release.htmlUrl.isNullOrBlank()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
