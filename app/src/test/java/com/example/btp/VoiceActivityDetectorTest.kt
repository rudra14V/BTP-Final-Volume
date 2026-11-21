package com.example.btp

import com.example.btp.ml.VoiceActivityDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class VoiceActivityDetectorTest {

    private val vad = VoiceActivityDetector()

    @Test
    fun testSilenceDetection() {
        val silenceAudio = FloatArray(16000) { 0.0001f }
        val result = vad.detectSpeech(silenceAudio)
        assertFalse(result.hasSpeech)
    }

    @Test
    fun testSpeechSignalDetection() {
        val sampleRate = 16000
        val speechAudio = FloatArray(sampleRate * 2) { i ->
            (0.5f * sin(2.0 * Math.PI * 300.0 * i / sampleRate)).toFloat()
        }

        val result = vad.detectSpeech(speechAudio)
        assertTrue(result.hasSpeech)
        assertTrue(result.speechRatio > 0.5f)
        assertTrue(result.voicedSamples.isNotEmpty())
    }
}
