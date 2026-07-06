package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.sonique.app.ui.component.liquidGlass
import com.sonique.app.ui.component.SettingsSectionHeader
import com.sonique.app.expect.ui.rememberBackdrop
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.Platform
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.viewModel.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUiScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val ambienceMode by viewModel.ambienceMode.collectAsStateWithLifecycle()
    val enableLiquidGlass by viewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val liquidGlassGlassiness by viewModel.liquidGlassGlassiness.collectAsStateWithLifecycle()
    val blurPlayerBackground by viewModel.blurPlayerBackground.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getData()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val backdrop = rememberBackdrop()
        TopAppBar(
            title = { Text("Appearance (New)") },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .then(
                            if (enableLiquidGlass) {
                                Modifier.liquidGlass(backdrop, shape = CircleShape, interactive = true)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Back")
                }
            }
        )
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item {
                SettingsSectionHeader("Background Effects")
                SettingItem(
                    title = "Ambience Mode",
                    subtitle = "Show gradient background based on album art colors",
                    switch = (ambienceMode to { viewModel.setAmbienceMode(it) }),
                )
                SettingItem(
                    title = "Frosted Player Background",
                    subtitle = "Blur background artwork based on album art using frosted glassmorphism",
                    switch = (blurPlayerBackground to { viewModel.setBlurPlayerBackground(it) }),
                )
                if (getPlatform() == Platform.Android) {
                    SettingsSectionHeader("Liquid Glass")
                    SettingItem(
                        title = "Apple Liquid Glass Bar",
                        subtitle = "Apple-style floating bottom bar with real-time backdrop luminance sensing",
                        switch = (enableLiquidGlass to { viewModel.setEnableLiquidGlass(it) }),
                    )
                    if (enableLiquidGlass) {
                        val backdrop = rememberBackdrop()
                        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Liquid Glass Opacity (Glassiness)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(liquidGlassGlassiness * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .then(
                                        if (enableLiquidGlass) {
                                            Modifier
                                                .background(Color.White.copy(alpha = 0.06f))
                                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
                                        } else {
                                            Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        }
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Opacity,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                var isInteracting by remember { mutableStateOf(false) }
                                val thumbScaleX by animateFloatAsState(
                                    targetValue = if (isInteracting) 1.25f else 1.0f,
                                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
                                )
                                val thumbScaleY by animateFloatAsState(
                                    targetValue = if (isInteracting) 0.82f else 1.0f,
                                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
                                )
                                val thumbWidth by animateDpAsState(
                                    targetValue = if (isInteracting) 54.dp else 24.dp,
                                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f)
                                )
                                val thumbHeight by animateDpAsState(
                                    targetValue = if (isInteracting) 44.dp else 24.dp,
                                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f)
                                )
                                val thumbCornerRadius by animateDpAsState(
                                    targetValue = if (isInteracting) 22.dp else 12.dp,
                                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f)
                                )
                                val solidAlpha by animateFloatAsState(
                                    targetValue = if (isInteracting) 0.0f else 1.0f,
                                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f)
                                )
                                var trackWidth by remember { mutableStateOf(0f) }
                                val density = LocalDensity.current
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                    isInteracting = true
                                                    if (trackWidth > 0f) {
                                                        val progress = (down.position.x / trackWidth).coerceIn(0f, 1f)
                                                        viewModel.setLiquidGlassGlassiness(progress)
                                                    }
                                                    var pointerId = down.id
                                                    var dragEvent: PointerInputChange? = null
                                                    do {
                                                        val event = awaitPointerEvent()
                                                        val dragChange = event.changes.firstOrNull { it.id == pointerId }
                                                        if (dragChange != null && dragChange.pressed) {
                                                            if (trackWidth > 0f) {
                                                                val progress = (dragChange.position.x / trackWidth).coerceIn(0f, 1f)
                                                                viewModel.setLiquidGlassGlassiness(progress)
                                                            }
                                                            dragChange.consume()
                                                            dragEvent = dragChange
                                                        } else {
                                                            dragEvent = null
                                                        }
                                                    } while (dragEvent != null || event.changes.any { it.pressed })
                                                    isInteracting = false
                                                }
                                            }
                                        }
                                        .onSizeChanged { trackWidth = it.width.toFloat() },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    // Track Background (thin line)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (enableLiquidGlass) Color.White.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    )
                                    
                                    // Active Track (blue line)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(liquidGlassGlassiness)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (enableLiquidGlass) Color(0xFFFF3B30)
                                                else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                    
                                    // Thumb
                                    Box(
                                        modifier = Modifier
                                            .offset {
                                                val xOffset = (liquidGlassGlassiness * trackWidth) - with(density) { (thumbWidth / 2).toPx() }
                                                IntOffset(xOffset.roundToInt().coerceIn(0, (trackWidth - with(density) { thumbWidth.toPx() }).roundToInt()), 0)
                                            }
                                            .size(width = thumbWidth, height = thumbHeight)
                                            .graphicsLayer {
                                                scaleX = thumbScaleX
                                                scaleY = thumbScaleY
                                            }
                                            .clip(RoundedCornerShape(thumbCornerRadius))
                                            .then(
                                                if (enableLiquidGlass) {
                                                    Modifier
                                                        .liquidGlass(backdrop, shape = RoundedCornerShape(thumbCornerRadius), interactive = false)
                                                        .border(1.dp, Color.White.copy(alpha = 0.25f * (1f - solidAlpha)), RoundedCornerShape(thumbCornerRadius))
                                                } else {
                                                    Modifier.background(MaterialTheme.colorScheme.primary)
                                                }
                                            )
                                    ) {
                                        if (enableLiquidGlass && solidAlpha > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.White.copy(alpha = solidAlpha))
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Layers,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Clear is more transparent and tinted increases opacity, adding contrast to content and controls.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
