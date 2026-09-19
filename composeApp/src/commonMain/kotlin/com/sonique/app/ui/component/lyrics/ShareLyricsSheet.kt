package com.sonique.app.ui.component.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonique.app.Platform
import com.sonique.app.expect.saveImageToDevice
import com.sonique.app.expect.shareImage
import com.sonique.app.expect.ui.rememberSaveImagePermission
import com.sonique.app.expect.ui.toPngByteArray
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.capture.capturable
import com.sonique.app.ui.component.capture.rememberCaptureController
import com.sonique.app.ui.component.SoniqueToastManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.share_lyrics
import sonique.composeapp.generated.resources.share_lyrics_background
import sonique.composeapp.generated.resources.share_lyrics_continue
import sonique.composeapp.generated.resources.share_lyrics_max_reached
import sonique.composeapp.generated.resources.share_lyrics_permission_denied
import sonique.composeapp.generated.resources.share_lyrics_save
import sonique.composeapp.generated.resources.share_lyrics_save_failed
import sonique.composeapp.generated.resources.share_lyrics_saved
import sonique.composeapp.generated.resources.share_lyrics_saved_desktop
import sonique.composeapp.generated.resources.share_lyrics_select_title
import sonique.composeapp.generated.resources.share_lyrics_selected_count
import sonique.composeapp.generated.resources.share_lyrics_share_action
import sonique.composeapp.generated.resources.share_lyrics_share_failed
import kotlin.random.Random

internal fun Modifier.verticalFadeEdges(
    topFade: Dp,
    bottomFade: Dp,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val topPx = topFade.toPx().coerceAtMost(size.height / 2f)
            val bottomPx = bottomFade.toPx().coerceAtMost(size.height / 2f)
            val topStop = if (size.height > 0f) topPx / size.height else 0f
            val bottomStop = if (size.height > 0f) 1f - bottomPx / size.height else 1f
            drawRect(
                brush =
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        topStop to Color.Black,
                        bottomStop to Color.Black,
                        1f to Color.Transparent,
                    ),
                blendMode = BlendMode.DstIn,
            )
        }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLyricsSheet(
    lines: List<String>,
    songTitle: String,
    artistName: String,
    artwork: ImageBitmap?,
    seedColor: Color,
    initialLineIndex: Int,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val selection = remember { ShareLyricsSelection(initialLineIndex.takeIf { it in lines.indices }) }
    var showPreview by remember { mutableStateOf(false) }
    var cardBackground by remember(seedColor) { mutableStateOf(seedColor) }
    var busy by remember { mutableStateOf(false) }

    val captureController = rememberCaptureController()

    val surfaceBrush =
        remember(seedColor) {
            Brush.verticalGradient(listOf(seedColor, lerp(seedColor, Color.Black, 0.82f)))
        }
    val content = seedColor.shareCardContentColor()
    val onFilled = seedColor.shareTintOn(content)

    val limitMessage = stringResource(Res.string.share_lyrics_max_reached, MAX_SHARE_LYRIC_LINES)
    val savedMessage =
        stringResource(
            if (getPlatform() == Platform.Desktop) Res.string.share_lyrics_saved_desktop else Res.string.share_lyrics_saved,
        )
    val saveFailedMessage = stringResource(Res.string.share_lyrics_save_failed)
    val shareFailedMessage = stringResource(Res.string.share_lyrics_share_failed)
    val permissionDeniedMessage = stringResource(Res.string.share_lyrics_permission_denied)
    val chooserTitle = stringResource(Res.string.share_lyrics)

    val baseFileName =
        remember(songTitle) {
            val stem = songTitle.ifBlank { "lyrics" }.take(32).map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
            "Sonique_${stem}"
        }

    val savePermission =
        rememberSaveImagePermission { granted ->
            if (!granted) {
                SoniqueToastManager.show(permissionDeniedMessage)
                busy = false
                return@rememberSaveImagePermission
            }
            scope.launch {
                try {
                    val currentFileName = "${baseFileName}_${Random.nextInt(100_000, 999_999)}.png"
                    val bytes = captureController.captureAsync().await().toPngByteArray()
                    val ok = bytes != null && saveImageToDevice(bytes, currentFileName)
                    SoniqueToastManager.show(if (ok) savedMessage else saveFailedMessage)
                } catch (e: Throwable) {
                    SoniqueToastManager.show(saveFailedMessage)
                } finally {
                    busy = false
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .6f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (showPreview) Modifier else Modifier.fillMaxHeight(0.94f))
                    .background(surfaceBrush),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                ShareLyricsSheetHeader(
                    title =
                        if (showPreview) {
                            stringResource(Res.string.share_lyrics)
                        } else {
                            stringResource(Res.string.share_lyrics_select_title)
                        },
                    subtitle =
                        if (showPreview) null else stringResource(Res.string.share_lyrics_selected_count, selection.count),
                    content = content,
                    onClose = { if (showPreview) showPreview = false else onDismiss() },
                )

                if (showPreview) {
                    ShareLyricsPreview(
                        selection = selection,
                        lines = lines,
                        songTitle = songTitle,
                        artistName = artistName,
                        artwork = artwork,
                        seedColor = seedColor,
                        cardBackground = cardBackground,
                        onSelectBackground = { cardBackground = it },
                        content = content,
                        onFilled = onFilled,
                        captureModifier = Modifier.capturable(captureController),
                        onSave = {
                            if (!busy) {
                                busy = true
                                savePermission.requestIfNeeded()
                            }
                        },
                        onShare = {
                            if (!busy) {
                                busy = true
                                scope.launch {
                                    try {
                                        val currentFileName = "${baseFileName}_${Random.nextInt(100_000, 999_999)}.png"
                                        val bytes = captureController.captureAsync().await().toPngByteArray()
                                        val ok = bytes != null && shareImage(bytes, currentFileName, chooserTitle)
                                        if (!ok) SoniqueToastManager.show(shareFailedMessage)
                                    } catch (e: Throwable) {
                                        SoniqueToastManager.show(shareFailedMessage)
                                    } finally {
                                        busy = false
                                    }
                                }
                            }
                        },
                    )
                } else {
                    ShareLyricsPicker(
                        lines = lines,
                        selection = selection,
                        onFilled = onFilled,
                        content = content,
                        initialLineIndex = initialLineIndex,
                        onLimitReached = { SoniqueToastManager.show(limitMessage) },
                        onContinue = { showPreview = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareLyricsSheetHeader(
    title: String,
    subtitle: String?,
    content: Color,
    onClose: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, color = content, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, color = content.copy(alpha = 0.62f), fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun ShareLyricsPicker(
    lines: List<String>,
    selection: ShareLyricsSelection,
    onFilled: Color,
    content: Color,
    initialLineIndex: Int,
    onLimitReached: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = (initialLineIndex - 1).coerceIn(0, maxOf(0, lines.lastIndex)),
        )

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().verticalFadeEdges(topFade = 24.dp, bottomFade = 96.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
        ) {
            itemsIndexed(lines) { index, text ->
                if (text.isBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    ShareLyricsLineRow(
                        text = text,
                        selected = selection.isSelected(index),
                        onFilled = onFilled,
                        content = content,
                        onClick = { selection.toggle(index, onLimitReached) },
                    )
                }
            }
        }

        ShareLyricsPill(
            text = stringResource(Res.string.share_lyrics_continue),
            container = content,
            label = onFilled,
            enabled = !selection.isEmpty,
            onClick = onContinue,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun ShareLyricsLineRow(
    text: String,
    selected: Boolean,
    onFilled: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) content else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            color = if (selected) onFilled else content.copy(alpha = 0.5f),
            fontSize = 18.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ShareLyricsPreview(
    selection: ShareLyricsSelection,
    lines: List<String>,
    songTitle: String,
    artistName: String,
    artwork: ImageBitmap?,
    seedColor: Color,
    cardBackground: Color,
    onSelectBackground: (Color) -> Unit,
    content: Color,
    onFilled: Color,
    captureModifier: Modifier,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val selectedLines =
        remember(selection.range, lines) {
            selection.range
                ?.mapNotNull { lines.getOrNull(it)?.takeIf(String::isNotBlank) }
                .orEmpty()
        }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        ShareLyricsCard(
            lines = selectedLines,
            songTitle = songTitle,
            artistName = artistName,
            artwork = artwork,
            background = cardBackground,
            modifier = captureModifier,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ShareLyricsPalette(
            seedColor = seedColor,
            selected = cardBackground,
            content = content,
            onSelect = onSelectBackground,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShareLyricsPill(
                text = stringResource(Res.string.share_lyrics_save),
                icon = Icons.Default.Download,
                container = Color.Transparent,
                label = content,
                outlined = true,
                onClick = onSave,
            )
            ShareLyricsPill(
                text = stringResource(Res.string.share_lyrics_share_action),
                icon = Icons.Default.Share,
                container = content,
                label = onFilled,
                onClick = onShare,
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun ShareLyricsPill(
    text: String,
    container: Color,
    label: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    outlined: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(if (enabled) container else container.copy(alpha = 0.35f))
                .then(
                    if (outlined) Modifier.border(1.dp, label.copy(alpha = 0.45f), CircleShape) else Modifier,
                ).clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 28.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) label else label.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = if (enabled) label else label.copy(alpha = 0.5f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ShareLyricsPalette(
    seedColor: Color,
    selected: Color,
    content: Color,
    onSelect: (Color) -> Unit,
) {
    val swatches =
        remember(seedColor) {
            listOf(
                seedColor,
                Color(0xFF1F1F1F),
                Color(0xFF2F5D50),
                Color(0xFF7A3B3B),
                Color(0xFFE8E2D4),
            )
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(Res.string.share_lyrics_background),
            color = content.copy(alpha = 0.6f),
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            swatches.forEach { swatch ->
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (swatch == selected) 2.dp else 0.dp,
                                color = if (swatch == selected) content else Color.Transparent,
                                shape = CircleShape,
                            ).clickable { onSelect(swatch) },
                )
            }
        }
    }
}
