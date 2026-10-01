package com.sonique.app.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sonique.app.ui.theme.typo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.pitch
import sonique.composeapp.generated.resources.playback_speed
import sonique.composeapp.generated.resources.set
import sonique.composeapp.generated.resources.sleep_minutes
import sonique.composeapp.generated.resources.sleep_timer_set_error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedPitchBottomSheet(
    onDismiss: () -> Unit,
    playbackSpeed: Float,
    pitch: Int,
    onSet: (playbackSpeed: Float, pitch: Int) -> Unit,
) {
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 10.dp),
            ) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                    colors =
                        CardDefaults.cardColors().copy(
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.playback_speed) + " ${playbackSpeed}x",
                    style = typo().labelSmall,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Slider(
                    value = playbackSpeed,
                    onValueChange = {
                        onSet(it, pitch)
                    },
                    modifier = Modifier,
                    enabled = true,
                    valueRange = 0.25f..2f,
                    steps = 13,
                    onValueChangeFinished = {},
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = stringResource(Res.string.pitch) + " $pitch",
                    style = typo().labelSmall,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Slider(
                    value = pitch.toFloat(),
                    onValueChange = {
                        onSet(playbackSpeed, it.toInt())
                    },
                    modifier = Modifier,
                    enabled = true,
                    valueRange = -12f..12f,
                    steps = 23,
                    onValueChangeFinished = {},
                )
                EndOfModalBottomSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    onDismiss: () -> Unit,
    onSetTimer: (minutes: Int) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    var minutes by rememberSaveable { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                    colors =
                        CardDefaults.cardColors().copy(
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                val sleepIntervals = listOf(5, 10, 15, 20, 25, 30, 45, 60)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sleepIntervals.forEach { interval ->
                        Chip(
                            text = interval.toString(),
                            isSelected = minutes == interval,
                            onClick = {
                                minutes = interval
                                onSetTimer(interval)
                                coroutineScope.launch {
                                    modelBottomSheetState.hide()
                                    onDismiss()
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(15.dp))
                Text(text = stringResource(Res.string.sleep_minutes), style = typo().labelSmall)
                Spacer(modifier = Modifier.height(5.dp))
                OutlinedTextField(
                    value = if (minutes == 0) "" else minutes.toString(),
                    placeholder = { Text("0", style = typo().bodyMedium.copy(color = Color.Gray)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    onValueChange = { value ->
                        if (value.isEmpty()) {
                            minutes = 0
                        } else if (value.all { it.isDigit() } && value.length < 5) {
                            minutes = value.toInt()
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(modifier = Modifier.height(5.dp))
                val sleepTimerSetErrorString = stringResource(Res.string.sleep_timer_set_error)
                TextButton(
                    onClick = {
                        if (minutes > 0) {
                            onSetTimer(minutes)
                            coroutineScope.launch {
                                modelBottomSheetState.hide()
                                onDismiss()
                            }
                        } else {
                            SoniqueToastManager.show(sleepTimerSetErrorString)
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                ) {
                    Text(text = stringResource(Res.string.set), style = typo().labelSmall)
                }
                Spacer(modifier = Modifier.height(5.dp))
                EndOfModalBottomSheet()
            }
        }
    }
}
