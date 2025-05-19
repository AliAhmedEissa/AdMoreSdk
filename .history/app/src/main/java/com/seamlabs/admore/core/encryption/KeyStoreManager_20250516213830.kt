package com.seamlabs.admore.core.encryption

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyStoreManager @Inject constructor(
    private val nativeKeyStore: NativeKeyStore
) {
    fun storeEncryptionKey(key: String): Boolean {
        return nativeKeyStore.storeKey(key)
    }

    fun getEncryptionKey(): String? {
        return if (nativeKeyStore.hasKey()) {
            nativeKeyStore.getKey()
        } else {
            null
        }
    }

    fun deleteKeys() {
        nativeKeyStore.deleteKey()
    }

    fun hasEncryptionKey(): Boolean {
        return nativeKeyStore.hasKey()
    }
} 