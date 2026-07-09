package com.sonique.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

private const val WAVE_SMOOTHNESS = 0.48f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavySliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    activeColor: Color,
    inactiveColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WavySliderTransition")
    val phaseFraction by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "WavySliderPhase"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val pathMeasure = remember { PathMeasure() }
    val cachedWavePath = remember { Path() }
    val displayedWavePath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val fraction = ((sliderState.value - sliderState.valueRange.start) /
                (sliderState.valueRange.endInclusive - sliderState.valueRange.start)).coerceIn(0f, 1f)

        val activeWidth = width * fraction

        // 1. Draw Inactive Track (Straight Line)
        if (fraction < 1f) {
            drawLine(
                color = inactiveColor,
                start = Offset(activeWidth, centerY),
                end = Offset(width, centerY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 2. Draw Active Track (Wavy Line) using Auxio's exact Bezier-based logic
        if (fraction > 0f && width > 0f) {
            val wavelength = 40.dp.toPx()
            val amplitude = 3.5.dp.toPx()

            // Calculate cycle count and adjusted wavelength to fit the track width
            val cycleCount = max(1, (width / wavelength).toInt())
            val adjustedWavelength = width / cycleCount

            // Regenerate cached wave path
            cachedWavePath.reset()
            cachedWavePath.moveTo(0f, centerY + amplitude)
            
            // Generate cycleCount + 1 to handle scroll phase over the edge
            for (i in 0..cycleCount) {
                val cycle = i.toFloat()
                cachedWavePath.cubicTo(
                    x1 = (2 * cycle + WAVE_SMOOTHNESS) * (adjustedWavelength / 2f),
                    y1 = centerY + amplitude,
                    x2 = (2 * cycle + 1 - WAVE_SMOOTHNESS) * (adjustedWavelength / 2f),
                    y2 = centerY - amplitude,
                    x3 = (2 * cycle + 1) * (adjustedWavelength / 2f),
                    y3 = centerY - amplitude
                )
                cachedWavePath.cubicTo(
                    x1 = (2 * cycle + 1 + WAVE_SMOOTHNESS) * (adjustedWavelength / 2f),
                    y1 = centerY - amplitude,
                    x2 = (2 * cycle + 2 - WAVE_SMOOTHNESS) * (adjustedWavelength / 2f),
                    y2 = centerY + amplitude,
                    x3 = (2 * cycle + 2) * (adjustedWavelength / 2f),
                    y3 = centerY + amplitude
                )
            }

            pathMeasure.setPath(cachedWavePath, false)

            if (pathMeasure.length > 0f) {
                // Calculate display segment matching Auxio's exact logic
                var adjustedStart = 0f
                var adjustedEnd = fraction
                var translationX = 0f

                val totalCycleCount = width / adjustedWavelength
                if (totalCycleCount > 0f) {
                    val phaseFractionInPath = phaseFraction / totalCycleCount
                    val ratio = totalCycleCount / (totalCycleCount + 1f)
                    adjustedStart = (adjustedStart + phaseFractionInPath) * ratio
                    adjustedEnd = (adjustedEnd + phaseFractionInPath) * ratio
                    translationX = -phaseFraction * adjustedWavelength
                }

                adjustedStart = adjustedStart.coerceIn(0f, 1f)
                adjustedEnd = adjustedEnd.coerceIn(0f, 1f)

                if (adjustedEnd > adjustedStart) {
                    displayedWavePath.reset()
                    pathMeasure.getSegment(
                        startDistance = adjustedStart * pathMeasure.length,
                        stopDistance = adjustedEnd * pathMeasure.length,
                        destination = displayedWavePath,
                        startWithMoveTo = true
                    )

                    // Draw the segment with translationX applied
                    drawContext.canvas.save()
                    drawContext.canvas.translate(translationX, 0f)
                    drawPath(
                        path = displayedWavePath,
                        color = activeColor,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawContext.canvas.restore()
                }
            }
        }
    }
}
