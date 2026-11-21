package com.example.btp.ml

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class AudioPreprocessor {

    companion object {
        const val TARGET_SAMPLE_RATE = 16000
        const val FRAME_LENGTH = 400 // 25ms at 16kHz
        const val FRAME_STEP = 160   // 10ms at 16kHz
        const val N_FFT = 512
        const val NUM_MEL_BINS = 80
    }

    /**
     * Normalizes peak amplitude of audio signal to target max magnitude (e.g., 0.95).
     */
    fun normalize(audio: FloatArray, targetPeak: Float = 0.95f): FloatArray {
        if (audio.isEmpty()) return audio
        var maxMagnitude = 0f
        for (sample in audio) {
            val mag = abs(sample)
            if (mag > maxMagnitude) {
                maxMagnitude = mag
            }
        }
        if (maxMagnitude == 0f) return audio.copyOf()
        val scale = targetPeak / maxMagnitude
        return FloatArray(audio.size) { i -> audio[i] * scale }
    }

    /**
     * Trim silence using simple energy thresholding.
     */
    fun trimSilence(audio: FloatArray, thresholdRatio: Float = 0.02f): FloatArray {
        if (audio.isEmpty()) return audio
        var maxEnergy = 0f
        for (sample in audio) {
            val e = abs(sample)
            if (e > maxEnergy) maxEnergy = e
        }
        val threshold = maxEnergy * thresholdRatio

        var startIndex = 0
        while (startIndex < audio.size && abs(audio[startIndex]) < threshold) {
            startIndex++
        }

        var endIndex = audio.size - 1
        while (endIndex > startIndex && abs(audio[endIndex]) < threshold) {
            endIndex--
        }

        if (startIndex >= endIndex) return audio.copyOf()
        return audio.copyOfRange(startIndex, endIndex + 1)
    }

    /**
     * Computes Mel-spectrogram features [numFrames, NUM_MEL_BINS] from 16kHz audio.
     */
    fun extractMelSpectrogram(audio: FloatArray): Array<FloatArray> {
        val normalized = normalize(audio)
        if (normalized.size < FRAME_LENGTH) {
            return Array(1) { FloatArray(NUM_MEL_BINS) }
        }

        val numFrames = (normalized.size - FRAME_LENGTH) / FRAME_STEP + 1
        val hammingWindow = FloatArray(FRAME_LENGTH) { i ->
            (0.54 - 0.46 * cos(2.0 * PI * i / (FRAME_LENGTH - 1))).toFloat()
        }

        val melFilterBank = createMelFilterBank(N_FFT, NUM_MEL_BINS, TARGET_SAMPLE_RATE)
        val spectrogram = Array(numFrames) { FloatArray(NUM_MEL_BINS) }

        for (f in 0 until numFrames) {
            val offset = f * FRAME_STEP
            val fftBufferReal = DoubleArray(N_FFT)
            val fftBufferImag = DoubleArray(N_FFT)

            for (i in 0 until FRAME_LENGTH) {
                fftBufferReal[i] = (normalized[offset + i] * hammingWindow[i]).toDouble()
            }

            fft(fftBufferReal, fftBufferImag)

            val powerSpectrum = FloatArray(N_FFT / 2 + 1)
            for (i in 0..N_FFT / 2) {
                val r = fftBufferReal[i]
                val im = fftBufferImag[i]
                powerSpectrum[i] = ((r * r + im * im) / N_FFT).toFloat()
            }

            for (m in 0 until NUM_MEL_BINS) {
                var melEnergy = 0f
                for (k in powerSpectrum.indices) {
                    melEnergy += powerSpectrum[k] * melFilterBank[m][k]
                }
                spectrogram[f][m] = log10(max(melEnergy.toDouble(), 1e-6)).toFloat()
            }
        }

        return spectrogram
    }

    private fun createMelFilterBank(nFft: Int, numBins: Int, sampleRate: Int): Array<FloatArray> {
        val numSpectrogramBins = nFft / 2 + 1
        val filterBank = Array(numBins) { FloatArray(numSpectrogramBins) }

        fun hzToMel(hz: Double) = 2595.0 * log10(1.0 + hz / 700.0)
        fun melToHz(mel: Double) = 700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0)

        val minMel = hzToMel(0.0)
        val maxMel = hzToMel(sampleRate / 2.0)
        val melPoints = DoubleArray(numBins + 2) { i ->
            minMel + i * (maxMel - minMel) / (numBins + 1)
        }
        val hzPoints = DoubleArray(numBins + 2) { i -> melToHz(melPoints[i]) }
        val binPoints = IntArray(numBins + 2) { i ->
            ((nFft + 1) * hzPoints[i] / sampleRate).toInt()
        }

        for (m in 1..numBins) {
            val left = binPoints[m - 1]
            val center = binPoints[m]
            val right = binPoints[m + 1]

            for (k in left until center) {
                if (center != left && k < numSpectrogramBins) {
                    filterBank[m - 1][k] = (k - left).toFloat() / (center - left)
                }
            }
            for (k in center until right) {
                if (right != center && k < numSpectrogramBins) {
                    filterBank[m - 1][k] = (right - k).toFloat() / (right - center)
                }
            }
        }

        return filterBank
    }

    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        if (n <= 1) return

        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR
                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * PI / len
            val wStepR = cos(angle)
            val wStepI = sin(angle)

            var i = 0
            while (i < n) {
                var wR = 1.0
                var wI = 0.0
                for (k in 0 until halfLen) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + halfLen] * wR - imag[i + k + halfLen] * wI
                    val vI = real[i + k + halfLen] * wI + imag[i + k + halfLen] * wR

                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + halfLen] = uR - vR
                    imag[i + k + halfLen] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }
}
