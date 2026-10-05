package com.sonique.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sonique.app.utils.VersionManager
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.app_icon_circle
import sonique.composeapp.generated.resources.baseline_album_24
import sonique.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import sonique.composeapp.generated.resources.baseline_lyrics_24
import sonique.composeapp.generated.resources.baseline_queue_music_24
import sonique.composeapp.generated.resources.baseline_tips_and_updates_24
import sonique.composeapp.generated.resources.metro_check
import sonique.composeapp.generated.resources.metro_music_note
import sonique.composeapp.generated.resources.metro_palette
import sonique.composeapp.generated.resources.metro_security
import sonique.composeapp.generated.resources.metro_speed
import sonique.composeapp.generated.resources.metro_tune
import sonique.composeapp.generated.resources.whats_new

private enum class FeatureAccent {
    PRIMARY,
    SECONDARY,
    TERTIARY
}

private data class WhatsNewFeature(
    val title: String,
    val description: String,
    val iconRes: DrawableResource,
    val accent: FeatureAccent,
)

private fun parseHighlights(changelog: String): List<WhatsNewFeature> {
    if (changelog.isBlank()) return emptyList()

    val lines = changelog.lines().map { it.trim() }

    fun isDivider(line: String): Boolean {
        val s = line.trim()
        return s.matches(Regex("""^[-*_]{3,}\s*$""")) || s == "---" || s == "===" || s == "***"
    }

    val bulletLines = lines.filter { line ->
        (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) &&
            !isDivider(line)
    }.map { line ->
        line.removePrefix("-").removePrefix("•").removePrefix("*").trim()
    }.filter { it.isNotBlank() && it.any { c -> c.isLetterOrDigit() } }

    val sourceItems = if (bulletLines.isNotEmpty()) {
        bulletLines
    } else {
        lines.filter { line ->
            line.isNotBlank() &&
                !line.startsWith("#") &&
                !isDivider(line) &&
                !line.startsWith("http", ignoreCase = true) &&
                line.any { c -> c.isLetterOrDigit() }
        }
    }

    return sourceItems.take(5).map { item ->
        val titleMatch = Regex("""\*\*(.*?)\*\*[:\s]*(.*)""").find(item)
        val title: String
        val description: String

        if (titleMatch != null) {
            title = titleMatch.groupValues[1].trim()
            description = titleMatch.groupValues[2].trim()
        } else if (item.contains(": ")) {
            val parts = item.split(": ", limit = 2)
            title = parts[0].replace("**", "").trim()
            description = parts[1].trim()
        } else {
            val words = item.split(" ")
            if (words.size > 4) {
                title = words.take(3).joinToString(" ")
                description = words.drop(3).joinToString(" ")
            } else {
                title = item
                description = ""
            }
        }

        val lower = (title + " " + description).lowercase()
        val (icon, accent) = when {
            // Artwork & Album display (placed before playback so 'playback' doesn't hijack artwork scaling)
            lower.contains("artwork") || lower.contains("cover") || lower.contains("album art") ||
                lower.contains("scaling") || lower.contains("poster") || lower.contains("label art") ->
                Pair(Res.drawable.baseline_album_24, FeatureAccent.TERTIARY)

            // Dynamic Palette, Themes & Colors (theming, theme, palette, contrast, hues)
            lower.contains("color") || lower.contains("theming") || lower.contains("theme") ||
                lower.contains("palette") || lower.contains("hues") || lower.contains("accent") ||
                lower.contains("contrast") || lower.contains("vibrant") || lower.contains("ui") ->
                Pair(Res.drawable.metro_palette, FeatureAccent.PRIMARY)

            // Queue Reordering, Drag & Drop, Swiping
            lower.contains("queue") || lower.contains("reorder") || lower.contains("drag") ||
                lower.contains("drop") || lower.contains("auto-scroll") || lower.contains("swipe") ||
                lower.contains("glide") || lower.contains("sort") || lower.contains("move") ->
                Pair(Res.drawable.baseline_queue_music_24, FeatureAccent.SECONDARY)

            // Buttons, Controls, Sliders, Headers & Dismiss
            lower.contains("button") || lower.contains("slider") || lower.contains("controls") ||
                lower.contains("sheet") || lower.contains("header") || lower.contains("dismiss") ||
                lower.contains("tile") || lower.contains("options") || lower.contains("dock") ->
                Pair(Res.drawable.metro_tune, FeatureAccent.SECONDARY)

            // Lyrics
            lower.contains("lyrics") || lower.contains("synced") || lower.contains("romanization") ||
                lower.contains("words") ->
                Pair(Res.drawable.baseline_lyrics_24, FeatureAccent.PRIMARY)

            // Audio & Playback Engine
            lower.contains("audio") || lower.contains("sound") || lower.contains("equalizer") ||
                lower.contains("flac") || lower.contains("music") || lower.contains("song") ->
                Pair(Res.drawable.metro_music_note, FeatureAccent.PRIMARY)

            // Speed, Latency, Buffer, Instant Performance
            lower.contains("latency") || lower.contains("buffer") || lower.contains("instant") ||
                lower.contains("sub-second") || lower.contains("speed") || lower.contains("fast") ||
                lower.contains("snappier") || lower.contains("battery") || lower.contains("cpu") ->
                Pair(Res.drawable.metro_speed, FeatureAccent.TERTIARY)

            // Security & Stability & Hardening
            lower.contains("security") || lower.contains("stability") || lower.contains("hardening") ||
                lower.contains("protocol") || lower.contains("reliable") || lower.contains("fix") ->
                Pair(Res.drawable.metro_security, FeatureAccent.SECONDARY)

            // Navigation & Liquid Glass
            lower.contains("glass") || lower.contains("navigation") || lower.contains("chevron") ||
                lower.contains("transition") ->
                Pair(Res.drawable.baseline_arrow_back_ios_new_24, FeatureAccent.PRIMARY)

            // General Feature Fallback
            else ->
                Pair(Res.drawable.baseline_tips_and_updates_24, FeatureAccent.PRIMARY)
        }

        WhatsNewFeature(
            title = title,
            description = description,
            iconRes = icon,
            accent = accent,
        )
    }
}

@Composable
fun ChangelogDialog(
    changelog: String,
    versionName: String = "",
    onDismiss: () -> Unit,
    onViewFullChangelog: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glowing App Icon Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_icon_circle),
                        contentDescription = "Sonique App Icon",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Version Badge Pill
                val displayVersion = if (versionName.isNotBlank()) versionName else VersionManager.getVersionName()
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Sonique v$displayVersion",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = stringResource(Res.string.whats_new),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = "Here's what just landed in your music experience",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Feature Highlights List (Scrollable if multiple items)
                val features = remember(changelog) { parseHighlights(changelog) }
                if (features.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 340.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        features.forEach { feature ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (containerColor, contentColor) = when (feature.accent) {
                                        FeatureAccent.PRIMARY -> Pair(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        FeatureAccent.SECONDARY -> Pair(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        FeatureAccent.TERTIARY -> Pair(
                                            MaterialTheme.colorScheme.tertiaryContainer,
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(containerColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(feature.iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = contentColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = feature.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (feature.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = feature.description,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.metro_check),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (changelog.isNotBlank()) changelog.take(200) else "Release notes will appear once published on GitHub.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action: Explore Sonique
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Explore Sonique",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Optional Secondary Action: View Full Release Notes
                if (onViewFullChangelog != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onViewFullChangelog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "View Full Release Notes",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
