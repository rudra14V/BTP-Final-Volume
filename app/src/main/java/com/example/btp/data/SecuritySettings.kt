package com.example.btp.data

/**
 * Data model holding security and voice lock settings.
 */
data class SecuritySettings(
    val similarityThreshold: Float = DEFAULT_SIMILARITY_THRESHOLD,
    val gracePeriodSeconds: Int = DEFAULT_GRACE_PERIOD_SECONDS,
    val maxFailedAttempts: Int = DEFAULT_MAX_FAILED_ATTEMPTS,
    val cooldownDurationSeconds: Int = DEFAULT_COOLDOWN_DURATION_SECONDS,
    val enrollmentSamplesCount: Int = DEFAULT_ENROLLMENT_SAMPLES_COUNT
) {
    companion object {
        const val DEFAULT_SIMILARITY_THRESHOLD = 0.83f
        const val MIN_SIMILARITY_THRESHOLD = 0.50f
        const val MAX_SIMILARITY_THRESHOLD = 0.95f

        const val DEFAULT_GRACE_PERIOD_SECONDS = 30 // Default 30s session window
        const val DEFAULT_MAX_FAILED_ATTEMPTS = 5
        const val DEFAULT_COOLDOWN_DURATION_SECONDS = 30

        const val DEFAULT_ENROLLMENT_SAMPLES_COUNT = 3
        val ENROLLMENT_SAMPLES_OPTIONS = listOf(2, 3, 5)

        val GRACE_PERIOD_OPTIONS = listOf(0, 15, 30, 60, 120)
        val MAX_FAILED_ATTEMPTS_OPTIONS = listOf(3, 5, 10)
        val COOLDOWN_OPTIONS = listOf(15, 30, 60, 120)

        fun sanitizeThreshold(value: Float): Float {
            return value.coerceIn(MIN_SIMILARITY_THRESHOLD, MAX_SIMILARITY_THRESHOLD)
        }
    }

    val sanitizedThreshold: Float
        get() = sanitizeThreshold(similarityThreshold)
}

enum class ThresholdValidationStatus(val label: String, val description: String) {
    UNSAFE_LOW("Low Security", "Threshold is low (0.50 - 0.64). May allow unauthorized voices."),
    MODERATE("Moderate", "Threshold is moderate (0.65 - 0.79). Standard protection level."),
    RECOMMENDED("Recommended", "Optimal balance of security and accuracy (0.80 - 0.88)."),
    STRICT("Strict Security", "High threshold (0.89 - 0.95). May require clear pronunciation in quiet environments.")
}

fun getThresholdStatus(threshold: Float): ThresholdValidationStatus {
    val sanitized = SecuritySettings.sanitizeThreshold(threshold)
    return when {
        sanitized < 0.65f -> ThresholdValidationStatus.UNSAFE_LOW
        sanitized < 0.80f -> ThresholdValidationStatus.MODERATE
        sanitized <= 0.88f -> ThresholdValidationStatus.RECOMMENDED
        else -> ThresholdValidationStatus.STRICT
    }
}
