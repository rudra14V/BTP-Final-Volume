package com.example.btp.ui.testing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.btp.audio.AudioRecorder
import com.example.btp.security.EncryptionManager
import com.example.btp.data.SecureStorage
import com.example.btp.data.VoiceRepository
import com.example.btp.data.VoiceRepositoryImpl
import com.example.btp.ml.EcapaTdnnModel
import com.example.btp.ml.SimilarityCalculator
import com.example.btp.ml.SpeakerEmbeddingExtractor
import com.example.btp.ml.VoiceActivityDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class TestingUiState(
    val isRecording: Boolean = false,
    val amplitude: Float = 0f,
    val isProcessing: Boolean = false,
    val isRegisteredProfileAvailable: Boolean = false,
    val threshold: Float = SimilarityCalculator.DEFAULT_THRESHOLD,
    val extractedEmbedding: FloatArray? = null,
    val embeddingL2Norm: Float = 0f,
    val pairwiseSimilarities: List<Float> = emptyList(),
    val maxSimilarity: Float = 0f,
    val isPass: Boolean? = null,
    val statusMessage: String = "Tap record to capture test voice sample",
    val errorMessage: String? = null
)

class TestingViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder(application)
    private val ecapaTdnnModel = EcapaTdnnModel(application)
    private val embeddingExtractor = SpeakerEmbeddingExtractor(ecapaTdnnModel)
    private val vad = VoiceActivityDetector()
    private val similarityCalculator = SimilarityCalculator()

    private val secureStorage = SecureStorage(application, EncryptionManager())
    private val voiceRepository: VoiceRepository = VoiceRepositoryImpl(secureStorage, similarityCalculator)

    private val _uiState = MutableStateFlow(TestingUiState())
    val uiState: StateFlow<TestingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            audioRecorder.amplitudeFlow.collect { amp ->
                _uiState.update { it.copy(amplitude = amp) }
            }
        }
        viewModelScope.launch {
            voiceRepository.isVoiceRegistered().collect { isEnrolled ->
                _uiState.update { it.copy(isRegisteredProfileAvailable = isEnrolled) }
            }
        }
    }

    fun updateThreshold(newThreshold: Float) {
        _uiState.update { state ->
            val isPass = if (state.maxSimilarity > 0f) state.maxSimilarity >= newThreshold else null
            state.copy(
                threshold = newThreshold,
                isPass = isPass
            )
        }
    }

    fun startTestVerification() {
        if (_uiState.value.isRecording || _uiState.value.isProcessing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRecording = true,
                    errorMessage = null,
                    statusMessage = "Recording test audio... Speak passphrase"
                )
            }

            val audioData = audioRecorder.recordAudio(durationSeconds = 4)

            _uiState.update {
                it.copy(
                    isRecording = false,
                    isProcessing = true,
                    statusMessage = "Analyzing test sample..."
                )
            }

            if (audioData.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Failed to record audio. Please check permission."
                    )
                }
                return@launch
            }

            val vadResult = vad.detectSpeech(audioData)
            if (!vadResult.hasSpeech) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "No clear voice detected in test recording."
                    )
                }
                return@launch
            }

            // Extract test embedding
            val testEmbedding = embeddingExtractor.extractEmbedding(vadResult.voicedSamples)
            var l2NormSum = 0.0
            for (v in testEmbedding) l2NormSum += (v * v).toDouble()
            val l2Norm = sqrt(l2NormSum).toFloat()

            // Fetch stored registered embeddings
            val registeredProfile = voiceRepository.getVoiceProfile()

            if (registeredProfile.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        extractedEmbedding = testEmbedding,
                        embeddingL2Norm = l2Norm,
                        pairwiseSimilarities = emptyList(),
                        maxSimilarity = 0f,
                        isPass = false,
                        statusMessage = "Test sample processed. (Note: No enrolled voice profile found)"
                    )
                }
                return@launch
            }

            val verificationResult = similarityCalculator.verify(
                testEmbedding = testEmbedding,
                registeredEmbeddings = registeredProfile,
                threshold = _uiState.value.threshold
            )

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    extractedEmbedding = testEmbedding,
                    embeddingL2Norm = l2Norm,
                    pairwiseSimilarities = verificationResult.individualSimilarities,
                    maxSimilarity = verificationResult.maxSimilarity,
                    isPass = verificationResult.isMatch,
                    statusMessage = if (verificationResult.isMatch) "VERIFICATION PASSED!" else "VERIFICATION FAILED"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ecapaTdnnModel.close()
    }
}
