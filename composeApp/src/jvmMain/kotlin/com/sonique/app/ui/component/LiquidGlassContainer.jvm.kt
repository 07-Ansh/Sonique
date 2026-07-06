package com.sonique.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.sonique.app.expect.ui.PlatformBackdrop

@Composable
actual fun Modifier.liquidGlass(
    backdrop: PlatformBackdrop,
    shape: Shape,
    interactive: Boolean,
): Modifier = this.clip(shape)

@Composable
actual fun Modifier.liquidGlass(
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape,
    interactive: Boolean,
): Modifier = this.clip(shape)
