package com.sonique.app.expect.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.sign
import com.kyant.backdrop.backdrops.layerBackdrop as nativeBackdrop

actual typealias PlatformBackdrop = LayerBackdrop

object DevicePerformance {
    private var isLowEndCached: Boolean? = null

    fun isLowEnd(context: Context): Boolean {
        isLowEndCached?.let { return it }
        val isLowRam = try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            activityManager?.isLowRamDevice == true
        } catch (e: Exception) {
            false
        }
        val isOlderSdk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S // Android 12 is API 31 (S)
        val result = isLowRam || isOlderSdk
        isLowEndCached = result
        return result
    }
}

@Composable
actual fun rememberBackdrop(): PlatformBackdrop = rememberLayerBackdrop {
    drawRect(Color.Black)
    drawContent()
}

actual fun Modifier.layerBackdrop(backdrop: PlatformBackdrop): Modifier = composed {
    val context = LocalContext.current
    if (DevicePerformance.isLowEnd(context)) {
        this.background(Color(0xFF151515).copy(alpha = 0.85f))
    } else {
        this.nativeBackdrop(backdrop)
    }
}

actual fun Modifier.drawBackdropCustomShape(
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape
): Modifier = composed {
    val context = LocalContext.current
    if (DevicePerformance.isLowEnd(context)) {
        this.background(Color(0xFF1E1E1E).copy(alpha = 0.75f), shape)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape)
    } else {
        this.drawBackdrop(
            backdrop = backdrop,
            effects = {
                val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
                vibrancy()
                colorControls(
                    brightness =
                        if (l > 0f) {
                            lerp(0.1f, 0.5f, l)
                        } else {
                            lerp(0.1f, -0.2f, -l)
                        },
                    contrast =
                        if (l > 0f) {
                            lerp(1f, 0f, l)
                        } else {
                            1f
                        },
                    saturation = 1.5f,
                )
                blur(
                    if (l > 0f) {
                        lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                    } else {
                        lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                    },
                )
                lens(24f.dp.toPx(), size.minDimension / 2f, true)
            },
            onDrawBackdrop = { drawBackdrop ->
                drawBackdrop()
                layer.record { drawBackdrop() }
            },
            shape = { shape },
            onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.1f)) }
        )
    }
}
