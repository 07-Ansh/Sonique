package com.sonique.app.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonique.app.ui.theme.musica_accent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.liquidGlass

@Composable
fun Chip(
    isAnimated: Boolean = false,
    isSelected: Boolean = false,
    text: String,
    onClick: () -> Unit,
) {
    val sharedViewModel: SharedViewModel = koinInject()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    if (enableLiquidGlass) {
        val bgAlpha by animateFloatAsState(
            targetValue = if (isSelected) 0.15f else 0.05f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
        val borderAlpha by animateFloatAsState(
            targetValue = if (isSelected) 0.2f else 0.08f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
        val textAlpha by animateFloatAsState(
            targetValue = if (isSelected) 1f else 0.6f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = bgAlpha))
                .border(BorderStroke(0.5.dp, Color.White.copy(alpha = borderAlpha)), RoundedCornerShape(20.dp))
                .liquidGlass(backdrop, shape = RoundedCornerShape(20.dp), interactive = true)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = expandHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(350)),
                    exit = shrinkHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(tween(350))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Done icon",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White.copy(alpha = textAlpha)
                )
            }
        }
    } else {
        InfiniteBorderAnimationView(
            isAnimated = isAnimated && isSelected,
            brush = Brush.sweepGradient(listOf(musica_accent, Color.White)),
            backgroundColor = MaterialTheme.colorScheme.background,
            contentPadding = 0.dp,
            borderWidth = 1.dp,
            shape = RoundedCornerShape(8.dp),
            oneCircleDurationMillis = 2500,
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                ElevatedFilterChip(
                    elevation = FilterChipDefaults.elevatedFilterChipElevation(
                        elevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        disabledElevation = 0.dp
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        FilterChipDefaults.elevatedFilterChipColors(
                            containerColor = Color.White.copy(alpha = 0.05f),
                            iconColor = Color.White,
                            selectedContainerColor = Color.White.copy(alpha = 0.15f),
                            labelColor = Color.Gray,
                            selectedLabelColor = Color.White,
                        ),
                    onClick = { onClick.invoke() },
                    label = {
                        Text(text)
                    },
                    border =
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = Color.Transparent,
                            borderColor = Color.Transparent,
                        ),
                    selected = isSelected,
                    leadingIcon =
                        if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Done icon",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                )
            }
        }
    }
}

