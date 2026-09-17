package com.sonique.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.painterResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_downloaded
import sonique.composeapp.generated.resources.baseline_error_outline_24
import sonique.composeapp.generated.resources.baseline_music_note_24
import sonique.composeapp.generated.resources.baseline_playlist_add_24
import sonique.composeapp.generated.resources.favorite
import sonique.composeapp.generated.resources.favorite_border
import sonique.composeapp.generated.resources.metro_check

enum class ToastIconType {
    LIKE,
    UNLIKE,
    SUCCESS,
    PLAYLIST,
    DOWNLOAD,
    ERROR,
    INFO
}

data class SoniqueToastData(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val iconType: ToastIconType = ToastIconType.INFO,
    val durationMs: Long = 2600L,
)

object SoniqueToastManager {
    private val _toastFlow = MutableStateFlow<SoniqueToastData?>(null)
    val toastFlow: StateFlow<SoniqueToastData?> = _toastFlow.asStateFlow()

    private var idCounter = 0L

    fun show(
        message: String?,
        subtitle: String? = null,
        iconType: ToastIconType? = null,
        durationMs: Long = 2600L,
    ) {
        if (message.isNullOrBlank()) return
        val parsed = parseMessage(message)
        val resolvedTitle = parsed.first
        val resolvedSubtitle = subtitle ?: parsed.second
        val resolvedIcon = iconType ?: detectIconType(resolvedTitle, resolvedSubtitle)

        _toastFlow.value = SoniqueToastData(
            id = ++idCounter,
            title = resolvedTitle,
            subtitle = resolvedSubtitle,
            iconType = resolvedIcon,
            durationMs = durationMs
        )
    }

    fun dismiss() {
        _toastFlow.value = null
    }

    private fun parseMessage(raw: String): Pair<String, String?> {
        val trimmed = raw.trim()
        // Format: "Title (Subtitle)" -> clean split
        val parenRegex = Regex("""^(.+?)\s*\((.+?)\)$""")
        val match = parenRegex.find(trimmed)
        if (match != null) {
            val t = match.groupValues[1].trim()
            val s = match.groupValues[2].trim()
            if (t.isNotEmpty() && s.isNotEmpty()) {
                return Pair(t, s)
            }
        }
        // Format: "Title\nSubtitle"
        if (trimmed.contains("\n")) {
            val parts = trimmed.split("\n", limit = 2)
            return Pair(parts[0].trim(), parts[1].trim())
        }
        return Pair(trimmed, null)
    }

    private fun detectIconType(title: String, subtitle: String?): ToastIconType {
        val lower = (title + " " + (subtitle ?: "")).lowercase()
        return when {
            lower.contains("removed from liked") ||
                lower.contains("removed from library") ||
                lower.contains("unliked") -> ToastIconType.UNLIKE

            lower.contains("liked") ||
                lower.contains("favorite") -> ToastIconType.LIKE

            lower.contains("playlist") ||
                lower.contains("queue") -> ToastIconType.PLAYLIST

            lower.contains("download") -> ToastIconType.DOWNLOAD

            lower.contains("error") ||
                lower.contains("failed") ||
                lower.contains("can_not") ||
                lower.contains("cannot") ||
                lower.contains("no equalizer") -> ToastIconType.ERROR

            lower.contains("added") ||
                lower.contains("success") ||
                lower.contains("copied") ||
                lower.contains("saved") ||
                lower.contains("restore") -> ToastIconType.SUCCESS

            else -> ToastIconType.INFO
        }
    }
}

/**
 * Top-level floating pill alert host.
 * Renders modern frosted glass pill with icons, smooth spring animations, and touch-dismiss.
 */
@Composable
fun SoniqueToastHost() {
    val currentToast by SoniqueToastManager.toastFlow.collectAsStateWithLifecycle()
    var activeToast by remember { mutableStateOf<SoniqueToastData?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentToast) {
        if (currentToast != null) {
            activeToast = currentToast
            isVisible = true
            delay(currentToast!!.durationMs)
            isVisible = false
            delay(280) // Allow exit animation to complete
            SoniqueToastManager.dismiss()
        } else {
            isVisible = false
        }
    }

    if (activeToast != null) {
        val topStatusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val calculatedTopPadding = if (topStatusBarInset > 0.dp) topStatusBarInset + 8.dp else 42.dp

        Popup(
            alignment = Alignment.TopCenter,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                clippingEnabled = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = calculatedTopPadding, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -it - 50 },
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(tween(180)) + scaleIn(
                        initialScale = 0.88f,
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { -it - 50 },
                        animationSpec = tween(220, easing = FastOutLinearInEasing)
                    ) + fadeOut(tween(180)) + scaleOut(targetScale = 0.88f)
                ) {
                    activeToast?.let { toast ->
                        SoniqueToastPill(
                            toast = toast,
                            onDismiss = {
                                isVisible = false
                                SoniqueToastManager.dismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoniqueToastPill(
    toast: SoniqueToastData,
    onDismiss: () -> Unit,
) {
    val pillShape = RoundedCornerShape(26.dp)

    val (iconRes, iconTint, iconBg) = when (toast.iconType) {
        ToastIconType.LIKE -> Triple(
            Res.drawable.favorite,
            Color(0xFFFF3B5C),
            Color(0x30FF3B5C)
        )
        ToastIconType.UNLIKE -> Triple(
            Res.drawable.favorite_border,
            Color(0xFFB4B7C4),
            Color(0x22FFFFFF)
        )
        ToastIconType.PLAYLIST -> Triple(
            Res.drawable.baseline_playlist_add_24,
            Color(0xFFAB7BFF),
            Color(0x30AB7BFF)
        )
        ToastIconType.DOWNLOAD -> Triple(
            Res.drawable.baseline_downloaded,
            Color(0xFF00E5FF),
            Color(0x3000E5FF)
        )
        ToastIconType.ERROR -> Triple(
            Res.drawable.baseline_error_outline_24,
            Color(0xFFFFAB00),
            Color(0x30FFAB00)
        )
        ToastIconType.SUCCESS -> Triple(
            Res.drawable.metro_check,
            Color(0xFF00E676),
            Color(0x3000E676)
        )
        ToastIconType.INFO -> Triple(
            Res.drawable.baseline_music_note_24,
            Color(0xFFFFFFFF),
            Color(0x22FFFFFF)
        )
    }

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .widthIn(min = 180.dp, max = 390.dp)
            .shadow(
                elevation = 16.dp,
                shape = pillShape,
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .clip(pillShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1F222D),
                        Color(0xFF121319),
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = pillShape
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    if (dragAmount < -8f) {
                        change.consume()
                        onDismiss()
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(Modifier.width(11.dp))

            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = toast.title,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!toast.subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(1.5.dp))
                    Text(
                        text = toast.subtitle,
                        color = Color(0xFFBCC2D1),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(4.dp))
        }
    }
}
