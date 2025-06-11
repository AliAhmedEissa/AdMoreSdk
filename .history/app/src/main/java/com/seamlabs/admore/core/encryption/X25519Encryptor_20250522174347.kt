package com.seamlabs.admore.core.encryption

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DataEncryptor using hybrid RSA/AES encryption.
 */
@Singleton
class X25519Encryptor @Inject constructor() : DataEncryptor {

    private val gson = Gson()
    private val publicKeyBase64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhylwhwTzPfMgHwNsnzbK/brtQ5sow8rSrYvCDdMUTcyz/6yEE/LTJUVM2BVRcoeg+YgZgW4ZkcpPLyccF4O9oieTcrJNLc/adArQr9fcUxpJ2pKCebpaRWOJRcxqXx4tNC3LcpgbmJE7Reu6Phc0WWDFDhXQKuQIvzdApQpU4norHBJaG4exi2BCnafqn8ncBrPX8IfgvdEThbtXl8brK9A/UAxlNcqB+ffBiApl9agjDkgOzaV+DCQJ0ZUIZ/HEpz4abZPX0wWOCFh4fCGy6DLcAxx0SwU5jCnRfKYGNog2VkcR/iXoJ2Ax5IfjX5OnTFkBSGoRLWXxxNJqpvw9CwIDAQAB"

    override fun encrypt(data: Map<String, Any>): String {
        try {
            // Convert data to JSON and wrap it in a JSON array
            val jsonObject = gson.toJsonTree(data)
            val jsonArray = JsonArray()
            jsonArray.add(jsonObject)
            val jsonData = gson.toJson(jsonArray)

            // Generate AES key
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            val aesKey = keyGen.generateKey()
            val aesKeyBytes = aesKey.encoded

            // Encrypt AES key with RSA
            val publicKeyBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(publicKeyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)

            val rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encryptedAesKey = rsaCipher.doFinal(aesKeyBytes)

            // Encrypt data with AES
            val random = SecureRandom()
            val iv = ByteArray(16)
            random.nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            val aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            aesCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKeyBytes, "AES"), ivSpec)
            val encryptedData = aesCipher.doFinal(jsonData.toByteArray(StandardCharsets.UTF_8))
            return Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(encryptedData, Base64.NO_WRAP)
        } catch (e: Exception) {
            return ""
        }
    }

    override fun decrypt(encryptedData: String): Map<String, Any> {
      return  emptyMap()
    }
}