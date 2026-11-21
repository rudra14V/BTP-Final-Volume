package com.example.btp.ui.lock

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.btp.audio.AudioRecorder
import com.example.btp.data.LockManager
import com.example.btp.data.SecureStorage
import com.example.btp.data.SecuritySettings
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VoiceLockUiState(
    val packageName: String = "",
    val appName: String = "",
    val appIcon: Drawable? = null,
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val amplitude: Float = 0f,
    val isUnlocked: Boolean = false,
    val errorMessage: String? = null,
    val failedAttempts: Int = 0,
    val maxFailedAttempts: Int = SecuritySettings.DEFAULT_MAX_FAILED_ATTEMPTS,
    val cooldownRemainingSeconds: Int = 0,
    val similarityScore: Float? = null,
    val threshold: Float = SimilarityCalculator.DEFAULT_THRESHOLD
)

class VoiceLockViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder(application)
    private val ecapaTdnnModel = EcapaTdnnModel(application)
    private val embeddingExtractor = SpeakerEmbeddingExtractor(ecapaTdnnModel)
    private val vad = VoiceActivityDetector()
    private val similarityCalculator = SimilarityCalculator()

    private val secureStorage = SecureStorage(application, EncryptionManager())
    private val voiceRepository: VoiceRepository = VoiceRepositoryImpl(secureStorage, similarityCalculator)
    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(application)

    private val _uiState = MutableStateFlow(VoiceLockUiState())
    val uiState: StateFlow<VoiceLockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            audioRecorder.amplitudeFlow.collect { amp ->
                _uiState.update { it.copy(amplitude = amp) }
            }
        }
    }

    fun initForPackage(packageName: String) {
        if (packageName.isBlank()) return

        val packageManager = getApplication<Application>().packageManager
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }

        val appIcon = try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }

        val failedAttempts = LockManager.getFailedAttempts(packageName)
        val cooldownRemaining = LockManager.getRemainingCooldownSeconds(packageName)

        _uiState.update {
            it.copy(
                packageName = packageName,
                appName = appName,
                appIcon = appIcon,
                failedAttempts = failedAttempts,
                cooldownRemainingSeconds = cooldownRemaining
            )
        }
    }

    fun startVoiceVerification() {
        val targetPkg = _uiState.value.packageName
        if (targetPkg.isBlank() || _uiState.value.isRecording || _uiState.value.isProcessing) return

        // Check if locked out
        val cooldownRemaining = LockManager.getRemainingCooldownSeconds(targetPkg)
        if (cooldownRemaining > 0) {
            _uiState.update {
                it.copy(
                    cooldownRemainingSeconds = cooldownRemaining,
                    errorMessage = "Lockout active! Please wait $cooldownRemaining seconds."
                )
            }
            return
        }

        viewModelScope.launch {
            val settings = settingsRepository.getSettings().firstOrNull() ?: SecuritySettings()

            _uiState.update {
                it.copy(
                    isRecording = true,
                    errorMessage = null,
                    threshold = settings.similarityThreshold,
                    maxFailedAttempts = settings.maxFailedAttempts
                )
            }

            val audioData = audioRecorder.recordAudio(durationSeconds = 4)

            _uiState.update {
                it.copy(
                    isRecording = false,
                    isProcessing = true
                )
            }

            if (audioData.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Audio recording failed. Check microphone permission."
                    )
                }
                return@launch
            }

            val vadResult = vad.detectSpeech(audioData)
            if (!vadResult.hasSpeech) {
                val attempts = LockManager.registerFailedAttempt(
                    targetPkg,
                    settings.maxFailedAttempts,
                    settings.cooldownDurationSeconds
                )
                val remainingCooldown = LockManager.getRemainingCooldownSeconds(targetPkg)

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        failedAttempts = attempts,
                        cooldownRemainingSeconds = remainingCooldown,
                        errorMessage = "No clear voice detected. Attempt $attempts / ${settings.maxFailedAttempts}"
                    )
                }
                return@launch
            }

            val registeredProfile = voiceRepository.getVoiceProfile()
            if (registeredProfile.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "No enrolled voice profile found. Please register voice in BTP app first."
                    )
                }
                return@launch
            }

            val testEmbedding = embeddingExtractor.extractEmbedding(vadResult.voicedSamples)
            val verificationResult = similarityCalculator.verify(
                testEmbedding = testEmbedding,
                registeredEmbeddings = registeredProfile,
                threshold = settings.similarityThreshold
            )

            if (verificationResult.isMatch) {
                LockManager.grantAccess(targetPkg)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        isUnlocked = true,
                        similarityScore = verificationResult.maxSimilarity,
                        errorMessage = null
                    )
                }
            } else {
                val attempts = LockManager.registerFailedAttempt(
                    targetPkg,
                    settings.maxFailedAttempts,
                    settings.cooldownDurationSeconds
                )
                val remainingCooldown = LockManager.getRemainingCooldownSeconds(targetPkg)

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        failedAttempts = attempts,
                        cooldownRemainingSeconds = remainingCooldown,
                        similarityScore = verificationResult.maxSimilarity,
                        errorMessage = if (remainingCooldown > 0) {
                            "Max failed attempts exceeded! Locked out for $remainingCooldown seconds."
                        } else {
                            "Voice mismatch (score: %.2f, required: %.2f). Attempt $attempts / ${settings.maxFailedAttempts}"
                                .format(verificationResult.maxSimilarity, settings.similarityThreshold)
                        }
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ecapaTdnnModel.close()
    }
}
