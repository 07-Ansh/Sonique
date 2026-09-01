package com.sonique.media3.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.sonique.domain.data.player.AudioEffects
import com.sonique.domain.data.player.ReverbImpulseResponse
import com.sonique.domain.data.player.ReverbPreset
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Media3 [AudioProcessor] applying convolution reverb.
 *
 * Impulse responses are generated via [ReverbImpulseResponse] and convolved in streaming blocks
 * using [PartitionedConvolver], one instance per audio channel.
 *
 * Input frames are buffered into a dry FIFO queue [dry] and dequeued in lock-step with
 * [PartitionedConvolver] output to ensure sample-aligned wet/dry blending without comb filtering.
 */
@UnstableApi
class ConvolutionReverbAudioProcessor(
    private val effects: () -> AudioEffects,
) : BaseAudioProcessor() {
    private var sampleRate = 0
    private var channelCount = 0

    private var appliedEffects: AudioEffects? = null
    private var appliedPreset: ReverbPreset? = null
    private var impulseSampleRate = 0
    private var mix = 0f
    private var bypass = true

    private var convolvers: Array<PartitionedConvolver> = emptyArray()

    private var dry = ShortArray(0)
    private var dryHead = 0
    private var dryTail = 0

    private var scratch: Array<FloatArray> = emptyArray()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        convolvers = emptyArray()
        scratch = emptyArray()
        appliedEffects = null
        appliedPreset = null
        impulseSampleRate = 0
        clearDry()
        bypass = true
        return inputAudioFormat
    }

    override fun onFlush() {
        for (convolver in convolvers) {
            convolver.flush()
        }
        clearDry()
    }

    override fun onReset() {
        convolvers = emptyArray()
        scratch = emptyArray()
        appliedEffects = null
        appliedPreset = null
        impulseSampleRate = 0
        clearDry()
        sampleRate = 0
        channelCount = 0
        mix = 0f
        bypass = true
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        syncEffects()

        if (bypass) {
            val backlog = dryTail - dryHead
            val output = replaceOutputBuffer(backlog * SHORT_BYTES + remaining)
            while (dryHead < dryTail) {
                output.putShort(dry[dryHead])
                dryHead++
            }
            clearDry()
            output.put(inputBuffer)
            output.flip()
            return
        }

        inputBuffer.order(ByteOrder.nativeOrder())
        val frames = remaining / (channelCount * SHORT_BYTES)
        ensureScratch(frames + PartitionedConvolver.PARTITION_SIZE)
        ensureDryCapacity(frames * channelCount)

        for (frame in 0 until frames) {
            for (channel in 0 until channelCount) {
                val sample = inputBuffer.short
                scratch[channel][frame] = sample.toFloat()
                dry[dryTail] = sample
                dryTail++
            }
        }

        while (inputBuffer.hasRemaining()) {
            inputBuffer.get()
        }

        for (channel in 0 until channelCount) {
            convolvers[channel].write(scratch[channel], 0, frames)
        }

        val produced = convolvers[0].availableOutput
        if (produced == 0) {
            replaceOutputBuffer(0).flip()
            return
        }
        for (channel in 0 until channelCount) {
            val moved = convolvers[channel].read(scratch[channel], 0, produced)
            check(moved == produced) { "channel $channel returned $moved of $produced finished samples" }
        }
        blendIntoOutput(produced)
    }

    override fun onQueueEndOfStream() {
        if (bypass || convolvers.isEmpty()) return
        val outstanding = dryFrames()
        if (outstanding == 0) return

        val padding = PartitionedConvolver.PARTITION_SIZE - outstanding
        ensureScratch(PartitionedConvolver.PARTITION_SIZE)
        for (channel in 0 until channelCount) {
            scratch[channel].fill(0f, 0, padding)
            convolvers[channel].write(scratch[channel], 0, padding)
            val moved = convolvers[channel].read(scratch[channel], 0, PartitionedConvolver.PARTITION_SIZE)
            check(moved == PartitionedConvolver.PARTITION_SIZE) {
                "channel $channel returned $moved samples of a padded block at end of stream"
            }
        }
        blendIntoOutput(outstanding)
    }

    private fun blendIntoOutput(frames: Int) {
        val output = replaceOutputBuffer(frames * channelCount * SHORT_BYTES)
        val wetLevel = mix
        val dryLevel = 1f - mix
        for (frame in 0 until frames) {
            for (channel in 0 until channelCount) {
                val blended = dry[dryHead].toFloat() * dryLevel + scratch[channel][frame] * wetLevel
                dryHead++
                output.putShort(blended.coerceIn(PCM16_MIN, PCM16_MAX).toInt().toShort())
            }
        }
        if (dryHead == dryTail) {
            dryHead = 0
            dryTail = 0
        }
        output.flip()
    }

    private fun syncEffects() {
        val next = effects()
        if (next === appliedEffects) return
        appliedEffects = next

        val reverb = next.reverb
        if (reverb == null || channelCount <= 0 || sampleRate <= 0) {
            enterBypass()
            return
        }
        mix = reverb.mix.coerceIn(0f, 1f)
        if (reverb.preset != appliedPreset || impulseSampleRate != sampleRate) {
            rebuild(reverb.preset)
        }
        bypass = false
    }

    private fun rebuild(preset: ReverbPreset) {
        val impulse = ReverbImpulseResponse.generate(preset, sampleRate)
        val rebuilt = Array(channelCount) { channel ->
            PartitionedConvolver(if (channel == RIGHT_CHANNEL) impulse.right else impulse.left)
        }

        val carried = dryFrames()
        if (carried > 0) {
            ensureScratch(carried)
            for (channel in 0 until channelCount) {
                for (frame in 0 until carried) {
                    scratch[channel][frame] = dry[dryHead + frame * channelCount + channel].toFloat()
                }
                rebuilt[channel].write(scratch[channel], 0, carried)
            }
        }

        convolvers = rebuilt
        appliedPreset = preset
        impulseSampleRate = sampleRate
    }

    private fun enterBypass() {
        if (!bypass) {
            for (convolver in convolvers) {
                convolver.reset()
            }
        }
        bypass = true
    }

    private fun dryFrames(): Int = if (channelCount <= 0) 0 else (dryTail - dryHead) / channelCount

    private fun clearDry() {
        dryHead = 0
        dryTail = 0
    }

    private fun ensureDryCapacity(additional: Int) {
        if (dryTail + additional <= dry.size) return
        val kept = dryTail - dryHead
        if (kept + additional > dry.size) {
            var capacity = maxOf(dry.size, PartitionedConvolver.PARTITION_SIZE)
            while (capacity < kept + additional) {
                capacity *= 2
            }
            val grown = ShortArray(capacity)
            dry.copyInto(grown, destinationOffset = 0, startIndex = dryHead, endIndex = dryTail)
            dry = grown
        } else {
            dry.copyInto(dry, destinationOffset = 0, startIndex = dryHead, endIndex = dryTail)
        }
        dryHead = 0
        dryTail = kept
    }

    private fun ensureScratch(frames: Int) {
        if (scratch.size == channelCount && scratch.isNotEmpty() && scratch[0].size >= frames) return
        scratch = Array(channelCount) { FloatArray(frames) }
    }

    private companion object {
        const val RIGHT_CHANNEL = 1
        const val SHORT_BYTES = 2
        const val PCM16_MIN = -32_768f
        const val PCM16_MAX = 32_767f
    }
}
