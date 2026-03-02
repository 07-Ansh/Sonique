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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonique.app.ui.theme.musica_accent

@Composable
fun Chip(
    isAnimated: Boolean = false,
    isSelected: Boolean = false,
    text: String,
    onClick: () -> Unit,
) {
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

