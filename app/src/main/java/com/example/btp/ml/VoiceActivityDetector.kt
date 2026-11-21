package com.example.btp.ml

import kotlin.math.log10
import kotlin.math.sqrt

class VoiceActivityDetector(
    private val frameSize: Int = 400, // 25ms at 16kHz
    private val frameStep: Int = 160, // 10ms at 16kHz
    private val energyThresholdDb: Float = -45.0f // Lenient energy threshold for mobile mics
) {

    data class VadResult(
        val hasSpeech: Boolean,
        val speechRatio: Float,
        val voicedSamples: FloatArray
    )

    /**
     * Evaluates whether audio contains spoken speech segments.
     */
    fun detectSpeech(audio: FloatArray): VadResult {
        if (audio.size < frameSize) {
            return VadResult(hasSpeech = false, speechRatio = 0f, voicedSamples = floatArrayOf())
        }

        val numFrames = (audio.size - frameSize) / frameStep + 1
        var voicedFrameCount = 0
        val isFrameVoiced = BooleanArray(numFrames)

        for (f in 0 until numFrames) {
            val offset = f * frameStep
            var sumSquare = 0.0
            for (i in 0 until frameSize) {
                val sample = audio[offset + i]
                sumSquare += (sample * sample).toDouble()
            }
            val rms = sqrt(sumSquare / frameSize)
            val db = if (rms > 1e-7) (20.0 * log10(rms)).toFloat() else -100.0f

            if (db >= energyThresholdDb) {
                isFrameVoiced[f] = true
                voicedFrameCount++
            }
        }

        val speechRatio = voicedFrameCount.toFloat() / numFrames.coerceAtLeast(1)
        val hasSpeech = speechRatio >= 0.08f && voicedFrameCount >= 3

        // Extract voiced audio samples
        val voicedList = ArrayList<Float>()
        for (f in 0 until numFrames) {
            if (isFrameVoiced[f]) {
                val offset = f * frameStep
                for (i in 0 until frameStep) {
                    if (offset + i < audio.size) {
                        voicedList.add(audio[offset + i])
                    }
                }
            }
        }

        val voicedSamples = if (voicedList.isNotEmpty()) voicedList.toFloatArray() else audio

        return VadResult(
            hasSpeech = hasSpeech,
            speechRatio = speechRatio,
            voicedSamples = voicedSamples
        )
    }
}
