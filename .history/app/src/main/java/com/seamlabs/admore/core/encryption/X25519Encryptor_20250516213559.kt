// File: com.seamlabs.admore/core/encryption/X25519Encryptor.kt
package com.seamlabs.admore.core.encryption

import android.util.Base64
import com.google.gson.Gson
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DataEncryptor using X25519 encryption with Android KeyStore integration.
 */
@Singleton
class X25519Encryptor @Inject constructor(
    private val keyStoreManager: KeyStoreManager
) : DataEncryptor {
    
    private val gson = Gson()
    private val keyPair: KeyPair
    private val secretKey: SecretKey
    
    init {
        // Generate X25519 key pair
        val keyPairGenerator = KeyPairGenerator.getInstance("XDH")
        keyPairGenerator.initialize(255, SecureRandom())
        keyPair = keyPairGenerator.generateKeyPair()
        
        // Get or create AES key from KeyStore
        secretKey = keyStoreManager.getOrCreateKey()
    }
    
    override fun encrypt(data: Map<String, Any>): String {
        // Convert data to JSON
        val jsonData = gson.toJson(data)
        
        // Generate shared secret
        val keyAgreement = KeyAgreement.getInstance("XDH")
        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(keyPair.public, true)
        val sharedSecret = keyAgreement.generateSecret()
        
        // Create AES key from shared secret
        val derivedKey = SecretKeySpec(sharedSecret, 0, 16, "AES")
        
        // Encrypt using AES
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(jsonData.toByteArray(Charsets.UTF_8))
        
        // Convert to Base64
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }
    
    override fun decrypt(encryptedData: String): Map<String, Any> {
        // Decode Base64
        val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
        
        // Generate shared secret
        val keyAgreement = KeyAgreement.getInstance("XDH")
        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(keyPair.public, true)
        val sharedSecret = keyAgreement.generateSecret()
        
        // Create AES key from shared secret
        val derivedKey = SecretKeySpec(sharedSecret, 0, 16, "AES")
        
        // Decrypt using AES
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        
        // Convert to Map
        val jsonData = String(decryptedBytes, Charsets.UTF_8)
        
        @Suppress("UNCHECKED_CAST")
        return gson.fromJson(jsonData, Map::class.java) as Map<String, Any>
    }
}