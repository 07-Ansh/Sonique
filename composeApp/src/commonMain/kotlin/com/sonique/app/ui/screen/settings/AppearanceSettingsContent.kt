package com.sonique.app.ui.screen.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.Platform
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun AppearanceSettingsContent(viewModel: SettingsViewModel) {
    val ambienceMode by viewModel.ambienceMode.collectAsStateWithLifecycle()
    val enableLiquidGlass by viewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val liquidGlassGlassiness by viewModel.liquidGlassGlassiness.collectAsStateWithLifecycle()
    val enablePageTransitions by viewModel.enablePageTransitions.collectAsStateWithLifecycle()
    val continueListeningLayout by viewModel.continueListeningLayout.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val currentLayoutLabel = when (continueListeningLayout) {
        "1_row" -> "1 Row (Standard)"
        "2_row" -> "2 Rows (Compact Grid)"
        "3x3" -> "3x3 Grid (Pages)"
        "list" -> "Vertical List"
        else -> "1 Row (Standard)"
    }

    val playerScreenStyle by viewModel.playerScreenStyle.collectAsStateWithLifecycle()
    val playerStyleLabel = when (playerScreenStyle) {
        "classic" -> "Classic"
        else -> "Material 3 (Modern)"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "Background Effects",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text("Ambience Mode") },
                        description = { Text("Show gradient background based on album art colors") },
                        isSwitch = true,
                        checked = ambienceMode,
                        onCheckedChange = { viewModel.setAmbienceMode(it) }
                    )
                )
            )
        }

        item {
            Material3SettingsGroup(
                title = "Player Screen",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text("Player Style") },
                        description = { Text(playerStyleLabel) },
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = "Player Style",
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = listOf(
                                                (playerScreenStyle == "classic") to "Classic",
                                                (playerScreenStyle != "classic") to "Material 3 (Modern)"
                                            )
                                        ),
                                        confirm = "Change" to { state ->
                                            val selected = state.selectOne?.getSelected() ?: ""
                                            val styleKey = if (selected == "Material 3 (Modern)") "modern" else "classic"
                                            viewModel.setPlayerScreenStyle(styleKey)
                                        },
                                        dismiss = "Cancel"
                                    )
                                )
                            }
                        }
                    )
                )
            )
        }

        item {
            Material3SettingsGroup(
                title = "Home Screen",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text("Continue Listening Layout") },
                        description = { Text(currentLayoutLabel) },
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = "Continue Listening Layout",
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = listOf(
                                                (continueListeningLayout == "1_row") to "1 Row (Standard)",
                                                (continueListeningLayout == "2_row") to "2 Rows (Compact Grid)",
                                                (continueListeningLayout == "3x3") to "3x3 Grid (Pages)",
                                                (continueListeningLayout == "list") to "Vertical List"
                                            )
                                        ),
                                        confirm = "Change" to { state ->
                                            val selectedLabel = state.selectOne?.getSelected() ?: ""
                                            val layoutValue = when (selectedLabel) {
                                                "1 Row (Standard)" -> "1_row"
                                                "2 Rows (Compact Grid)" -> "2_row"
                                                "3x3 Grid (Pages)" -> "3x3"
                                                "Vertical List" -> "list"
                                                else -> "1_row"
                                            }
                                            viewModel.setContinueListeningLayout(layoutValue)
                                        },
                                        dismiss = "Cancel"
                                    )
                                )
                            }
                        }
                    )
                )
            )
        }

        if (getPlatform() == Platform.Android) {
            item {
                Material3SettingsGroup(
                    title = "Liquid Glass",
                    items = buildList {
                        add(
                            Material3SettingsItem(
                                title = { Text("Apple Liquid Glass") },
                                description = { Text("Apple-style floating bottom layout with real-time backdrop luminance sensing") },
                                isSwitch = true,
                                checked = enableLiquidGlass,
                                onCheckedChange = { viewModel.setEnableLiquidGlass(it) }
                            )
                        )
                        if (!enableLiquidGlass) {
                            add(
                                Material3SettingsItem(
                                    title = { Text("Page Transitions") },
                                    description = { Text("Enable sliding animation when switching pages") },
                                    isSwitch = true,
                                    checked = enablePageTransitions,
                                    onCheckedChange = { viewModel.setEnablePageTransitions(it) }
                                )
                            )
                        }
                    }
                )
            }

            if (enableLiquidGlass) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Liquid Glass Opacity (Glassiness)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(liquidGlassGlassiness * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
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
                            val thumbScaleX = animateFloatAsState(targetValue = if (isInteracting) 1.25f else 1.0f)
                            val thumbScaleY = animateFloatAsState(targetValue = if (isInteracting) 0.82f else 1.0f)
                            val thumbWidth = animateDpAsState(targetValue = if (isInteracting) 54.dp else 24.dp)
                            val thumbHeight = animateDpAsState(targetValue = if (isInteracting) 44.dp else 24.dp)
                            val thumbCornerRadius = animateDpAsState(targetValue = if (isInteracting) 22.dp else 12.dp)
                            val solidAlpha = animateFloatAsState(targetValue = if (isInteracting) 0.0f else 1.0f)
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
                                                var dragEvent: androidx.compose.ui.input.pointer.PointerInputChange? = null
                                                do {
                                                    val event = awaitPointerEvent()
                                                    val dragChange = event.changes.firstOrNull { change -> change.id == pointerId }
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
                                                } while (dragEvent != null || event.changes.any { change -> change.pressed })
                                                isInteracting = false
                                            }
                                        }
                                    }
                                    .onSizeChanged { size -> trackWidth = size.width.toFloat() },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(liquidGlassGlassiness)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White)
                                )
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            val thumbWidthPx = with(density) { thumbWidth.value.toPx() }
                                            val xOffset = (liquidGlassGlassiness * trackWidth) - (thumbWidthPx / 2f)
                                            IntOffset(xOffset.roundToInt().coerceIn(0, (trackWidth - thumbWidthPx).roundToInt()), 0)
                                        }
                                        .size(width = thumbWidth.value, height = thumbHeight.value)
                                        .graphicsLayer {
                                            scaleX = thumbScaleX.value
                                            scaleY = thumbScaleY.value
                                        }
                                        .clip(RoundedCornerShape(thumbCornerRadius.value))
                                        .background(Color.White.copy(alpha = 0.15f * (1f - solidAlpha.value)))
                                        .border(1.dp, Color.White.copy(alpha = 0.25f * (1f - solidAlpha.value)), RoundedCornerShape(thumbCornerRadius.value))
                                ) {
                                    if (solidAlpha.value > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White.copy(alpha = solidAlpha.value))
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
                    }
                }
            }
        }
    }
}
