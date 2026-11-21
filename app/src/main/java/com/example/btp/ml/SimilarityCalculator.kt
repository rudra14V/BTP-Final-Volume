package com.example.btp.ml

import kotlin.math.sqrt

class SimilarityCalculator {

    companion object {
        const val DEFAULT_THRESHOLD = 0.83f
        const val ENROLLMENT_CONSISTENCY_THRESHOLD = 0.60f
    }

    /**
     * Calculates cosine similarity between two float embedding vectors.
     * Returns a float value typically between -1.0 and 1.0 (or 0.0 to 1.0 for normalized embeddings).
     */
    fun calculateCosineSimilarity(vecA: FloatArray, vecB: FloatArray): Float {
        if (vecA.isEmpty() || vecB.isEmpty() || vecA.size != vecB.size) {
            return 0.0f
        }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in vecA.indices) {
            val a = vecA[i].toDouble()
            val b = vecB[i].toDouble()
            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        val denominator = sqrt(normA) * sqrt(normB)
        if (denominator <= 1e-12) {
            return 0.0f
        }

        val similarity = (dotProduct / denominator).toFloat()
        return similarity.coerceIn(-1.0f, 1.0f)
    }

    /**
     * Computes the L2-normalized average centroid embedding from a list of registered embeddings.
     */
    fun computeCentroid(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return FloatArray(0)
        val dim = embeddings[0].size
        val centroid = FloatArray(dim)
        for (emb in embeddings) {
            for (i in 0 until dim) {
                centroid[i] += emb[i]
            }
        }
        val n = embeddings.size.toFloat()
        for (i in 0 until dim) {
            centroid[i] /= n
        }

        var sumSq = 0.0
        for (v in centroid) {
            sumSq += (v * v).toDouble()
        }
        val norm = sqrt(sumSq).toFloat()
        if (norm > 1e-12f) {
            for (i in centroid.indices) {
                centroid[i] /= norm
            }
        }
        return centroid
    }

    /**
     * Computes similarity between a test embedding and the list of registered embeddings
     * using the averaged centroid embedding.
     */
    fun calculateMaxSimilarity(
        testEmbedding: FloatArray,
        registeredEmbeddings: List<FloatArray>
    ): Float {
        if (registeredEmbeddings.isEmpty()) return 0.0f
        val centroid = computeCentroid(registeredEmbeddings)
        return calculateCosineSimilarity(testEmbedding, centroid)
    }

    /**
     * Verifies if a test embedding matches the registered voice profile centroid.
     * Unlocks ONLY if similarity with the average centroid is >= threshold (default 0.83).
     */
    fun verify(
        testEmbedding: FloatArray,
        registeredEmbeddings: List<FloatArray>,
        threshold: Float = DEFAULT_THRESHOLD
    ): VerificationResult {
        if (registeredEmbeddings.isEmpty()) {
            return VerificationResult(
                isMatch = false,
                maxSimilarity = 0.0f,
                threshold = threshold,
                individualSimilarities = emptyList()
            )
        }

        val centroid = computeCentroid(registeredEmbeddings)
        val similarity = calculateCosineSimilarity(testEmbedding, centroid)
        // STRICT THRESHOLD ENFORCEMENT: Only unlock if similarity with centroid >= threshold (0.83)
        val isMatch = similarity >= threshold

        return VerificationResult(
            isMatch = isMatch,
            maxSimilarity = similarity,
            threshold = threshold,
            individualSimilarities = listOf(similarity)
        )
    }

    /**
     * Checks if enrollment embeddings are mutually consistent (pairwise cosine similarity >= consistency threshold).
     */
    fun checkEnrollmentConsistency(
        embeddings: List<FloatArray>,
        consistencyThreshold: Float = ENROLLMENT_CONSISTENCY_THRESHOLD
    ): EnrollmentConsistencyResult {
        if (embeddings.size < 2) {
            return EnrollmentConsistencyResult(isConsistent = true, minPairwiseSimilarity = 1.0f)
        }

        val pairwise = mutableListOf<Float>()
        for (i in 0 until embeddings.size) {
            for (j in i + 1 until embeddings.size) {
                val sim = calculateCosineSimilarity(embeddings[i], embeddings[j])
                pairwise.add(sim)
            }
        }

        val minSim = pairwise.minOrNull() ?: 0.0f
        val isConsistent = pairwise.all { it >= consistencyThreshold }

        return EnrollmentConsistencyResult(
            isConsistent = isConsistent,
            minPairwiseSimilarity = minSim,
            pairwiseSimilarities = pairwise
        )
    }

    data class VerificationResult(
        val isMatch: Boolean,
        val maxSimilarity: Float,
        val threshold: Float,
        val individualSimilarities: List<Float>
    )

    data class EnrollmentConsistencyResult(
        val isConsistent: Boolean,
        val minPairwiseSimilarity: Float,
        val pairwiseSimilarities: List<Float> = emptyList()
    )
}
