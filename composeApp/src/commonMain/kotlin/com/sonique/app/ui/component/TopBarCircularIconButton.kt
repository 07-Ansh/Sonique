package com.sonique.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sonique.app.expect.ui.PlatformBackdrop
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * A circular action button for top app bars that smoothly adapts between
 * Normal theme (Material 3 surfaceContainerHigh circle) and Liquid Glass theme (interactive glass surface).
 */
@Composable
fun TopBarCircularIconButton(
    enableLiquidGlass: Boolean,
    backdrop: PlatformBackdrop,
    modifier: Modifier = Modifier.size(40.dp),
    resId: DrawableResource? = null,
    imageVector: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null,
    onClick: () -> Unit,
    badge: @Composable (BoxScope.() -> Unit)? = null,
) {
    val iconContent = @Composable {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                resId != null -> {
                    Icon(
                        painter = painterResource(resId),
                        contentDescription = contentDescription,
                        tint = tint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                imageVector != null -> {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = contentDescription,
                        tint = tint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    if (enableLiquidGlass) {
        LiquidGlassContainer(
            backdrop = backdrop,
            modifier = modifier,
            shape = CircleShape,
            interactive = true,
        ) {
            iconContent()
            badge?.invoke(this)
        }
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            iconContent()
            badge?.invoke(this)
        }
    }
}
