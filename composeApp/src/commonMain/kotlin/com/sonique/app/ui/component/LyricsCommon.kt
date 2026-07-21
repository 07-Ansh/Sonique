package com.sonique.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonique.domain.data.model.metadata.LyricsEntry
import org.jetbrains.compose.resources.painterResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_close_24
import sonique.composeapp.generated.resources.baseline_share_24
import sonique.composeapp.generated.resources.baseline_sync_24
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

sealed class LyricsListItem {
    data class Line(val index: Int, val entry: LyricsEntry) : LyricsListItem()
    data class Indicator(
        val afterLineIndex: Int,
        val gapMs: Long,
        val gapStartMs: Long,
        val gapEndMs: Long,
        val nextAgent: String?
    ) : LyricsListItem()
}

@Composable
fun CircularWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color,
    trackColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition()
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val strokeWidth = 3.dp.toPx()
        val baseRadius = (size.width / 2) - strokeWidth
        val waveAmplitude = 2.dp.toPx()
        val waveCount = 8

        // Draw track
        drawCircle(
            color = trackColor,
            radius = baseRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // Draw progress arc with wave shape
        val p = progress().coerceIn(0f, 1f)
        if (p > 0f) {
            val progressPath = Path()
            val points = (p * 360).toInt().coerceAtLeast(1)

            for (angleDegrees in 0..points) {
                val angleRad = (angleDegrees - 90) * PI.toFloat() / 180f
                val waveOffset = sin(angleDegrees * waveCount * PI.toFloat() / 180f + wavePhase) * waveAmplitude
                val r = baseRadius + waveOffset
                val x = center.x + r * cos(angleRad)
                val y = center.y + r * sin(angleRad)

                if (angleDegrees == 0) {
                    progressPath.moveTo(x, y)
                } else {
                    progressPath.lineTo(x, y)
                }
            }

            drawPath(
                path = progressPath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
internal fun IntervalIndicator(
    gapStartMs: Long,
    gapEndMs: Long,
    currentPositionMs: Long,
    visible: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    val rowHeightPx = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            rowHeightPx.animateTo(1f, tween(200))
            alpha.animateTo(1f, tween(200))
        } else {
            alpha.animateTo(0f, tween(200))
            rowHeightPx.animateTo(0f, tween(200))
        }
    }

    val targetHeightDp = 72.dp

    val progress = if (gapEndMs > gapStartMs) {
        ((currentPositionMs - gapStartMs).toFloat() / (gapEndMs - gapStartMs).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "intervalProgress"
    )

    Box(
        modifier = modifier
            .height(targetHeightDp * rowHeightPx.value)
            .padding(top = 16.dp * rowHeightPx.value)
            .graphicsLayer {
                this.alpha = alpha.value
                this.clip = true
            },
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .size(36.dp)
                .alpha(alpha.value),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

// ── Fading edge modifier ──────────────────────────────────────────────────────

/**
 * Applies a gradient fade mask to the top and bottom of the composable,
 * creating a smooth clipping effect for the lyrics scroll list.
 */
fun Modifier.fadingEdge(
    top: Dp = 0.dp,
    bottom: Dp = 0.dp
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val topPx = top.toPx()
        val bottomPx = bottom.toPx()
        if (topPx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = topPx
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (bottomPx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = size.height - bottomPx,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - bottomPx),
                size = Size(size.width, bottomPx),
                blendMode = BlendMode.DstIn
            )
        }
    }

// ── LyricsActionOverlay ───────────────────────────────────────────────────────

/**
 * Floating overlay at the bottom of the lyrics area. Shows:
 *  - An "Auto-scroll" re-engage button when the user has manually scrolled away.
 *  - "Cancel" + "Share" buttons when selection mode is active.
 */
@Composable
internal fun LyricsActionOverlay(
    isAutoScrollEnabled: Boolean,
    isSynced: Boolean,
    isSelectionModeActive: Boolean,
    anySelected: Boolean,
    onSyncClick: () -> Unit,
    onCancelSelection: () -> Unit,
    onShareSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Auto-scroll re-engage button
        AnimatedVisibility(
            visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            FilledTonalButton(onClick = onSyncClick) {
                Icon(
                    painter = painterResource(Res.drawable.baseline_sync_24),
                    contentDescription = "Auto-scroll",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Auto-scroll", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Selection mode — cancel + share
        AnimatedVisibility(
            visible = isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = onCancelSelection) {
                    Icon(
                        painter = painterResource(Res.drawable.baseline_close_24),
                        contentDescription = "Cancel",
                        modifier = Modifier.size(20.dp)
                    )
                }
                FilledTonalButton(
                    onClick = onShareSelection,
                    enabled = anySelected
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.baseline_share_24),
                        contentDescription = "Share",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Share", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
