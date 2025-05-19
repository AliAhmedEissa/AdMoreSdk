package com.seamlabs.admore.core.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyStoreManager @Inject constructor() {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    companion object {
        private const val KEY_ALIAS = "admore_x25519_key"
        private const val ENCRYPTED_KEY_ALIAS = "admore_encrypted_key"
        private const val KEY_SIZE = 256
    }

    fun storeEncryptionKey(key: String) {
        // Generate a wrapping key
        val wrappingKey = getOrCreateWrappingKey()
        
        // Convert the input key to bytes
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        
        // Encrypt the key using the wrapping key
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        val encryptedKey = cipher.doFinal(keyBytes)
        
        // Store the encrypted key in SharedPreferences or other storage
        // For this example, we'll store it in the KeyStore
        val encryptedKeyEntry = KeyStore.SecretKeyEntry(SecretKeySpec(encryptedKey, "AES"))
        keyStore.setEntry(ENCRYPTED_KEY_ALIAS, encryptedKeyEntry, null)
    }

    fun getEncryptionKey(): String? {
        try {
            // Get the wrapping key
            val wrappingKey = getOrCreateWrappingKey()
            
            // Get the encrypted key from storage
            val encryptedKeyEntry = keyStore.getEntry(ENCRYPTED_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            val encryptedKey = encryptedKeyEntry?.secretKey?.encoded ?: return null
            
            // Decrypt the key using the wrapping key
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, wrappingKey)
            val decryptedKey = cipher.doFinal(encryptedKey)
            
            // Convert back to string
            return String(decryptedKey, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            createWrappingKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun createWrappingKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setKeySize(KEY_SIZE)
            .setRandomizedEncryptionRequired(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun deleteKeys() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
        if (keyStore.containsAlias(ENCRYPTED_KEY_ALIAS)) {
            keyStore.deleteEntry(ENCRYPTED_KEY_ALIAS)
        }
    }

    // Helper method to check if encryption key exists
    fun hasEncryptionKey(): Boolean {
        return try {
            keyStore.containsAlias(ENCRYPTED_KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }
} 