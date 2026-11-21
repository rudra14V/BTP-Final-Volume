package com.example.btp

import com.example.btp.ml.AudioPreprocessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

class AudioPreprocessorTest {

    private val preprocessor = AudioPreprocessor()

    @Test
    fun testNormalize() {
        val rawAudio = floatArrayOf(0.1f, -0.5f, 0.2f, 0.4f)
        val normalized = preprocessor.normalize(rawAudio, targetPeak = 1.0f)

        var maxPeak = 0f
        for (sample in normalized) {
            val mag = abs(sample)
            if (mag > maxPeak) maxPeak = mag
        }
        assertEquals(1.0f, maxPeak, 1e-4f)
    }

    @Test
    fun testTrimSilence() {
        val audioWithSilence = floatArrayOf(0f, 0.001f, 0.5f, 0.8f, 0.2f, 0.001f, 0f)
        val trimmed = preprocessor.trimSilence(audioWithSilence, thresholdRatio = 0.05f)

        assertTrue(trimmed.size < audioWithSilence.size)
        assertEquals(0.5f, trimmed[0], 1e-4f)
    }

    @Test
    fun testMelSpectrogramShape() {
        val sampleRate = 16000
        val durationSec = 1
        val audio = FloatArray(sampleRate * durationSec) { i ->
            (sin(2.0 * Math.PI * 440.0 * i / sampleRate)).toFloat()
        }

        val melSpec = preprocessor.extractMelSpectrogram(audio)
        assertTrue(melSpec.isNotEmpty())
        assertEquals(80, melSpec[0].size)
    }
}
