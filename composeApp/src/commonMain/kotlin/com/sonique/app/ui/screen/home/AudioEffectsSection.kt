package com.sonique.app.ui.screen.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.domain.data.player.DelayEffect
import com.sonique.domain.data.player.ReverbPreset
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.delay_preset_dub
import sonique.composeapp.generated.resources.delay_preset_quarter
import sonique.composeapp.generated.resources.delay_preset_slapback
import sonique.composeapp.generated.resources.effect_feedback
import sonique.composeapp.generated.resources.effect_mix
import sonique.composeapp.generated.resources.effect_time
import sonique.composeapp.generated.resources.ok
import sonique.composeapp.generated.resources.reverb_help_cathedral
import sonique.composeapp.generated.resources.reverb_help_damping
import sonique.composeapp.generated.resources.reverb_help_damping_meaning
import sonique.composeapp.generated.resources.reverb_help_hall
import sonique.composeapp.generated.resources.reverb_help_intro
import sonique.composeapp.generated.resources.reverb_help_mix
import sonique.composeapp.generated.resources.reverb_help_plate
import sonique.composeapp.generated.resources.reverb_help_predelay
import sonique.composeapp.generated.resources.reverb_help_predelay_meaning
import sonique.composeapp.generated.resources.reverb_help_room
import sonique.composeapp.generated.resources.reverb_help_rt60
import sonique.composeapp.generated.resources.reverb_help_rt60_meaning
import sonique.composeapp.generated.resources.reverb_help_title
import sonique.composeapp.generated.resources.reverb_preset_cathedral
import sonique.composeapp.generated.resources.reverb_preset_hall
import sonique.composeapp.generated.resources.reverb_preset_plate
import sonique.composeapp.generated.resources.reverb_preset_room
import kotlin.math.abs
import kotlin.math.roundToInt

private val EFFECT_READOUT_WIDTH = 64.dp
private const val DELAY_TIME_STEP_MS = 10
private const val PRESET_MATCH_TOLERANCE = 0.001f

private data class DelayPresetOption(
    val label: StringResource,
    val timeMs: Int,
    val feedback: Float,
    val mix: Float,
)

private val DELAY_PRESETS: List<DelayPresetOption> =
    listOf(
        DelayPresetOption(label = Res.string.delay_preset_slapback, timeMs = 80, feedback = 0.3f, mix = 0.25f),
        DelayPresetOption(label = Res.string.delay_preset_quarter, timeMs = 400, feedback = 0.45f, mix = 0.3f),
        DelayPresetOption(label = Res.string.delay_preset_dub, timeMs = 600, feedback = 0.7f, mix = 0.4f),
    )

private fun delayPresetFor(
    timeMs: Int,
    feedback: Float,
    mix: Float,
): DelayPresetOption? =
    DELAY_PRESETS.firstOrNull {
        it.timeMs == timeMs &&
            abs(it.feedback - feedback) < PRESET_MATCH_TOLERANCE &&
            abs(it.mix - mix) < PRESET_MATCH_TOLERANCE
    }

@Composable
fun DelaySection(viewModel: SettingsViewModel) {
    val timeMs by viewModel.delayTimeMs.collectAsStateWithLifecycle()
    val feedback by viewModel.delayFeedback.collectAsStateWithLifecycle()
    val mix by viewModel.delayMix.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.getAudioEffects() }

    val activePreset = remember(timeMs, feedback, mix) { delayPresetFor(timeMs, feedback, mix) }

    EffectCard {
        EffectPresetRow {
            DELAY_PRESETS.forEach { preset ->
                FilterChip(
                    selected = preset == activePreset,
                    onClick = { viewModel.applyDelayPreset(preset.timeMs, preset.feedback, preset.mix) },
                    label = { Text(stringResource(preset.label), style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
        EffectSlider(
            label = stringResource(Res.string.effect_time),
            value = timeMs.toFloat(),
            valueRange = DelayEffect.MIN_TIME_MS.toFloat()..DelayEffect.MAX_TIME_MS.toFloat(),
            snap = { (it / DELAY_TIME_STEP_MS).roundToInt() * DELAY_TIME_STEP_MS.toFloat() },
            readout = { "${it.roundToInt()} ms" },
            onCommit = { viewModel.setDelayTimeMs(it.roundToInt()) },
            modifier = Modifier.padding(top = 12.dp),
        )
        EffectSlider(
            label = stringResource(Res.string.effect_feedback),
            value = feedback,
            valueRange = 0f..DelayEffect.MAX_FEEDBACK,
            readout = { "${(it * 100).roundToInt()}%" },
            onCommit = { viewModel.setDelayFeedback(it) },
            modifier = Modifier.padding(top = 12.dp),
        )
        EffectSlider(
            label = stringResource(Res.string.effect_mix),
            value = mix,
            valueRange = 0f..1f,
            readout = { "${(it * 100).roundToInt()}%" },
            onCommit = { viewModel.setDelayMix(it) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
fun ReverbSection(viewModel: SettingsViewModel) {
    val preset by viewModel.reverbPreset.collectAsStateWithLifecycle()
    val mix by viewModel.reverbMix.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.getAudioEffects() }

    var showHelp by remember { mutableStateOf(false) }

    EffectCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                EffectPresetRow {
                    ReverbPreset.entries.forEach { option ->
                        FilterChip(
                            selected = option == preset,
                            onClick = { viewModel.setReverbPreset(option) },
                            label = { Text(stringResource(reverbPresetLabel(option)), style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
            }
            IconButton(
                onClick = { showHelp = true },
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(Res.string.reverb_help_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        EffectSlider(
            label = stringResource(Res.string.effect_mix),
            value = mix,
            valueRange = 0f..1f,
            readout = { "${(it * 100).roundToInt()}%" },
            onCommit = { viewModel.setReverbMix(it) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }

    if (showHelp) {
        ReverbHelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun ReverbHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.reverb_help_title),
                style = MaterialTheme.typography.titleSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = stringResource(Res.string.reverb_help_intro), style = MaterialTheme.typography.bodySmall)
                ReverbPreset.entries.forEach { preset ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(reverbPresetLabel(preset)),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text =
                                stringResource(Res.string.reverb_help_rt60, formatSeconds(preset.rt60Ms)) + " · " +
                                    stringResource(Res.string.reverb_help_predelay, "${preset.preDelayMs} ms") + " · " +
                                    stringResource(Res.string.reverb_help_damping, formatKiloHertz(preset.dampingHz)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(text = stringResource(reverbPresetHelp(preset)), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(text = stringResource(Res.string.reverb_help_rt60_meaning), style = MaterialTheme.typography.bodySmall)
                Text(text = stringResource(Res.string.reverb_help_predelay_meaning), style = MaterialTheme.typography.bodySmall)
                Text(text = stringResource(Res.string.reverb_help_damping_meaning), style = MaterialTheme.typography.bodySmall)
                Text(text = stringResource(Res.string.reverb_help_mix), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.ok))
            }
        },
    )
}

private fun reverbPresetLabel(preset: ReverbPreset): StringResource =
    when (preset) {
        ReverbPreset.ROOM -> Res.string.reverb_preset_room
        ReverbPreset.HALL -> Res.string.reverb_preset_hall
        ReverbPreset.PLATE -> Res.string.reverb_preset_plate
        ReverbPreset.CATHEDRAL -> Res.string.reverb_preset_cathedral
    }

private fun reverbPresetHelp(preset: ReverbPreset): StringResource =
    when (preset) {
        ReverbPreset.ROOM -> Res.string.reverb_help_room
        ReverbPreset.HALL -> Res.string.reverb_help_hall
        ReverbPreset.PLATE -> Res.string.reverb_help_plate
        ReverbPreset.CATHEDRAL -> Res.string.reverb_help_cathedral
    }

private fun formatSeconds(ms: Int): String {
    val tenths = (ms + 50) / 100
    return "${tenths / 10}.${tenths % 10} s"
}

private fun formatKiloHertz(hz: Int): String {
    val tenths = (hz + 50) / 100
    return if (tenths % 10 == 0) "${tenths / 10} kHz" else "${tenths / 10}.${tenths % 10} kHz"
}

@Composable
internal fun EffectCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            content = content,
        )
    }
}

@Composable
internal fun EffectPresetRow(chips: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips()
    }
}

@Composable
internal fun EffectSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    readout: (Float) -> String,
    onCommit: (Float) -> Unit,
    modifier: Modifier = Modifier,
    snap: (Float) -> Float = { it },
) {
    var draft by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(value) { draft = null }
    val shown = draft ?: value

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Slider(
                value = shown,
                onValueChange = { draft = snap(it).coerceIn(valueRange.start, valueRange.endInclusive) },
                onValueChangeFinished = { draft?.let(onCommit) },
                valueRange = valueRange,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = readout(shown),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(EFFECT_READOUT_WIDTH),
            )
        }
    }
}
