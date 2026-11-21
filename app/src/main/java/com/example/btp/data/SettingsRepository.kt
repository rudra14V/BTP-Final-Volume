package com.example.btp.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /**
     * Flow emitting the current security settings.
     */
    fun getSettings(): Flow<SecuritySettings>

    /**
     * Updates similarity threshold with range validation (0.50 to 0.95).
     */
    suspend fun updateSimilarityThreshold(threshold: Float)

    /**
     * Updates grace period in seconds.
     */
    suspend fun updateGracePeriod(seconds: Int)

    /**
     * Updates max failed attempts count.
     */
    suspend fun updateMaxFailedAttempts(attempts: Int)

    /**
     * Updates cooldown / lockout duration in seconds.
     */
    suspend fun updateCooldownDuration(seconds: Int)

    /**
     * Updates required enrollment samples count (2, 3, 5).
     */
    suspend fun updateEnrollmentSamplesCount(count: Int)

    /**
     * Resets all settings to default values.
     */
    suspend fun resetToDefaults()
}
