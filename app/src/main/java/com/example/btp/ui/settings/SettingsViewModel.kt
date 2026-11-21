package com.example.btp.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.btp.data.SecureStorage
import com.example.btp.data.SecuritySettings
import com.example.btp.data.SettingsRepository
import com.example.btp.data.SettingsRepositoryImpl
import com.example.btp.data.ThresholdValidationStatus
import com.example.btp.data.VoiceRepository
import com.example.btp.data.VoiceRepositoryImpl
import com.example.btp.data.getThresholdStatus
import com.example.btp.security.EncryptionManager
import com.example.btp.utils.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: SecuritySettings = SecuritySettings(),
    val isVoiceRegistered: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayEnabled: Boolean = false,
    val thresholdValidationStatus: ThresholdValidationStatus = ThresholdValidationStatus.RECOMMENDED
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(application)
    private val voiceRepository: VoiceRepository = VoiceRepositoryImpl(
        SecureStorage(application, EncryptionManager())
    )

    private val isAccessibilityEnabledFlow = MutableStateFlow(false)
    private val isOverlayEnabledFlow = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getSettings(),
        voiceRepository.isVoiceRegistered(),
        isAccessibilityEnabledFlow,
        isOverlayEnabledFlow
    ) { settings, isEnrolled, isAccessibilityEnabled, isOverlayEnabled ->
        SettingsUiState(
            settings = settings,
            isVoiceRegistered = isEnrolled,
            isAccessibilityEnabled = isAccessibilityEnabled,
            isOverlayEnabled = isOverlayEnabled,
            thresholdValidationStatus = getThresholdStatus(settings.similarityThreshold)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        checkAccessibilityStatus()
    }

    fun checkAccessibilityStatus() {
        val context = getApplication<Application>()
        isAccessibilityEnabledFlow.value = PermissionUtils.isAccessibilityServiceEnabled(context)
        isOverlayEnabledFlow.value = PermissionUtils.canDrawOverlays(context)
    }

    fun openAccessibilitySettings() {
        val context = getApplication<Application>()
        PermissionUtils.openAccessibilitySettings(context)
    }

    fun openOverlaySettings() {
        val context = getApplication<Application>()
        PermissionUtils.openOverlaySettings(context)
    }

    fun updateSimilarityThreshold(threshold: Float) {
        viewModelScope.launch {
            settingsRepository.updateSimilarityThreshold(threshold)
        }
    }

    fun updateGracePeriod(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.updateGracePeriod(seconds)
        }
    }

    fun updateMaxFailedAttempts(attempts: Int) {
        viewModelScope.launch {
            settingsRepository.updateMaxFailedAttempts(attempts)
        }
    }

    fun updateCooldownDuration(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.updateCooldownDuration(seconds)
        }
    }

    fun updateEnrollmentSamplesCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.updateEnrollmentSamplesCount(count)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
        }
    }

    fun clearVoiceProfile() {
        viewModelScope.launch {
            voiceRepository.clearVoiceProfile()
        }
    }
}
