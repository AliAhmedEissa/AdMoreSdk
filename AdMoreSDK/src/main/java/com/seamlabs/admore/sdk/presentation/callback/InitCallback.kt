package com.seamlabs.admore.sdk.presentation.callback

/**
 * Callback interface for SDK initialization.
 */
interface InitCallback {
    /**
     * Called when initialization is successful.
     */
    fun onSuccess()
    
    /**
     * Called when initialization fails.
     * @param error The error that occurred
     */
    fun onError(error: Throwable)
}
