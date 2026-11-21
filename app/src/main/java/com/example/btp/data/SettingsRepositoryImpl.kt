package com.example.btp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    companion object {
        val KEY_SIMILARITY_THRESHOLD = floatPreferencesKey("similarity_threshold")
        val KEY_GRACE_PERIOD = intPreferencesKey("grace_period_seconds")
        val KEY_MAX_FAILED_ATTEMPTS = intPreferencesKey("max_failed_attempts")
        val KEY_COOLDOWN_DURATION = intPreferencesKey("cooldown_duration_seconds")
        val KEY_ENROLLMENT_SAMPLES_COUNT = intPreferencesKey("enrollment_samples_count")
    }

    override fun getSettings(): Flow<SecuritySettings> {
        return context.voiceDataStore.data.map { prefs ->
            val rawThreshold = prefs[KEY_SIMILARITY_THRESHOLD] ?: SecuritySettings.DEFAULT_SIMILARITY_THRESHOLD
            val threshold = SecuritySettings.sanitizeThreshold(rawThreshold)
            val gracePeriod = prefs[KEY_GRACE_PERIOD] ?: SecuritySettings.DEFAULT_GRACE_PERIOD_SECONDS
            val maxFailed = prefs[KEY_MAX_FAILED_ATTEMPTS] ?: SecuritySettings.DEFAULT_MAX_FAILED_ATTEMPTS
            val cooldown = prefs[KEY_COOLDOWN_DURATION] ?: SecuritySettings.DEFAULT_COOLDOWN_DURATION_SECONDS
            val enrollmentSamples = prefs[KEY_ENROLLMENT_SAMPLES_COUNT] ?: SecuritySettings.DEFAULT_ENROLLMENT_SAMPLES_COUNT

            SecuritySettings(
                similarityThreshold = threshold,
                gracePeriodSeconds = gracePeriod,
                maxFailedAttempts = maxFailed,
                cooldownDurationSeconds = cooldown,
                enrollmentSamplesCount = enrollmentSamples
            )
        }
    }

    override suspend fun updateSimilarityThreshold(threshold: Float) {
        val sanitized = SecuritySettings.sanitizeThreshold(threshold)
        context.voiceDataStore.edit { prefs ->
            prefs[KEY_SIMILARITY_THRESHOLD] = sanitized
        }
    }

    override suspend fun updateGracePeriod(seconds: Int) {
        context.voiceDataStore.edit { prefs ->
            prefs[KEY_GRACE_PERIOD] = seconds.coerceAtLeast(0)
        }
    }

    override suspend fun updateMaxFailedAttempts(attempts: Int) {
        context.voiceDataStore.edit { prefs ->
            prefs[KEY_MAX_FAILED_ATTEMPTS] = attempts.coerceAtLeast(1)
        }
    }

    override suspend fun updateCooldownDuration(seconds: Int) {
        context.voiceDataStore.edit { prefs ->
            prefs[KEY_COOLDOWN_DURATION] = seconds.coerceAtLeast(0)
        }
    }

    override suspend fun updateEnrollmentSamplesCount(count: Int) {
        context.voiceDataStore.edit { prefs ->
            prefs[KEY_ENROLLMENT_SAMPLES_COUNT] = if (count in listOf(2, 3, 5)) count else 3
        }
    }

    override suspend fun resetToDefaults() {
        context.voiceDataStore.edit { prefs ->
            prefs[KEY_SIMILARITY_THRESHOLD] = SecuritySettings.DEFAULT_SIMILARITY_THRESHOLD
            prefs[KEY_GRACE_PERIOD] = SecuritySettings.DEFAULT_GRACE_PERIOD_SECONDS
            prefs[KEY_MAX_FAILED_ATTEMPTS] = SecuritySettings.DEFAULT_MAX_FAILED_ATTEMPTS
            prefs[KEY_COOLDOWN_DURATION] = SecuritySettings.DEFAULT_COOLDOWN_DURATION_SECONDS
            prefs[KEY_ENROLLMENT_SAMPLES_COUNT] = SecuritySettings.DEFAULT_ENROLLMENT_SAMPLES_COUNT
        }
    }
}
