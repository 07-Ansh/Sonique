package com.sonique.app.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.typo
import com.sonique.app.ui.theme.white
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.domain.data.model.download.DownloadProgress
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.album
import sonique.composeapp.generated.resources.artists
import sonique.composeapp.generated.resources.baseline_keyboard_arrow_down_24
import sonique.composeapp.generated.resources.bitrate
import sonique.composeapp.generated.resources.bpm
import sonique.composeapp.generated.resources.codec
import sonique.composeapp.generated.resources.description
import sonique.composeapp.generated.resources.download_speed
import sonique.composeapp.generated.resources.download_this_song_video_file_to_your_device
import sonique.composeapp.generated.resources.downloaded
import sonique.composeapp.generated.resources.downloading
import sonique.composeapp.generated.resources.downloading_audio
import sonique.composeapp.generated.resources.downloading_video
import sonique.composeapp.generated.resources.error_occurred
import sonique.composeapp.generated.resources.extract_source
import sonique.composeapp.generated.resources.itag
import sonique.composeapp.generated.resources.key
import sonique.composeapp.generated.resources.like
import sonique.composeapp.generated.resources.like_and_dislike
import sonique.composeapp.generated.resources.merging_audio_and_video
import sonique.composeapp.generated.resources.mime_type
import sonique.composeapp.generated.resources.no_description
import sonique.composeapp.generated.resources.now_playing_upper
import sonique.composeapp.generated.resources.ok
import sonique.composeapp.generated.resources.plays
import sonique.composeapp.generated.resources.scale
import sonique.composeapp.generated.resources.title
import sonique.composeapp.generated.resources.to_download_folder
import sonique.composeapp.generated.resources.unknown
import sonique.composeapp.generated.resources.youtube_url

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoPlayerBottomSheet(
    onDismiss: () -> Unit,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val windowInsets = WindowInsets.systemBars
    var swipeEnabled by rememberSaveable { mutableStateOf(true) }
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = {
                swipeEnabled
            },
        )
    val scrollState = rememberScrollState()

    LaunchedEffect(true) {
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collectLatest {
                swipeEnabled = scrollState.value == 0
            }
    }

    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val songEntity by sharedViewModel.nowPlayingState.map { it?.songEntity }.collectAsState(null)
    val format by sharedViewModel.format.collectAsState(null)
    val extractSource by sharedViewModel.extractSource.collectAsState()
    val downloadProgress by sharedViewModel.downloadFileProgress.collectAsStateWithLifecycle()

    if (downloadProgress != DownloadProgress.INIT) {
        Box(modifier = Modifier.fillMaxSize()) {
            BasicAlertDialog(
                onDismissRequest = { },
                modifier = Modifier.wrapContentSize(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                    shadowElevation = 1.dp,
                ) {
                    Column(
                        Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 20.dp,
                        ),
                    ) {
                        Text(
                            stringResource(Res.string.downloading),
                            style = typo().headlineMedium,
                        )
                        Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!downloadProgress.isDone && !downloadProgress.isError) {
                                CircularProgressIndicator()
                                Spacer(Modifier.size(15.dp))
                            }
                            Crossfade(downloadProgress) {
                                if (it.isMerging) {
                                    Text(
                                        text = stringResource(Res.string.merging_audio_and_video),
                                        modifier = Modifier.padding(vertical = 5.dp),
                                        style = typo().bodyMedium,
                                    )
                                } else if (it.isError) {
                                    Column {
                                        Text(
                                            text = stringResource(Res.string.error_occurred),
                                            modifier = Modifier.padding(vertical = 5.dp),
                                            style = typo().bodyMedium,
                                        )
                                        Text(
                                            text = downloadProgress.errorMessage,
                                            modifier = Modifier.padding(bottom = 5.dp),
                                            maxLines = 2,
                                            style = typo().bodyMedium,
                                        )
                                    }
                                } else if (it.isDone) {
                                    Text(
                                        text = stringResource(Res.string.downloaded) + stringResource(Res.string.to_download_folder),
                                        modifier = Modifier.padding(vertical = 5.dp),
                                        style = typo().bodyMedium,
                                    )
                                } else {
                                    Column {
                                        if (it.audioDownloadProgress != 0f) {
                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.downloading_audio,
                                                        (downloadProgress.audioDownloadProgress * 100).toString() + "%",
                                                    ),
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                style = typo().bodyMedium,
                                            )
                                        }
                                        if (it.videoDownloadProgress != 0f) {
                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.downloading_video,
                                                        (downloadProgress.videoDownloadProgress * 100).toString() + "%",
                                                    ),
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                style = typo().bodyMedium,
                                            )
                                        }
                                        if (downloadProgress.downloadSpeed != 0) {
                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.download_speed,
                                                        downloadProgress.downloadSpeed.toString() + " kb/s",
                                                    ),
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                style = typo().bodyMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Crossfade(downloadProgress) {
                            if (it.isError) {
                                Column {
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(onClick = {
                                        sharedViewModel.downloadFileDone()
                                    }) { Text(stringResource(Res.string.ok)) }
                                }
                            }
                            if (it.isDone) {
                                Column {
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(onClick = {
                                        sharedViewModel.downloadFileDone()
                                    }) { Text(stringResource(Res.string.ok)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        containerColor = md_theme_dark_background,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = md_theme_dark_background.copy(alpha = .5f),
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            shape = RectangleShape,
            colors = CardDefaults.cardColors().copy(containerColor = md_theme_dark_background),
        ) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(scrollState)
                        .padding(
                            top =
                                with(localDensity) {
                                    windowInsets.getTop(localDensity).toDp()
                                },
                        ),
            ) {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors =
                        TopAppBarDefaults.topAppBarColors().copy(
                            containerColor = Color.Transparent,
                        ),
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.now_playing_upper),
                                style = typo().bodyMedium,
                                color = Color.White,
                            )
                            Text(
                                text = screenDataState.nowPlayingTitle,
                                style = typo().labelMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(align = Alignment.CenterVertically).focusable(),
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.baseline_keyboard_arrow_down_24),
                                contentDescription = "",
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier.size(48.dp),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.title),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = screenDataState.nowPlayingTitle,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .padding(horizontal = 10.dp).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.artists),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = screenDataState.artistName,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.album),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = songEntity?.albumName ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.itag),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = format?.itag?.toString() ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.mime_type),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = format?.mimeType ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.codec),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = format?.codecs ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.bitrate),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = format?.bitrate?.toString() ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.bpm),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = format?.bpm?.toString() ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.key),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = format?.musicKey ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.scale),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = format?.keyScale ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.extract_source),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = extractSource ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.plays),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = screenDataState.songInfoData?.viewCount?.toString() ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.like),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text =
                        stringResource(
                            Res.string.like_and_dislike,
                            screenDataState.songInfoData?.like ?: 0,
                            screenDataState.songInfoData?.dislike ?: 0,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.description),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = screenDataState.songInfoData?.description ?: stringResource(Res.string.no_description),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.youtube_url),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text =
                        buildAnnotatedString {
                            withLink(
                                LinkAnnotation.Url(
                                    "https://music.youtube.com/watch?v=${songEntity?.videoId}",
                                    TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline)),
                                ),
                            ) {
                                append("https://music.youtube.com/watch?v=${songEntity?.videoId}")
                            }
                        },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically).focusable(),
                    style = typo().bodyMedium,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(
                    enabled = screenDataState.bitmap != null,
                    onClick = {
                        sharedViewModel.downloadFile(
                            bitmap = screenDataState.bitmap ?: return@OutlinedButton,
                        )
                    },
                    modifier =
                        Modifier
                            .wrapContentSize()
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 10.dp),
                ) {
                    Text(text = stringResource(Res.string.download_this_song_video_file_to_your_device))
                }
                Spacer(modifier = Modifier.height(10.dp))

                EndOfModalBottomSheet()
            }
        }
    }
}
