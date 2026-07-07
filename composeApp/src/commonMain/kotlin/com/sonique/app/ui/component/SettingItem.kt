package com.sonique.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import com.sonique.app.viewModel.SharedViewModel
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import com.sonique.app.extension.greyScale
import com.sonique.app.ui.theme.typo
import com.sonique.app.ui.theme.white
import com.sonique.app.ui.component.liquidGlass
import com.sonique.app.expect.ui.PlatformBackdrop
import com.sonique.app.expect.ui.rememberBackdrop

@Composable
fun SettingItem(
    title: String = "Title",
    subtitle: String = "Subtitle",
    smallSubtitle: Boolean = false,
    isEnable: Boolean = true,
    onClick: (() -> Unit)? = null,
    switch: Pair<Boolean, ((Boolean) -> Unit)>? = null,
    onDisable: (() -> Unit)? = null,  
    otherView: @Composable (() -> Unit)? = null,
    loading: Boolean = false,
) {
    val sharedViewModel: SharedViewModel = koinInject()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    LaunchedEffect(Unit) {
        if (!isEnable && onDisable != null) {
            onDisable.invoke()
        }
    }

    val cardModifier = if (enableLiquidGlass) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(28.dp))
            .liquidGlass(backdrop, shape = RoundedCornerShape(28.dp), interactive = false)
            .then(
                if (onClick != null && isEnable && !loading) {
                    Modifier.clickable { onClick.invoke() }
                } else {
                    Modifier
                }
            )
            .then(
                if (!isEnable) {
                    Modifier.greyScale()
                } else {
                    Modifier
                }
            )
    } else {
        Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && isEnable && !loading) {
                    Modifier.clickable { onClick.invoke() }
                } else {
                    Modifier
                }
            )
            .then(
                if (!isEnable) {
                    Modifier.greyScale()
                } else {
                    Modifier
                }
            )
    }

    Box(
        modifier = cardModifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = if (enableLiquidGlass) 12.dp else 8.dp,
                    horizontal = if (enableLiquidGlass) 20.dp else 24.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    style =
                        typo().labelMedium.copy(fontSize = 12.sp).let {
                            if (!isEnable) it.greyScale() else it
                        },
                    color = white,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style =
                        if (smallSubtitle) {
                            typo().bodySmall.copy(fontSize = 9.5.sp).let {
                                if (!isEnable) it.greyScale() else it
                            }
                        } else {
                            typo().bodyMedium.copy(fontSize = 11.sp).let {
                                if (!isEnable) it.greyScale() else it
                            }
                        },
                    maxLines = 2,
                )

                otherView?.let {
                    Spacer(Modifier.height(16.dp))
                    it.invoke()
                }
            }
            if (loading) {
                Spacer(Modifier.width(10.dp))
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.width(24.dp).height(24.dp),
                    strokeWidth = 2.dp
                )
            } else if (switch != null) {
                Spacer(Modifier.width(10.dp))
                val sharedViewModel: SharedViewModel = koinInject()
                val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
                if (enableLiquidGlass) {
                    val density = LocalDensity.current
                    val targetOffset = if (switch.first) 25.dp else 3.dp
                    val thumbOffset by animateDpAsState(
                        targetValue = targetOffset,
                        animationSpec = spring(dampingRatio = 0.55f, stiffness = 350f)
                    )
                    val targetOffsetPx = with(density) { targetOffset.toPx() }
                    val thumbOffsetPx = with(density) { thumbOffset.toPx() }
                    val distance = abs(targetOffsetPx - thumbOffsetPx)
                    val stretch = (distance * 0.008f).coerceIn(0f, 0.5f)
                    val thumbWidth = 22.dp * (1f + stretch)
                    val thumbHeight = 22.dp * (1f - stretch * 0.4f)
                    val trackBg = if (switch.first) Color(0xFFFF3B30).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                    val borderAlpha = if (switch.first) 0.3f else 0.15f
                    val thumbColor = if (switch.first) Color.White else Color(0xFFE0E0E0)
                    
                    val switchInteractionSource = remember { MutableInteractionSource() }
                    val isSwitchPressed by switchInteractionSource.collectIsPressedAsState()
                    val switchScale by animateFloatAsState(
                        targetValue = if (isSwitchPressed) 0.90f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 50.dp, height = 28.dp)
                            .graphicsLayer {
                                scaleX = switchScale
                                scaleY = switchScale
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(trackBg)
                            .border(1.dp, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = switchInteractionSource,
                                indication = null,
                                enabled = isEnable
                            ) { switch.second.invoke(!switch.first) },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = thumbOffset)
                                .size(width = thumbWidth, height = thumbHeight)
                                .clip(RoundedCornerShape(11.dp))
                                .background(thumbColor)
                        )
                    }
                } else {
                    Switch(
                        modifier = Modifier.wrapContentWidth(),
                        checked = switch.first,
                        onCheckedChange = {
                            switch.second.invoke(it)
                        },
                        enabled = isEnable,
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(text: String) {
    val sharedViewModel: SharedViewModel = koinInject()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(
            start = if (enableLiquidGlass) 28.dp else 24.dp,
            top = 18.dp,
            bottom = 8.dp
        )
    )
}


