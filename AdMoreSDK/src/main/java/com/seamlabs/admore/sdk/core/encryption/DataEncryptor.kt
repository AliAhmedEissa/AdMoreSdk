package com.seamlabs.admore.sdk.core.encryption

/**
 * Interface for data encryption.
 */
interface DataEncryptor {
    /**
     * Encrypts data.
     * @param data The data to encrypt
     * @return Encrypted data as a string
     */
    fun encrypt(data: Map<String, Any>): String
    
    /**
     * Decrypts data.
     * @param encryptedData The encrypted data
     * @return Decrypted data
     */
    fun decrypt(encryptedData: String): Map<String, Any>
}