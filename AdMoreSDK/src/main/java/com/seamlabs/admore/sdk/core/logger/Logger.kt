package com.seamlabs.admore.sdk.core.logger

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for logging.
 */
@Singleton
class Logger @Inject constructor() {
    private val TAG = "AdMoreSDK"

    /**
     * Logs a debug message.
     * @param message The message to log
     */
    fun debug(message: String) {
        Log.d(TAG, message)
    }

    /**
     * Logs an info message.
     * @param message The message to log
     */
    fun info(message: String) {
        Log.i(TAG, message)
    }

    /**
     * Logs a warning message.
     * @param message The message to log
     */
    fun warn(message: String) {
        Log.w(TAG, message)
    }

    /**
     * Logs an error message.
     * @param message The message to log
     * @param throwable The throwable to log
     */
    fun error(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}