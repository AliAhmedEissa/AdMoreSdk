package com.seamlabs.admore.core.encryption

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class X25519EncryptorTest {
    private val testDispatcher = TestCoroutineDispatcher()
    
    private lateinit var encryptor: X25519Encryptor
    
    @Before
    fun setup() {
        encryptor = X25519Encryptor()
    }
    
    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `encrypt should return non-empty string`() {
        // Arrange
        val data = mapOf("key" to "value", "num" to 123)
        
        // Act
        val encrypted = encryptor.encrypt(data)
        
        // Assert
        assert(encrypted.isNotEmpty())
    }
    
    @Test
    fun `decrypt should restore original data`() {
        // Arrange
        val originalData = mapOf(
            "string_key" to "string_value",
            "int_key" to 123,
            "bool_key" to true,
            "double_key" to 3.14
        )
        
        // Act
        val encrypted = encryptor.encrypt(originalData)
        val decrypted = encryptor.decrypt(encrypted)
        
        // Assert
        assert(decrypted["string_key"] == "string_value")
        
        // Note: When using Gson to serialize/deserialize, numbers might be
        // converted to different numeric types, so direct equality might not work.
        // Instead, we check for approximate equality.
        assert((decrypted["int_key"] as Number).toInt() == 123)
        assert(decrypted["bool_key"] == true)
        assert((decrypted["double_key"] as Number).toDouble() - 3.14 < 0.0001)
    }
    
    @Test
    fun `encrypt should handle nested maps`() {
        // Arrange
        val nestedData = mapOf(
            "outer_key" to "outer_value",
            "nested" to mapOf(
                "inner_key" to "inner_value",
                "inner_num" to 456
            )
        )
        
        // Act
        val encrypted = encryptor.encrypt(nestedData)
        val decrypted = encryptor.decrypt(encrypted)
        
        // Assert
        assert(decrypted["outer_key"] == "outer_value")
        assert(decrypted["nested"] is Map<*, *>)
        
        @Suppress("UNCHECKED_CAST")
        val nestedResult = decrypted["nested"] as Map<String, Any>
        assert(nestedResult["inner_key"] == "inner_value")
    }
    
    @Test
    fun `encrypt should handle different instances of encryptor`() {
        // Arrange
        val data = mapOf("key" to "value")
        val encryptor1 = X25519Encryptor()
        val encryptor2 = X25519Encryptor()
        
        // Act
        val encrypted1 = encryptor1.encrypt(data)
        val encrypted2 = encryptor2.encrypt(data)
        
        // Each encryptor should be able to decrypt its own encrypted data
        val decrypted1 = encryptor1.decrypt(encrypted1)
        val decrypted2 = encryptor2.decrypt(encrypted2)
        
        // Assert
        assert(decrypted1["key"] == "value")
        assert(decrypted2["key"] == "value")
        
        // But they might not be able to decrypt each other's data
        // This test could fail if they happen to generate the same key pair
        // So we don't test cross-decryption
    }
}