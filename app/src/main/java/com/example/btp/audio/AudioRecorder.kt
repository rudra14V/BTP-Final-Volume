package com.example.btp.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class AudioRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val _amplitudeFlow = MutableStateFlow(0f)
    val amplitudeFlow: StateFlow<Float> = _amplitudeFlow.asStateFlow()

    private val _isRecordingFlow = MutableStateFlow(false)
    val isRecordingFlow: StateFlow<Boolean> = _isRecordingFlow.asStateFlow()

    private var audioRecord: AudioRecord? = null

    /**
     * Records audio for the given duration in seconds at 16kHz mono.
     * Returns a FloatArray of audio samples scaled between -1.0 and 1.0.
     */
    @SuppressLint("MissingPermission")
    suspend fun recordAudio(durationSeconds: Int = 4): FloatArray = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        val bufferSize = maxOf(minBufferSize, SAMPLE_RATE * durationSeconds * 2)

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: SecurityException) {
            _isRecordingFlow.value = false
            return@withContext FloatArray(0)
        } catch (e: IllegalArgumentException) {
            _isRecordingFlow.value = false
            return@withContext FloatArray(0)
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            _isRecordingFlow.value = false
            return@withContext FloatArray(0)
        }

        val totalSamples = SAMPLE_RATE * durationSeconds
        val pcmBuffer = ShortArray(1024)
        val resultSamples = FloatArray(totalSamples)
        var samplesRead = 0

        _isRecordingFlow.value = true
        record.startRecording()

        try {
            while (samplesRead < totalSamples && _isRecordingFlow.value) {
                val read = record.read(pcmBuffer, 0, minOf(pcmBuffer.size, totalSamples - samplesRead))
                if (read > 0) {
                    var sumSquare = 0.0
                    for (i in 0 until read) {
                        val sampleShort = pcmBuffer[i]
                        val floatVal = sampleShort / 32768.0f
                        resultSamples[samplesRead + i] = floatVal
                        sumSquare += floatVal * floatVal
                    }
                    val rms = sqrt(sumSquare / read).toFloat()
                    // Scale amplitude for lively visualizer feedback
                    _amplitudeFlow.value = (rms * 6.0f).coerceIn(0f, 1f)
                    samplesRead += read
                } else if (read < 0) {
                    break
                }
            }
        } finally {
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            } catch (_: Exception) {}
            record.release()
            _isRecordingFlow.value = false
            _amplitudeFlow.value = 0f
        }

        if (samplesRead < totalSamples) {
            resultSamples.copyOf(samplesRead)
        } else {
            resultSamples
        }
    }

    fun stopRecording() {
        _isRecordingFlow.value = false
    }
}
