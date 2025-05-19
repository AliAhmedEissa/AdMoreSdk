package com.seamlabs.admore.core.encryption

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeKeyStore @Inject constructor(
    private val context: Context
) {
    
    init {
        System.loadLibrary("admore-keystore")
        initializeWithSecureKey()
    }
    
    private fun initializeWithSecureKey() {
        val signingInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES
        )
        
        val signature = signingInfo.signatures[0]
        val md = MessageDigest.getInstance("SHA-256")
        val key = md.digest(signature.toByteArray())
        
        if (!initialize(String(key))) {
            throw IllegalStateException("Failed to initialize native key store")
        }
    }
    
    private external fun initialize(encryptionKey: String): Boolean
    external fun storeKey(key: String): Boolean
    external fun getKey(): String
    external fun deleteKey(): Boolean
    external fun hasKey(): Boolean
} 