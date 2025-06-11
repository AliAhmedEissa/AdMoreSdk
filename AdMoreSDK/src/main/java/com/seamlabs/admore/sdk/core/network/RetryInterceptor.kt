package com.seamlabs.admore.sdk.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/**
 * OkHttp interceptor for retrying failed requests.
 */
class RetryInterceptor @Inject constructor(
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null
        
        var retryCount = 0
        while (retryCount < 3) {
            try {
                response = chain.proceed(request)
                
                // Check if response is successful
                if (response.isSuccessful) {
                    return response
                } else {
                    // Close previous response
                    response.close()
                }
            } catch (e: IOException) {
                exception = e
            }
            
            // Increment retry count
            retryCount++
            
            // Wait before retrying
            if (retryCount < 3) {
                try {
                    Thread.sleep(1000L * retryCount)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("Retry interrupted", e)
                }
            }
        }
        
        // If we reach here, all retries failed
        if (response != null) {
            return response
        } else {
            throw exception ?: IOException("Unknown error")
        }
    }
}