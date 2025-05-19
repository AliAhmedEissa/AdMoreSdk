package com.seamlabs.admore.core.encryption

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeKeyStore @Inject constructor() {
    
    init {
        System.loadLibrary("admore-keystore")
    }
    
    external fun storeKey(key: String): Boolean
    external fun getKey(): String
    external fun deleteKey(): Boolean
    external fun hasKey(): Boolean
} 