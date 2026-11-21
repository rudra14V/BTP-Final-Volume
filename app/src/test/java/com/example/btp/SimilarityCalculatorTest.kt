package com.example.btp

import com.example.btp.ml.SimilarityCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarityCalculatorTest {

    private val calculator = SimilarityCalculator()

    @Test
    fun testCosineSimilarityIdentical() {
        val vecA = floatArrayOf(1f, 2f, 3f, 4f)
        val vecB = floatArrayOf(1f, 2f, 3f, 4f)
        val sim = calculator.calculateCosineSimilarity(vecA, vecB)
        assertEquals(1.0f, sim, 1e-4f)
    }

    @Test
    fun testCosineSimilarityOrthogonal() {
        val vecA = floatArrayOf(1f, 0f, 0f)
        val vecB = floatArrayOf(0f, 1f, 0f)
        val sim = calculator.calculateCosineSimilarity(vecA, vecB)
        assertEquals(0.0f, sim, 1e-4f)
    }

    @Test
    fun testCentroidSimilarity() {
        val test = floatArrayOf(1f, 0f, 0f)
        val ref1 = floatArrayOf(1f, 0f, 0f)
        val ref2 = floatArrayOf(1f, 0f, 0f)

        val centroid = calculator.computeCentroid(listOf(ref1, ref2))
        val sim = calculator.calculateCosineSimilarity(test, centroid)
        assertEquals(1.0f, sim, 1e-4f)
    }

    @Test
    fun testDefaultThreshold() {
        assertEquals(0.83f, SimilarityCalculator.DEFAULT_THRESHOLD, 1e-4f)
    }

    @Test
    fun testVerification() {
        val test = floatArrayOf(1f, 1f, 0f)
        val ref = floatArrayOf(1f, 1f, 0f)

        val result = calculator.verify(test, listOf(ref), threshold = 0.83f)
        assertTrue(result.isMatch)
        assertEquals(1.0f, result.maxSimilarity, 1e-4f)
    }

    @Test
    fun testEnrollmentConsistency() {
        val e1 = floatArrayOf(1f, 0.1f, 0f)
        val e2 = floatArrayOf(1f, 0.12f, 0f)
        val e3 = floatArrayOf(1f, 0.08f, 0f)

        val consistency = calculator.checkEnrollmentConsistency(listOf(e1, e2, e3), consistencyThreshold = 0.60f)
        assertTrue(consistency.isConsistent)
    }
}
