package com.sonique.app.ui.screen.changelog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Velocity
import com.sonique.app.expect.ui.BackHandler
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_close_24

private val MARKDOWN_REGEX = Regex("(\\*\\*(.*?)\\*\\*|`(.*?)`|\\[(.*?)\\]\\((.*?)\\))")

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
        val matches = MARKDOWN_REGEX.findAll(text)

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
    visible: Boolean = true,
    onDismissRequest: () -> Unit = {},
    onDismiss: () -> Unit = onDismissRequest,
    updateViewModel: UpdateViewModel = koinViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val changelogState by updateViewModel.changelogState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()

    val sheetSpringSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeightPx = constraints.maxHeight.toFloat()
        val sheetHeightPx = screenHeightPx
        val sheetHeightDp = with(density) { sheetHeightPx.toDp() }

        // Default open height: ~78% of screen height
        val halfHeightPx = (screenHeightPx * 0.78f).coerceAtLeast(1f)
        val halfOffsetPx = (sheetHeightPx - halfHeightPx).coerceAtLeast(0f)
        val hiddenOffsetPx = sheetHeightPx + with(density) { 60.dp.toPx() }

        val offsetAnimatable = remember { Animatable(hiddenOffsetPx) }

        // Track where the current drag gesture originated (Fullscreen vs Stage 1)
        var gestureStartOffset by remember { mutableFloatStateOf(halfOffsetPx) }

        // When `visible` toggles:
        LaunchedEffect(visible) {
            if (visible) {
                offsetAnimatable.snapTo(hiddenOffsetPx)
                coroutineScope.launch { runCatching { lazyListState.scrollToItem(0) } }
                gestureStartOffset = halfOffsetPx
                updateViewModel.loadChangelog()
                offsetAnimatable.animateTo(
                    targetValue = halfOffsetPx,
                    animationSpec = sheetSpringSpec
                )
            } else {
                if (offsetAnimatable.value < hiddenOffsetPx) {
                    offsetAnimatable.animateTo(
                        targetValue = hiddenOffsetPx,
                        animationSpec = sheetSpringSpec
                    )
                }
            }
        }

        fun dismissSmoothly() {
            coroutineScope.launch {
                offsetAnimatable.animateTo(
                    targetValue = hiddenOffsetPx,
                    animationSpec = sheetSpringSpec
                )
                onDismiss()
            }
        }

        val currentOffset = offsetAnimatable.value
        val isSheetVisible = currentOffset < hiddenOffsetPx - 10f

        // BackHandler: collapses from Fullscreen to Stage 1, or dismisses from Stage 1
        if (visible && isSheetVisible) {
            BackHandler(enabled = true) {
                coroutineScope.launch {
                    if (offsetAnimatable.value < halfOffsetPx - 20f) {
                        // Collapse to Stage 1
                        offsetAnimatable.animateTo(
                            targetValue = halfOffsetPx,
                            animationSpec = sheetSpringSpec
                        )
                    } else {
                        // Dismiss
                        offsetAnimatable.animateTo(
                            targetValue = hiddenOffsetPx,
                            animationSpec = sheetSpringSpec
                        )
                        onDismiss()
                    }
                }
            }
        }

        fun snapOrAnimateToAnchor(velocityY: Float = 0f) {
            val curr = offsetAnimatable.value
            val wasStartingFromFull = gestureStartOffset < halfOffsetPx * 0.45f
            coroutineScope.launch {
                if (velocityY < -500f) {
                    // Quick flick UP -> expand to Fullscreen
                    offsetAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = sheetSpringSpec,
                        initialVelocity = velocityY
                    )
                } else if (velocityY > 500f) {
                    // Quick flick DOWN
                    if (wasStartingFromFull) {
                        // Swiped down from Fullscreen -> collapse to Stage 1 first
                        offsetAnimatable.animateTo(
                            targetValue = halfOffsetPx,
                            animationSpec = sheetSpringSpec,
                            initialVelocity = velocityY
                        )
                    } else {
                        // Swiped down from Stage 1 -> Dismiss sheet
                        offsetAnimatable.animateTo(
                            targetValue = hiddenOffsetPx,
                            animationSpec = sheetSpringSpec,
                            initialVelocity = velocityY
                        )
                        onDismiss()
                    }
                } else {
                    // Low velocity / slow drag release:
                    if (wasStartingFromFull) {
                        val fullToHalfThreshold = halfOffsetPx * 0.25f
                        if (curr > fullToHalfThreshold) {
                            offsetAnimatable.animateTo(
                                targetValue = halfOffsetPx,
                                animationSpec = sheetSpringSpec
                            )
                        } else {
                            offsetAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = sheetSpringSpec
                            )
                        }
                    } else {
                        val halfToFullThreshold = halfOffsetPx * 0.75f
                        val halfToHiddenThreshold = halfOffsetPx + (hiddenOffsetPx - halfOffsetPx) * 0.25f

                        when {
                            curr < halfToFullThreshold -> {
                                offsetAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = sheetSpringSpec
                                )
                            }
                            curr > halfToHiddenThreshold -> {
                                offsetAnimatable.animateTo(
                                    targetValue = hiddenOffsetPx,
                                    animationSpec = sheetSpringSpec
                                )
                                onDismiss()
                            }
                            else -> {
                                offsetAnimatable.animateTo(
                                    targetValue = halfOffsetPx,
                                    animationSpec = sheetSpringSpec
                                )
                            }
                        }
                    }
                }
            }
        }

        var isNestedScrollDragging by remember { mutableStateOf(false) }
        var touchStartedInContentScroll by remember { mutableStateOf(false) }

        LaunchedEffect(lazyListState.isScrollInProgress) {
            if (!lazyListState.isScrollInProgress) {
                touchStartedInContentScroll = false
                isNestedScrollDragging = false
            }
        }

        val nestedScrollConnection = remember(sheetHeightPx, halfOffsetPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val deltaY = available.y
                    val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0

                    if (!isNestedScrollDragging) {
                        isNestedScrollDragging = true
                        touchStartedInContentScroll = !isAtTop
                        gestureStartOffset = offsetAnimatable.value
                    }

                    if (touchStartedInContentScroll) {
                        return Offset.Zero
                    }

                    // Dragging UP: if sheet is not fully expanded, consume drag to expand sheet
                    if (deltaY < 0 && offsetAnimatable.value > 0f) {
                        val newTarget = (offsetAnimatable.value + deltaY).coerceAtLeast(0f)
                        val consumed = newTarget - offsetAnimatable.value
                        coroutineScope.launch { offsetAnimatable.snapTo(newTarget) }
                        return Offset(0f, consumed)
                    }

                    // Dragging DOWN when content is already at the top:
                    if (deltaY > 0 && isAtTop) {
                        val wasStartingFromFull = gestureStartOffset < halfOffsetPx * 0.45f
                        val maxTarget = if (wasStartingFromFull) halfOffsetPx else hiddenOffsetPx
                        val newTarget = (offsetAnimatable.value + deltaY).coerceIn(0f, maxTarget)
                        val consumed = newTarget - offsetAnimatable.value
                        coroutineScope.launch { offsetAnimatable.snapTo(newTarget) }
                        return Offset(0f, consumed)
                    }

                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (touchStartedInContentScroll) {
                        return Offset.Zero
                    }

                    val deltaY = available.y
                    val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                    if (deltaY > 0 && isAtTop) {
                        val wasStartingFromFull = gestureStartOffset < halfOffsetPx * 0.45f
                        val maxTarget = if (wasStartingFromFull) halfOffsetPx else hiddenOffsetPx
                        val newTarget = (offsetAnimatable.value + deltaY).coerceIn(0f, maxTarget)
                        val consumedY = newTarget - offsetAnimatable.value
                        coroutineScope.launch { offsetAnimatable.snapTo(newTarget) }
                        return Offset(0f, consumedY)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (touchStartedInContentScroll) {
                        isNestedScrollDragging = false
                        touchStartedInContentScroll = false
                        return Velocity.Zero
                    }
                    if (offsetAnimatable.value > 0f) {
                        isNestedScrollDragging = false
                        snapOrAnimateToAnchor(available.y)
                        return available
                    }
                    isNestedScrollDragging = false
                    return Velocity.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    val wasInContent = touchStartedInContentScroll
                    isNestedScrollDragging = false
                    touchStartedInContentScroll = false
                    if (wasInContent) {
                        return available
                    }
                    val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                    if (isAtTop && available.y != 0f) {
                        snapOrAnimateToAnchor(available.y)
                        return available
                    }
                    return Velocity.Zero
                }
            }
        }

        val handleVelocityTracker = remember { VelocityTracker() }

        // In-Hierarchy Scrim
        if (isSheetVisible) {
            val progress = ((hiddenOffsetPx - currentOffset) / hiddenOffsetPx).coerceIn(0f, 1f)
            val scrimAlpha = (progress * 0.42f).coerceIn(0f, 0.42f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha }
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        dismissSmoothly()
                    }
            )
        }

        // Sheet Surface & Content
        if (currentOffset < hiddenOffsetPx) {
            val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val expandFraction = if (halfOffsetPx > 0f) {
                ((halfOffsetPx - currentOffset) / halfOffsetPx).coerceIn(0f, 1f)
            } else 0f
            val dynamicTopPadding = statusBarTopPadding * expandFraction
            val dynamicCornerRadius = 24.dp * (1f - expandFraction)
            val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeightDp)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationY = currentOffset
                    }
                    .clip(RoundedCornerShape(topStart = dynamicCornerRadius, topEnd = dynamicCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = dynamicTopPadding)
                ) {
                    // Pinned Top Header (Never scrolls out of view)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Minimal pill drag handle with vertical gesture detection
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 6.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            gestureStartOffset = offsetAnimatable.value
                                            handleVelocityTracker.resetTracking()
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            handleVelocityTracker.addPointerInputChange(change)
                                            val wasStartingFromFull = gestureStartOffset < halfOffsetPx * 0.45f
                                            val maxTarget = if (wasStartingFromFull) halfOffsetPx else hiddenOffsetPx
                                            val newTarget = (offsetAnimatable.value + dragAmount).coerceIn(0f, maxTarget)
                                            coroutineScope.launch { offsetAnimatable.snapTo(newTarget) }
                                        },
                                        onDragEnd = {
                                            val vy = handleVelocityTracker.calculateVelocity().y
                                            snapOrAnimateToAnchor(vy)
                                        },
                                        onDragCancel = {
                                            snapOrAnimateToAnchor(0f)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Changelog",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = { dismissSmoothly() },
                                modifier = Modifier.align(Alignment.CenterEnd),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_close_24),
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Clean straight line divider
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        )
                    }

                    // Scrollable Content (Only the content below the line scrolls)
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .nestedScroll(nestedScrollConnection)
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(
                            top = 16.dp,
                            bottom = navBarPadding + 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
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
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
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
