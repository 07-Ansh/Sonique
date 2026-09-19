package com.sonique.app.extension

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.kmpalette.palette.graphics.Palette
import com.sonique.domain.data.model.ui.ScreenSizeInfo
import com.sonique.logger.Logger
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.shimmerBackground
import com.sonique.app.ui.theme.shimmerLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

fun generateRandomColor(): Color {
    val red = Random.nextInt(256)
    val green = Random.nextInt(256)
    val blue = Random.nextInt(256)
    return Color(red, green, blue)
}

fun Modifier.shimmer(): Modifier =
    composed {
        var size by remember {
            mutableStateOf(IntSize.Zero)
        }
        val transition = rememberInfiniteTransition(label = "Shimmer")
        val startOffsetX by transition.animateFloat(
            initialValue = -2 * size.width.toFloat(),
            targetValue = 2 * size.width.toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000),
                ),
            label = "Shimmer",
        )

        val sharedViewModel: com.sonique.app.viewModel.SharedViewModel = org.koin.compose.koinInject()
        val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsState(false)
        val shimmerBg = if (enableLiquidGlass && com.sonique.app.getPlatform() == com.sonique.app.Platform.Android) Color(0x15FFFFFF) else shimmerBackground
        val shimmerLn = if (enableLiquidGlass && com.sonique.app.getPlatform() == com.sonique.app.Platform.Android) Color(0x35FFFFFF) else shimmerLine

        background(
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            shimmerBg,
                            shimmerLn,
                            shimmerBg,
                        ),
                    start = Offset(startOffsetX, 0f),
                    end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat()),
                ),
        ).onGloballyPositioned {
            size = it.size
        }
    }

class GreyScaleModifier : DrawModifier {
    override fun ContentDrawScope.draw() {
        val saturationMatrix = ColorMatrix().apply { setToSaturation(0f) }
        val saturationFilter = ColorFilter.colorMatrix(saturationMatrix)
        val paint =
            Paint().apply {
                colorFilter = saturationFilter
            }
        drawIntoCanvas {
            it.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
            drawContent()
            it.restore()
        }
    }
}

fun Modifier.greyScale() = this.then(GreyScaleModifier())

fun Modifier.angledGradientBackground(
    colors: List<Color>,
    degrees: Float,
) = this.then(
    if (colors.size < 2) {
        Modifier
    } else {
        Modifier.drawBehind {
             

            val (x, y) = size
            val gamma = atan2(y, x)

            if (gamma == 0f || gamma == (PI / 2).toFloat()) {
                 
                return@drawBehind
            }

            val degreesNormalised = (degrees % 360).let { if (it < 0) it + 360 else it }

            val alpha = (degreesNormalised * PI / 180).toFloat()

            val gradientLength =
                when (alpha) {
                     
                    in 0f..gamma, in (2 * PI - gamma)..2 * PI -> {
                        x / cos(alpha)
                    }
                     
                    in gamma..(PI - gamma).toFloat() -> {
                        y / sin(alpha)
                    }
                     
                    in (PI - gamma)..(PI + gamma) -> {
                        x / -cos(alpha)
                    }
                     
                    in (PI + gamma)..(2 * PI - gamma) -> {
                        y / -sin(alpha)
                    }
                     
                    else -> hypot(x, y)
                }

            val centerOffsetX = cos(alpha) * gradientLength / 2
            val centerOffsetY = sin(alpha) * gradientLength / 2

            drawRect(
                brush =
                    Brush.linearGradient(
                        colors = colors,
                         
                        start = Offset(center.x - centerOffsetX, center.y - centerOffsetY),
                        end = Offset(center.x + centerOffsetX, center.y + centerOffsetY),
                    ),
                size = size,
            )
        }
    },
)

 
fun GradientOffset(angle: GradientAngle): GradientOffset =
    when (angle) {
        GradientAngle.CW45 ->
            GradientOffset(
                start = Offset.Zero,
                end = Offset.Infinite,
            )

        GradientAngle.CW90 ->
            GradientOffset(
                start = Offset.Zero,
                end = Offset(0f, Float.POSITIVE_INFINITY),
            )

        GradientAngle.CW135 ->
            GradientOffset(
                start = Offset(Float.POSITIVE_INFINITY, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY),
            )

        GradientAngle.CW180 ->
            GradientOffset(
                start = Offset(Float.POSITIVE_INFINITY, 0f),
                end = Offset.Zero,
            )

        GradientAngle.CW225 ->
            GradientOffset(
                start = Offset.Infinite,
                end = Offset.Zero,
            )

        GradientAngle.CW270 ->
            GradientOffset(
                start = Offset(0f, Float.POSITIVE_INFINITY),
                end = Offset.Zero,
            )

        GradientAngle.CW315 ->
            GradientOffset(
                start = Offset(0f, Float.POSITIVE_INFINITY),
                end = Offset(Float.POSITIVE_INFINITY, 0f),
            )

        else ->
            GradientOffset(
                start = Offset.Zero,
                end = Offset(Float.POSITIVE_INFINITY, 0f),
            )
    }

 
data class GradientOffset(
    val start: Offset,
    val end: Offset,
)

enum class GradientAngle {
    CW0,
    CW45,
    CW90,
    CW135,
    CW180,
    CW225,
    CW270,
    CW315,
}

@Composable
expect fun getScreenSizeInfo(): ScreenSizeInfo

@Composable
fun NonLazyGrid(
    columns: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
    content:
        @Composable()
        (Int) -> Unit,
) {
    Column(modifier = modifier) {
        var rows = (itemCount / columns)
        if (itemCount.mod(columns) > 0) {
            rows += 1
        }

        for (rowId in 0 until rows) {
            val firstIndex = rowId * columns

            Row {
                for (columnId in 0 until columns) {
                    val index = firstIndex + columnId
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    ) {
                        if (index < itemCount) {
                            content(index)
                        }
                    }
                }
            }
        }
    }
}

fun LazyListState.animateScrollAndCentralizeItem(
    index: Int,
    scope: CoroutineScope,
) {
    val itemInfo = this.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    scope.launch {
        if (itemInfo != null) {
            val center = this@animateScrollAndCentralizeItem.layoutInfo.viewportEndOffset / 2
            val childCenter = itemInfo.offset + itemInfo.size / 2
            this@animateScrollAndCentralizeItem.animateScrollBy((childCenter - center / 1.5f).toFloat(), tween(800))
        } else {
            this@animateScrollAndCentralizeItem.animateScrollToItem(index)
        }
    }
}

@Composable
expect fun KeepScreenOn()

@Composable
fun LazyListState.isScrollingUp(thresholdPx: Int = 12): State<Boolean> {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    var scrollingUpState by remember(this) { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        snapshotFlow { layoutInfo.totalItemsCount }.collect {
            previousIndex = firstVisibleItemIndex
            previousScrollOffset = firstVisibleItemScrollOffset
        }
    }

    return remember(this) {
        derivedStateOf {
            if (firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0) {
                if (previousIndex != firstVisibleItemIndex) {
                    scrollingUpState = previousIndex > firstVisibleItemIndex
                    previousIndex = firstVisibleItemIndex
                    previousScrollOffset = firstVisibleItemScrollOffset
                } else {
                    val delta = firstVisibleItemScrollOffset - previousScrollOffset
                    if (kotlin.math.abs(delta) >= thresholdPx) {
                        scrollingUpState = delta <= 0
                        previousScrollOffset = firstVisibleItemScrollOffset
                    }
                }
                scrollingUpState
            } else {
                scrollingUpState = true
                previousIndex = 0
                previousScrollOffset = 0
                true
            }
        }
    }
}

@Composable
fun LazyGridState.isScrollingUp(thresholdPx: Int = 12): State<Boolean> {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    var scrollingUpState by remember(this) { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        snapshotFlow { layoutInfo.totalItemsCount }.collect {
            previousIndex = firstVisibleItemIndex
            previousScrollOffset = firstVisibleItemScrollOffset
        }
    }

    return remember(this) {
        derivedStateOf {
            if (firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0) {
                if (previousIndex != firstVisibleItemIndex) {
                    scrollingUpState = previousIndex > firstVisibleItemIndex
                    previousIndex = firstVisibleItemIndex
                    previousScrollOffset = firstVisibleItemScrollOffset
                } else {
                    val delta = firstVisibleItemScrollOffset - previousScrollOffset
                    if (kotlin.math.abs(delta) >= thresholdPx) {
                        scrollingUpState = delta <= 0
                        previousScrollOffset = firstVisibleItemScrollOffset
                    }
                }
                scrollingUpState
            } else {
                scrollingUpState = true
                previousIndex = 0
                previousScrollOffset = 0
                true
            }
        }
    }
}

fun Palette?.getColorFromPalette(): Color {
    val p = this ?: return md_theme_dark_background
    val defaultColor = 0x000000
    // Only use the darkest swatches — no fallback to Vibrant/Muted (which can be bright)
    val startColor = p.getDarkVibrantColor(defaultColor)
        .takeIf { it != defaultColor }
        ?: p.getDarkMutedColor(defaultColor)
    return if (startColor == defaultColor) {
        md_theme_dark_background
    } else {
        Color(startColor).darkenForAmbience()
    }
}

fun Palette?.getSecondaryColorFromPalette(): Color {
    val p = this ?: return md_theme_dark_background
    val defaultColor = 0x000000
    // Only use the darkest swatches — prefer DarkMuted as a complement to DarkVibrant
    val secondaryColor = p.getDarkMutedColor(defaultColor)
        .takeIf { it != defaultColor }
        ?: p.getDarkVibrantColor(defaultColor)
    return if (secondaryColor == defaultColor) {
        md_theme_dark_background
    } else {
        Color(secondaryColor).darkenForAmbience()
    }
}

/**
 * Converts a Compose Color to HSL representation:
 * [0] = Hue in degrees [0f, 360f)
 * [1] = Saturation [0f, 1f]
 * [2] = Lightness [0f, 1f]
 */
fun Color.toHsl(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, maxOf(g, b))
    val min = minOf(r, minOf(g, b))
    val delta = max - min
    val l = (max + min) / 2f

    val s = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * l - 1f))

    var h = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    if (h < 0f) h += 360f

    return floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

/**
 * Creates a Compose Color from HSL parameters.
 */
fun hslToColor(hue: Float, saturation: Float, lightness: Float, alpha: Float = 1f): Color {
    val h = (hue % 360f + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)

    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f

    val (rPrime, gPrime, bPrime) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (rPrime + m).coerceIn(0f, 1f),
        green = (gPrime + m).coerceIn(0f, 1f),
        blue = (bPrime + m).coerceIn(0f, 1f),
        alpha = alpha
    )
}

/**
 * Extracts a rich, vibrant ambient sheet background color from the album artwork palette.
 * If the artwork is monochrome/grayscale (saturation < 0.12f), returns a neutral dark slate
 * (Color(0xFF141316)) that matches the player screen instead of forcing a fake red/pink tint.
 * For colored artwork, preserves the artwork's actual hue with a subtle, solid dark tint (14% lightness).
 */
fun Palette?.getAmbientSheetColor(): Color {
    val p = this ?: return Color(0xFF141316)
    // Look for swatches with genuine color saturation (>= 0.12f)
    val colorSwatch = p.vibrantSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.12f }
        ?: p.lightVibrantSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.12f }
        ?: p.darkVibrantSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.12f }
        ?: p.dominantSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.12f }
        ?: p.mutedSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.12f }
        ?: p.swatches.filter { Color(it.rgb).toHsl()[1] >= 0.12f }.maxByOrNull { it.population }

    if (colorSwatch != null) {
        val hsl = Color(colorSwatch.rgb).toHsl()
        val hue = hsl[0]
        val saturation = hsl[1].coerceIn(0.12f, 0.38f)
        return hslToColor(hue, saturation, 0.09f, 1f)
    }
    // Grayscale / Black & White artwork: return neutral dark mode background matching player
    return Color(0xFF101214)
}

/**
 * Extracts a lively accent color from the album artwork palette.
 * If the artwork is monochrome/grayscale (saturation < 0.12f), returns clean White (Color.White),
 * matching the player screen's white play/pause pill and timeline slider, rather than pink.
 * For colored artwork, returns a pastel/luminous tint of the exact album hue (76% lightness).
 */
fun Palette?.getAmbientAccentColor(): Color {
    val p = this ?: return Color(0xFF98D2EB)
    // Look for swatches with genuine color saturation (>= 0.12f)
    val colorSwatch = p.vibrantSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.12f }
        ?: p.lightVibrantSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.12f }
        ?: p.dominantSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.12f }
        ?: p.mutedSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.12f }
        ?: p.swatches.filter { Color(it.rgb).toHsl()[1] >= 0.12f }.maxByOrNull { it.population }

    if (colorSwatch != null) {
        val hsl = Color(colorSwatch.rgb).toHsl()
        val hue = hsl[0]
        val saturation = hsl[1].coerceIn(0.40f, 0.75f)
        return hslToColor(hue, saturation, 0.78f, 1f)
    }
    // Grayscale / Black & White artwork: clean White accent matching the player
    return Color.White
}

/**
 * Returns a tinted white for player buttons (Play/Pause, 3-dot, Share, Like, Queue).
 * Stays predominantly white (~90% white) with a subtle, minimal tint (10%) from the album artwork.
 * If the artwork is monochrome/grayscale, returns pure Color.White.
 */
fun Palette?.getAmbientTintedWhiteColor(): Color {
    val p = this ?: return Color.White
    val colorSwatch = p.vibrantSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.10f }
        ?: p.lightVibrantSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.10f }
        ?: p.dominantSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.10f }
        ?: p.mutedSwatch?.takeIf { Color(it.rgb).toHsl()[1] >= 0.10f }
        ?: p.swatches.filter { Color(it.rgb).toHsl()[1] >= 0.10f }.maxByOrNull { it.population }

    if (colorSwatch != null) {
        val hsl = Color(colorSwatch.rgb).toHsl()
        val hue = hsl[0]
        val sat = hsl[1].coerceIn(0.40f, 0.85f)
        val vividBase = hslToColor(hue, sat, 0.50f, 1f)
        return lerp(Color.White, vividBase, 0.10f)
    }
    return Color.White
}

/**
 * Safety clamp: ensures any color used in the ambient gradient stays dark.
 * Since we only pick DarkVibrant/DarkMuted this rarely triggers, but guards
 * against edge cases where even those swatches are surprisingly bright.
 */
fun Color.darkenForAmbience(): Color {
    val maxComponent = maxOf(red, green, blue)
    return if (maxComponent > 0.40f) {
        val scale = 0.35f / maxComponent
        Color(
            red = red * scale,
            green = green * scale,
            blue = blue * scale,
            alpha = alpha,
        )
    } else {
        this
    }
}

fun Modifier.isElementVisible(onVisibilityChanged: (Boolean) -> Unit) =
    composed {
        val isVisible by remember { derivedStateOf { mutableStateOf(false) } }
        LaunchedEffect(isVisible.value) { onVisibilityChanged.invoke(isVisible.value) }
        this.onGloballyPositioned { layoutCoordinates ->
            isVisible.value = layoutCoordinates.parentLayoutCoordinates?.let {
                val parentBounds = it.boundsInWindow()
                val childBounds = layoutCoordinates.boundsInWindow()
                parentBounds.overlaps(childBounds)
            } == true
        }
    }

fun Color.rgbFactor(factor: Float): Color {
    val r = min(red * factor, 255f)
    val g = min(green * factor, 255f)
    val b = min(blue * factor, 255f)
    return Color(r, g, b, alpha)
}

fun TextStyle.greyScale(): TextStyle =
    this.copy(
        color = Color.Gray,
    )

@Composable
expect fun rememberIsInPipMode(): Boolean

@Composable
fun PaddingValues.copy(
    start: Dp? = null,
    top: Dp? = null,
    end: Dp? = null,
    bottom: Dp? = null,
): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = start ?: this.calculateStartPadding(layoutDirection),
        top = top ?: this.calculateTopPadding(),
        end = end ?: this.calculateEndPadding(layoutDirection),
        bottom = bottom ?: this.calculateBottomPadding(),
    )
}

fun ImageBitmap.toResizedBitmap(
    width: Int,
    height: Int,
): ImageBitmap {
    val resized = ImageBitmap(width, height)
    val canvas = Canvas(resized)
    canvas.drawImageRect(
        image = this,
        dstSize = IntSize(width, height),
        paint = Paint(),
    )
    return resized
}

fun getStringBlocking(res: StringResource): String =
    runBlocking {
        getString(res)
    }

fun Palette?.toImmersiveBackground(): Color {
    val p = this ?: return md_theme_dark_background
    val rgb =
        p.getDominantColor(0).takeIf { it != 0 }
            ?: p.getMutedColor(0).takeIf { it != 0 }
            ?: p.getVibrantColor(0).takeIf { it != 0 }
            ?: return md_theme_dark_background
    val base = Color(rgb)
    // Perceived luminance (0 dark .. 1 light) of the source swatch.
    val luminance = 0.299f * base.red + 0.587f * base.green + 0.114f * base.blue
    // Darken more for lighter artwork so the page stays dark enough for white text.
    val darkenFactor = 0.35f + 0.45f * luminance
    return androidx.compose.ui.graphics.lerp(base, md_theme_dark_background, darkenFactor)
}

fun smoothScrimBrush(
    from: Color,
    to: Color,
    startFraction: Float = 0f,
    endFraction: Float = 1f,
    startY: Float = 0f,
    endY: Float = Float.POSITIVE_INFINITY,
    steps: Int = 24,
): Brush =
    Brush.verticalGradient(
        colorStops =
            Array(steps + 1) { i ->
                val t = i / steps.toFloat()
                val position = startFraction + (endFraction - startFraction) * t
                position to androidx.compose.ui.graphics.lerp(from, to, t * t * (3f - 2f * t))
            },
        startY = startY,
        endY = endY,
    )

fun artworkScrimBrush(
    color: Color,
    steps: Int = 24,
): Brush = smoothScrimBrush(from = color.copy(alpha = 0f), to = color, steps = steps)

/**
 * Cleans song titles by removing extra metadata suffixes such as:
 * - Parenthetical text: "Song (From Movie)", "Song (Official Audio)"
 * - Bracketed text: "Song [Remix]", "Song [Official Video]"
 * - Dash suffixes: "Song - Something", "Song – Something", "Song — Something", "Song- Something"
 * - Pipe suffixes: "Song | Something"
 * - Slash suffixes: "Song // Something"
 */
fun String.cleanSongTitle(): String {
    if (isBlank()) return this
    var cleaned = this.trim()

    val parenIndex = cleaned.indexOf('(')
    if (parenIndex > 0) {
        cleaned = cleaned.substring(0, parenIndex)
    }

    val bracketIndex = cleaned.indexOf('[')
    if (bracketIndex > 0) {
        cleaned = cleaned.substring(0, bracketIndex)
    }

    val dashRegex = Regex("\\s+[-–—]\\s*|\\s*[-–—]\\s+")
    val dashMatch = dashRegex.find(cleaned)
    if (dashMatch != null && dashMatch.range.first > 0) {
        cleaned = cleaned.substring(0, dashMatch.range.first)
    }

    val pipeIndex = cleaned.indexOf('|')
    if (pipeIndex > 0) {
        cleaned = cleaned.substring(0, pipeIndex)
    }

    val slashRegex = Regex("\\s+[/\\\\]+\\s*|\\s*[/\\\\]+\\s+")
    val slashMatch = slashRegex.find(cleaned)
    if (slashMatch != null && slashMatch.range.first > 0) {
        cleaned = cleaned.substring(0, slashMatch.range.first)
    }

    val result = cleaned.trim()
    return if (result.isNotBlank()) result else this.trim()
}


