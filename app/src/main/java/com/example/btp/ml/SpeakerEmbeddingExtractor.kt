package com.example.btp.ml

import kotlin.math.sqrt

class SpeakerEmbeddingExtractor(
    private val ecapaTdnnModel: EcapaTdnnModel
) {

    /**
     * Extracts L2-normalized speaker embedding vector from processed audio signal.
     */
    fun extractEmbedding(audio: FloatArray): FloatArray {
        if (audio.isEmpty()) {
            return FloatArray(EcapaTdnnModel.EMBEDDING_DIM)
        }

        val rawEmbedding = ecapaTdnnModel.extractRawEmbedding(audio)
        return l2Normalize(rawEmbedding)
    }

    /**
     * Applies L2 normalization to a float array vector.
     */
    fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0.0
        for (v in vector) {
            sumSquares += (v * v).toDouble()
        }
        val norm = sqrt(sumSquares).toFloat()
        if (norm <= 1e-12f) {
            return vector.copyOf()
        }

        val normalized = FloatArray(vector.size)
        for (i in vector.indices) {
            normalized[i] = vector[i] / norm
        }
        return normalized
    }
}
