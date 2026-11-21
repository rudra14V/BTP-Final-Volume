package com.example.btp.data

import com.example.btp.ml.SimilarityCalculator
import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface VoiceRepository {
    fun isVoiceRegistered(): Flow<Boolean>
    suspend fun saveVoiceProfile(embeddings: List<FloatArray>): Boolean
    suspend fun getVoiceProfile(): List<FloatArray>?
    suspend fun clearVoiceProfile()
}

class VoiceRepositoryImpl(
    private val secureStorage: SecureStorage,
    private val similarityCalculator: SimilarityCalculator = SimilarityCalculator()
) : VoiceRepository {

    override fun isVoiceRegistered(): Flow<Boolean> {
        return secureStorage.isVoiceRegisteredFlow
    }

    override suspend fun saveVoiceProfile(embeddings: List<FloatArray>): Boolean {
        if (embeddings.isEmpty()) return false

        // Check enrollment consistency across all pairs before persisting
        val consistencyResult = similarityCalculator.checkEnrollmentConsistency(embeddings)
        if (!consistencyResult.isConsistent) {
            return false
        }

        val serializedBytes = serializeEmbeddings(embeddings)
        secureStorage.saveEncryptedVoiceProfile(serializedBytes)
        return true
    }

    override suspend fun getVoiceProfile(): List<FloatArray>? {
        val decryptedBytes = secureStorage.getDecryptedVoiceProfile() ?: return null
        return deserializeEmbeddings(decryptedBytes)
    }

    override suspend fun clearVoiceProfile() {
        secureStorage.clearVoiceProfile()
    }

    private fun serializeEmbeddings(embeddings: List<FloatArray>): ByteArray {
        val numVectors = embeddings.size
        if (numVectors == 0) return ByteArray(0)
        val vectorDim = embeddings[0].size

        val totalFloatCount = numVectors * vectorDim
        val bufferSize = 8 + (totalFloatCount * 4) // 4 bytes for count, 4 bytes for dim, plus floats

        val buffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(numVectors)
        buffer.putInt(vectorDim)

        for (vector in embeddings) {
            for (floatVal in vector) {
                buffer.putFloat(floatVal)
            }
        }
        return buffer.array()
    }

    private fun deserializeEmbeddings(bytes: ByteArray): List<FloatArray>? {
        if (bytes.size < 8) return null

        try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val numVectors = buffer.int
            val vectorDim = buffer.int

            if (numVectors <= 0 || vectorDim <= 0) return null

            val expectedBytes = 8 + (numVectors * vectorDim * 4)
            if (bytes.size < expectedBytes) return null

            val resultList = ArrayList<FloatArray>(numVectors)
            for (v in 0 until numVectors) {
                val vector = FloatArray(vectorDim)
                for (d in 0 until vectorDim) {
                    vector[d] = buffer.float
                }
                resultList.add(vector)
            }
            return resultList
        } catch (_: Exception) {
            return null
        }
    }
}
