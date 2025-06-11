package com.seamlabs.admore.sdk.core.network

import com.seamlabs.admore.sdk.BuildConfig
import okhttp3.CertificatePinner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for certificate pinning.
 */
@Singleton
class CertificatePinner @Inject constructor() {
    
    private val HOST = BuildConfig.host
    private val PIN =BuildConfig.certificatePin
    
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