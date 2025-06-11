// File: com.seamlabs.admore/core/network/CertificatePinner.kt
package com.seamlabs.admore.sdk.core.network

import okhttp3.CertificatePinner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for certificate pinning.
 */
@Singleton
class CertificatePinner @Inject constructor() {
    
    private val HOST = "api.admore.seamlabs.com"
    private val PIN = "sha256/MCowBQYDK2VuAyEAq9m6BNi+QtbXyIm/SYmZmJqof1d6xdcv/+obsEHcqSI=" // This should be the actual hash in production
    
    /**
     * Gets the OkHttp CertificatePinner.
     * @return Configured CertificatePinner
     */
    fun getPinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add(HOST, PIN)
            .build()
    }
}