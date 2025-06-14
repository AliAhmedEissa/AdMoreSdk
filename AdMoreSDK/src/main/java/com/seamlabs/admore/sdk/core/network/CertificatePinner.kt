package com.seamlabs.admore.sdk.core.network

import com.seamlabs.admore.sdk.BuildConfig
import okhttp3.CertificatePinner


/**
 * Utility class for certificate pinning.
 */
class CertificatePinner {

    // Extract hostname from the full URL
    private val HOST = extractHostFromUrl(BuildConfig.host)
    private val PIN = BuildConfig.certificatePin

    /**
     * Gets the OkHttp CertificatePinner.
     * @return Configured CertificatePinner
     */
    fun getPinner(): CertificatePinner {
        return if (HOST.isNotEmpty() && PIN.isNotEmpty() && !isIpAddress(HOST)) {
            // Only pin certificates for domain names, not IP addresses
            CertificatePinner.Builder().add(HOST, PIN).build()
        } else {
            // Return empty certificate pinner for IP addresses or invalid hosts
            CertificatePinner.Builder().build()
        }
    }

    /**
     * Extract hostname from URL
     */
    private fun extractHostFromUrl(url: String): String {
        return try {
            val cleanUrl = url.trim()
            when {
                cleanUrl.startsWith("http://") -> cleanUrl.removePrefix("http://").removeSuffix("/")
                cleanUrl.startsWith("https://") -> cleanUrl.removePrefix("https://")
                    .removeSuffix("/")

                else -> cleanUrl.removeSuffix("/")
            }.split("/")[0] // Take only the host part, ignore path
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Check if the host is an IP address
     */
    private fun isIpAddress(host: String): Boolean {
        return host.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d+)?$"))
    }
}