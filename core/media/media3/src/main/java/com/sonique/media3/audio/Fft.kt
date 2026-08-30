package com.sonique.media3.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radix-2 complex FFT of fixed size, transforming in place.
 */
class Fft(
    val size: Int,
) {
    private val twiddleCos = DoubleArray(size / 2)
    private val twiddleSin = DoubleArray(size / 2)
    private val reversed = IntArray(size)

    init {
        require(size > 0 && size and (size - 1) == 0) {
            "FFT size must be a positive power of two, was $size"
        }
        for (k in 0 until size / 2) {
            val angle = 2.0 * PI * k / size
            twiddleCos[k] = cos(angle)
            twiddleSin[k] = sin(angle)
        }
        var bits = 0
        while (1 shl bits < size) {
            bits++
        }
        for (index in 0 until size) {
            reversed[index] = Integer.reverse(index) ushr (32 - bits)
        }
    }

    fun forward(
        real: DoubleArray,
        imaginary: DoubleArray,
    ) {
        transform(real, imaginary, FORWARD_SIGN)
    }

    fun inverse(
        real: DoubleArray,
        imaginary: DoubleArray,
    ) {
        transform(real, imaginary, INVERSE_SIGN)
        val scale = 1.0 / size
        for (index in 0 until size) {
            real[index] *= scale
            imaginary[index] *= scale
        }
    }

    private fun transform(
        real: DoubleArray,
        imaginary: DoubleArray,
        sign: Double,
    ) {
        require(real.size >= size && imaginary.size >= size) {
            "FFT buffers must hold at least $size points"
        }

        for (index in 0 until size) {
            val target = reversed[index]
            if (target > index) {
                val tempReal = real[index]
                real[index] = real[target]
                real[target] = tempReal
                val tempImaginary = imaginary[index]
                imaginary[index] = imaginary[target]
                imaginary[target] = tempImaginary
            }
        }

        var half = 1
        while (half < size) {
            val twiddleStep = size / (half * 2)
            var blockStart = 0
            while (blockStart < size) {
                var lower = blockStart
                var twiddle = 0
                while (lower < blockStart + half) {
                    val upper = lower + half
                    val cosine = twiddleCos[twiddle]
                    val sine = sign * twiddleSin[twiddle]
                    val productReal = real[upper] * cosine - imaginary[upper] * sine
                    val productImaginary = real[upper] * sine + imaginary[upper] * cosine
                    real[upper] = real[lower] - productReal
                    imaginary[upper] = imaginary[lower] - productImaginary
                    real[lower] += productReal
                    imaginary[lower] += productImaginary
                    lower++
                    twiddle += twiddleStep
                }
                blockStart += half * 2
            }
            half *= 2
        }
    }

    private companion object {
        const val FORWARD_SIGN = -1.0
        const val INVERSE_SIGN = 1.0
    }
}
