package com.example.btp.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.btp.security.EncryptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

val Context.voiceDataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_lock_preferences")

class SecureStorage(
    private val context: Context,
    private val encryptionManager: EncryptionManager
) {

    companion object {
        private val KEY_ENCRYPTED_DATA = stringPreferencesKey("encrypted_voice_embeddings_data")
        private val KEY_ENCRYPTED_IV = stringPreferencesKey("encrypted_voice_embeddings_iv")
        private val KEY_IS_ENROLLED = booleanPreferencesKey("is_voice_profile_enrolled")
    }

    val isVoiceRegisteredFlow: Flow<Boolean> = context.voiceDataStore.data.map { prefs ->
        prefs[KEY_IS_ENROLLED] ?: false
    }

    /**
     * Encrypts and saves raw bytes representing the voice profile embeddings.
     */
    suspend fun saveEncryptedVoiceProfile(plainDataBytes: ByteArray) {
        val encryptedPayload = encryptionManager.encrypt(plainDataBytes)
        val dataBase64 = Base64.encodeToString(encryptedPayload.cipherText, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(encryptedPayload.iv, Base64.NO_WRAP)

        context.voiceDataStore.edit { prefs ->
            prefs[KEY_ENCRYPTED_DATA] = dataBase64
            prefs[KEY_ENCRYPTED_IV] = ivBase64
            prefs[KEY_IS_ENROLLED] = true
        }
    }

    /**
     * Retrieves and decrypts the raw bytes representing the voice profile embeddings.
     */
    suspend fun getDecryptedVoiceProfile(): ByteArray? {
        val prefs = context.voiceDataStore.data.firstOrNull() ?: return null
        val dataBase64 = prefs[KEY_ENCRYPTED_DATA] ?: return null
        val ivBase64 = prefs[KEY_ENCRYPTED_IV] ?: return null

        return try {
            val cipherText = Base64.decode(dataBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val payload = EncryptionManager.EncryptedPayload(cipherText = cipherText, iv = iv)
            encryptionManager.decrypt(payload)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Clears all saved voice profile data.
     */
    suspend fun clearVoiceProfile() {
        context.voiceDataStore.edit { prefs ->
            prefs.remove(KEY_ENCRYPTED_DATA)
            prefs.remove(KEY_ENCRYPTED_IV)
            prefs[KEY_IS_ENROLLED] = false
        }
    }
}
