package com.example.btp.ui.registration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.btp.audio.AudioRecorder
import com.example.btp.data.SecureStorage
import com.example.btp.data.SettingsRepository
import com.example.btp.data.SettingsRepositoryImpl
import com.example.btp.data.VoiceRepository
import com.example.btp.data.VoiceRepositoryImpl
import com.example.btp.ml.EcapaTdnnModel
import com.example.btp.ml.SimilarityCalculator
import com.example.btp.ml.SpeakerEmbeddingExtractor
import com.example.btp.ml.VoiceActivityDetector
import com.example.btp.security.EncryptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrationUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 3,
    val recordedEmbeddings: List<FloatArray> = emptyList(),
    val isRecording: Boolean = false,
    val amplitude: Float = 0f,
    val isProcessing: Boolean = false,
    val statusMessage: String = "Step 1 of 3: Tap record and speak passphrase 'My voice is my password'",
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isVoiceRegistered: Boolean = false
)

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder(application)
    private val ecapaTdnnModel = EcapaTdnnModel(application)
    private val embeddingExtractor = SpeakerEmbeddingExtractor(ecapaTdnnModel)
    private val vad = VoiceActivityDetector()
    private val similarityCalculator = SimilarityCalculator()

    private val secureStorage = SecureStorage(application, EncryptionManager())
    private val voiceRepository: VoiceRepository = VoiceRepositoryImpl(secureStorage, similarityCalculator)
    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(application)

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            audioRecorder.amplitudeFlow.collect { amp ->
                _uiState.update { it.copy(amplitude = amp) }
            }
        }
        viewModelScope.launch {
            voiceRepository.isVoiceRegistered().collect { isEnrolled ->
                _uiState.update { it.copy(isVoiceRegistered = isEnrolled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                val total = settings.enrollmentSamplesCount
                _uiState.update { state ->
                    val step = state.currentStep.coerceAtMost(total)
                    state.copy(
                        totalSteps = total,
                        statusMessage = if (state.recordedEmbeddings.isEmpty() && !state.isSuccess) {
                            "Step 1 of $total: Tap record and speak passphrase 'My voice is my password'"
                        } else {
                            state.statusMessage
                        }
                    )
                }
            }
        }
    }

    fun startRecordingStep() {
        if (_uiState.value.isRecording || _uiState.value.isProcessing) return
        val totalSteps = _uiState.value.totalSteps

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRecording = true,
                    errorMessage = null,
                    statusMessage = "Recording sample ${_uiState.value.currentStep} of $totalSteps... Speak now!"
                )
            }

            // Record 4 seconds of PCM audio
            val audioData = audioRecorder.recordAudio(durationSeconds = 4)

            _uiState.update {
                it.copy(
                    isRecording = false,
                    isProcessing = true,
                    statusMessage = "Processing voice sample..."
                )
            }

            if (audioData.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Audio recording failed or empty. Please check microphone permission."
                    )
                }
                return@launch
            }

            // Check voice activity
            val vadResult = vad.detectSpeech(audioData)
            if (!vadResult.hasSpeech) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "No clear voice detected. Please speak louder or re-try."
                    )
                }
                return@launch
            }

            // Extract L2-normalized embedding
            val embedding = embeddingExtractor.extractEmbedding(vadResult.voicedSamples)
            val updatedEmbeddings = _uiState.value.recordedEmbeddings + embedding
            val currentStep = _uiState.value.currentStep

            if (currentStep < totalSteps) {
                val nextStep = currentStep + 1
                _uiState.update {
                    it.copy(
                        currentStep = nextStep,
                        recordedEmbeddings = updatedEmbeddings,
                        isProcessing = false,
                        statusMessage = "Sample $currentStep captured! Step $nextStep of $totalSteps: Tap record and speak passphrase again."
                    )
                }
            } else {
                // Final sample complete - Validate consistency across all recordings
                val consistencyResult = similarityCalculator.checkEnrollmentConsistency(updatedEmbeddings)
                if (!consistencyResult.isConsistent) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            recordedEmbeddings = emptyList(),
                            currentStep = 1,
                            errorMessage = "Voice samples were inconsistent (min similarity: ${"%.2f".format(consistencyResult.minPairwiseSimilarity)}). Please re-enroll in a quiet environment."
                        )
                    }
                    return@launch
                }

                // Save enrolled profile securely
                val savedSuccess = voiceRepository.saveVoiceProfile(updatedEmbeddings)
                if (savedSuccess) {
                    _uiState.update {
                        it.copy(
                            currentStep = totalSteps,
                            recordedEmbeddings = updatedEmbeddings,
                            isProcessing = false,
                            isSuccess = true,
                            isVoiceRegistered = true,
                            statusMessage = "Voice profile enrolled with $totalSteps samples and encrypted successfully!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Failed to encrypt or store voice profile."
                        )
                    }
                }
            }
        }
    }

    fun resetEnrollment() {
        viewModelScope.launch {
            audioRecorder.stopRecording()
            _uiState.update {
                RegistrationUiState(
                    totalSteps = it.totalSteps,
                    isVoiceRegistered = it.isVoiceRegistered,
                    statusMessage = "Step 1 of ${it.totalSteps}: Tap record and speak passphrase 'My voice is my password'"
                )
            }
        }
    }

    fun clearVoiceProfile() {
        viewModelScope.launch {
            voiceRepository.clearVoiceProfile()
            resetEnrollment()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ecapaTdnnModel.close()
    }
}
