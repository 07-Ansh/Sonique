package com.sonique.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Custom Canvas-drawn slider track.
 * Matches Metrolist PlayerSlider.kt exactly:
 *  - 10.dp track height, StrokeCap.Round on both active and inactive lines
 *  - No thumb (caller passes thumb = { Spacer(size(0.dp)) })
 *  - Active portion drawn on top of inactive full-width line
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(),
    trackHeight: Dp = 10.dp,
) {
    val inactiveTrackColor = colors.inactiveTrackColor
    val activeTrackColor = colors.activeTrackColor
    val inactiveTickColor = colors.inactiveTickColor
    val activeTickColor = colors.activeTickColor
    val valueRange = sliderState.valueRange
    Canvas(
        modifier
            .fillMaxWidth()
            .height(trackHeight),
    ) {
        drawTrack(
            tickFractions = stepsToTickFractions(sliderState.steps),
            activeRangeStart = 0f,
            activeRangeEnd = calcFraction(
                a = valueRange.start,
                b = valueRange.endInclusive,
                pos = sliderState.value.coerceIn(valueRange.start, valueRange.endInclusive),
            ),
            inactiveTrackColor = inactiveTrackColor,
            activeTrackColor = activeTrackColor,
            inactiveTickColor = inactiveTickColor,
            activeTickColor = activeTickColor,
            trackHeight = trackHeight,
        )
    }
}

private fun DrawScope.drawTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeEnd: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    inactiveTickColor: Color,
    activeTickColor: Color,
    trackHeight: Dp = 2.dp,
) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val sliderLeft = Offset(0f, center.y)
    val sliderRight = Offset(size.width, center.y)
    val sliderStart = if (isRtl) sliderRight else sliderLeft
    val sliderEnd = if (isRtl) sliderLeft else sliderRight
    val tickSize = 2.0.dp.toPx()
    val trackStrokeWidth = trackHeight.toPx()

    // Full inactive track
    drawLine(
        color = inactiveTrackColor,
        start = sliderStart,
        end = sliderEnd,
        strokeWidth = trackStrokeWidth,
        cap = StrokeCap.Round,
    )

    // Active filled portion
    val sliderValueEnd = Offset(
        x = sliderStart.x + (sliderEnd.x - sliderStart.x) * activeRangeEnd,
        y = center.y,
    )
    val sliderValueStart = Offset(
        x = sliderStart.x + (sliderEnd.x - sliderStart.x) * activeRangeStart,
        y = center.y,
    )
    drawLine(
        color = activeTrackColor,
        start = sliderValueStart,
        end = sliderValueEnd,
        strokeWidth = trackStrokeWidth,
        cap = StrokeCap.Round,
    )

    // Tick marks
    for (tick in tickFractions) {
        val outsideFraction = tick > activeRangeEnd || tick < activeRangeStart
        drawCircle(
            color = if (outsideFraction) inactiveTickColor else activeTickColor,
            center = Offset(lerp(sliderStart, sliderEnd, tick).x, center.y),
            radius = tickSize / 2f,
        )
    }
}

private fun stepsToTickFractions(steps: Int): FloatArray =
    if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }

private fun calcFraction(a: Float, b: Float, pos: Float): Float =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)
