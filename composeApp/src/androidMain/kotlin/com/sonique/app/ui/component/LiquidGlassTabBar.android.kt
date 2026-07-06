package com.sonique.app.ui.component

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.sonique.app.expect.ui.PlatformBackdrop
import com.sonique.app.ui.theme.typo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

private val CapsuleShape = RoundedCornerShape(percent = 50)
private val TabWidth = 96.dp
private val BarHeight = 64.dp
private val BlobHeight = 56.dp

private val bottomBarSeedDark = Color(0xFFFF3B30)
private val white = Color(0xFFFFFFFF)

/**
 * iOS 26 / Kyant-style liquid sliding tab bar.
 */
@Composable
fun LiquidGlassTabBar(
    tabs: List<BottomNavScreen>,
    selectedTab: Int,
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer,
    luminance: Float,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    tabWidth: Dp = TabWidth,
    glassiness: Float = 0.5f,
    onTabSelected: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val tabsCount = tabs.size
    val tabWidthPx = with(density) { tabWidth.toPx() }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val barInteraction = rememberGlassInteraction()

    var currentIndex by remember { mutableIntStateOf(selectedTab.coerceAtLeast(0)) }
    val draggedFlag = remember { booleanArrayOf(false) }

    val dampedDrag =
        remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTab.coerceAtLeast(0).toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 76f / 56f,
                onDragStarted = { draggedFlag[0] = false },
                onDragStopped = {
                    if (draggedFlag[0]) {
                        val target = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                        currentIndex = target
                        animateToValue(target.toFloat())
                    }
                },
                onDrag = { _, dragAmount ->
                    if (dragAmount.x != 0f) draggedFlag[0] = true
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                },
            )
        }

    LaunchedEffect(selectedTab) {
        if (selectedTab >= 0 && currentIndex != selectedTab) currentIndex = selectedTab
    }

    LaunchedEffect(dampedDrag) {
        snapshotFlow { currentIndex }
            .drop(1)
            .collectLatest { index ->
                dampedDrag.animateToValue(index.toFloat())
                if (draggedFlag[0]) onTabSelected(index)
            }
    }

    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 200f)
    )

    val animatedWidth = 48.dp + (tabWidth * tabsCount - 48.dp) * expandProgress
    val animatedHeight = 48.dp + (BarHeight - 48.dp) * expandProgress

    Box(
        modifier =
            modifier
                .height(animatedHeight)
                .width(animatedWidth)
                .pointerInput(barInteraction) { barInteraction.detectPress(this) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.matchParentSize().drawInteractiveGlass(backdrop, layer, luminance, CapsuleShape, barInteraction, glassiness = glassiness))

        if (expandProgress > 0f) {
            Box(
                Modifier
                    .graphicsLayer {
                        translationX =
                            ((if (isLtr) dampedDrag.value else (tabsCount - 1) - dampedDrag.value) * tabWidthPx +
                                4.dp.toPx()) * expandProgress
                        alpha = expandProgress
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CapsuleShape },
                        effects = {
                            val l = (luminance * 2f - 1f).let { sign(it) * it * it }
                            val progress = dampedDrag.pressProgress
                            vibrancy()
                            colorControls(
                                brightness = 0.05f,
                                contrast = 1f,
                                saturation = 1.5f,
                            )
                            blur(
                                (if (l > 0f) lerp(8f.dp.toPx(), 16f.dp.toPx(), l) else lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)) +
                                    20f.dp.toPx(),
                            )
                            lens(10f.dp.toPx() * progress, 14f.dp.toPx() * progress, chromaticAberration = true)
                        },
                        highlight = { Highlight.Default.copy(alpha = 0.6f) },
                        shadow = { Shadow(radius = 4f.dp, alpha = 0.4f) },
                        innerShadow = {
                            val progress = dampedDrag.pressProgress
                            InnerShadow(radius = 8f.dp * progress, alpha = progress)
                        },
                        layerBlock = {
                            scaleX = dampedDrag.scaleX
                            scaleY = dampedDrag.scaleY
                            val velocity = dampedDrag.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val darken = lerp(0.22f, 0.55f, ((luminance - 0.3f) / 0.5f).coerceIn(0f, 1f))
                            drawRect(Color.Black.copy(alpha = darken))
                        },
                    )
                    .width(((tabWidth - 8.dp) * expandProgress).coerceAtLeast(1.dp))
                    .height(BlobHeight * expandProgress + (40.dp - BlobHeight) * (1f - expandProgress)),
            )
        }

        Row(
            Modifier
                .matchParentSize()
                .then(dampedDrag.modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { position, screen ->
                val tabAlpha = if (position == 0) 1f else expandProgress
                val animatedTabWidth = if (position == 0) {
                    48.dp + (tabWidth - 48.dp) * expandProgress
                } else {
                    tabWidth * expandProgress
                }
                Box(
                    modifier = Modifier
                        .width(animatedTabWidth)
                        .alpha(tabAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassTab(
                        screen = screen,
                        selected = currentIndex == position && isExpanded,
                        tabWidth = animatedTabWidth,
                        isExpanded = isExpanded,
                    ) {
                        if (isExpanded) {
                            if (position == currentIndex) {
                                onTabSelected(position)
                            } else {
                                currentIndex = position
                                onTabSelected(position)
                            }
                        } else {
                            currentIndex = 0
                            onTabSelected(0)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassTab(
    screen: BottomNavScreen,
    selected: Boolean,
    tabWidth: Dp,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) bottomBarSeedDark else white
    Column(
        Modifier
            .width(tabWidth)
            .fillMaxHeight()
            .clip(CapsuleShape)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            Icon(imageVector = screen.icon, contentDescription = null)
            if (isExpanded) {
                Text(
                    text = stringResource(screen.title),
                    style = typo().bodySmall,
                    color = color,
                    maxLines = 1,
                )
            }
        }
    }
}

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {
    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier =
        Modifier.pointerInput(Unit) {
            inspectDragGestures(
                onDragStart = { down ->
                    onDragStarted(down.position)
                    press()
                },
                onDragEnd = {
                    onDragStopped()
                    release()
                },
                onDragCancel = {
                    onDragStopped()
                    release()
                },
            ) { _, dragAmount ->
                onDrag(size, dragAmount)
            }
        }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            withFrameNanos {}
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
        animationScope.launch {
            valueAnimation.animateTo(target, valueAnimationSpec) { updateVelocity() }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
                launch { valueAnimation.animateTo(target, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(SystemClock.uptimeMillis(), Offset(value, 0f))
        val targetVelocity =
            velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}
