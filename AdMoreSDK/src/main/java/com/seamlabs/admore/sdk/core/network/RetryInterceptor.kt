package com.seamlabs.admore.sdk.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * OkHttp interceptor for retrying failed requests.
 * Fixed version that properly handles response streams and doesn't interfere with logging interceptor.
 */
class RetryInterceptor : Interceptor {

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1000L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var lastException: IOException? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = chain.proceed(originalRequest)

                // If the response is successful, return it immediately
                if (response.isSuccessful) {
                    return response
                }

                // For non-successful responses, close the current response and retry if it's a server error
                if (response.code in 500..599 && attempt < MAX_RETRIES - 1) {
                    response.close()

                    // Wait before retrying with exponential backoff
                    val delayMs = INITIAL_DELAY_MS * (attempt + 1)
                    try {
                        Thread.sleep(delayMs)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Retry interrupted", e)
                    }
                } else {
                    // Return the response for client errors or final attempt
                    return response
                }

            } catch (e: IOException) {
                lastException = e

                // Only retry for specific network-related exceptions
                val shouldRetry = when (e) {
                    is SocketTimeoutException,
                    is UnknownHostException -> true
                    else -> e.message?.contains("failed to connect", ignoreCase = true) == true
                }

                if (!shouldRetry || attempt == MAX_RETRIES - 1) {
                }

                // Wait before retrying
                val delayMs = INITIAL_DELAY_MS * (attempt + 1)
                try {
                    Thread.sleep(delayMs)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }

        // This should never be reached, but just in case
        throw lastException ?: IOException("All retries failed")
    }
}