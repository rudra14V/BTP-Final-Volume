package com.example.btp

import com.example.btp.data.SecuritySettings
import com.example.btp.data.ThresholdValidationStatus
import com.example.btp.data.getThresholdStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySettingsTest {

    @Test
    fun testDefaultSettingsValues() {
        val settings = SecuritySettings()
        assertEquals(0.83f, settings.similarityThreshold, 1e-4f)
        assertEquals(0, settings.gracePeriodSeconds)
        assertEquals(5, settings.maxFailedAttempts)
        assertEquals(30, settings.cooldownDurationSeconds)
        assertEquals(3, settings.enrollmentSamplesCount)
    }

    @Test
    fun testSanitizeThresholdClamping() {
        // Below minimum
        val lowClamped = SecuritySettings.sanitizeThreshold(0.20f)
        assertEquals(0.50f, lowClamped, 1e-4f)

        // Above maximum
        val highClamped = SecuritySettings.sanitizeThreshold(0.99f)
        assertEquals(0.95f, highClamped, 1e-4f)

        // Valid threshold within range
        val validThreshold = SecuritySettings.sanitizeThreshold(0.85f)
        assertEquals(0.85f, validThreshold, 1e-4f)
    }

    @Test
    fun testThresholdStatusClassification() {
        assertEquals(ThresholdValidationStatus.UNSAFE_LOW, getThresholdStatus(0.55f))
        assertEquals(ThresholdValidationStatus.MODERATE, getThresholdStatus(0.75f))
        assertEquals(ThresholdValidationStatus.RECOMMENDED, getThresholdStatus(0.83f))
        assertEquals(ThresholdValidationStatus.STRICT, getThresholdStatus(0.92f))
    }

    @Test
    fun testPresetOptionsLists() {
        assertTrue(SecuritySettings.GRACE_PERIOD_OPTIONS.contains(0))
        assertTrue(SecuritySettings.MAX_FAILED_ATTEMPTS_OPTIONS.contains(5))
        assertTrue(SecuritySettings.COOLDOWN_OPTIONS.contains(30))
        assertTrue(SecuritySettings.ENROLLMENT_SAMPLES_OPTIONS.contains(3))
    }
}
