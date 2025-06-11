// File: com.seamlabs.admore/data/source/local/collector/BaseCollector.kt
package com.seamlabs.admore.data.source.local.collector

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Base class for all data collectors in the application.
 * This abstract class provides the fundamental structure for collecting
 * various types of device and system data.
 * 
 * Each collector that extends this class must implement:
 * 1. isPermissionGranted(): Check if required permissions are available
 * 2. collect(): Gather and return the specific data
 */
abstract class BaseCollector(
    @ApplicationContext protected val context: Context
) {
    /**
     * Checks if the collector has all required permissions to operate.
     * This method should be implemented by each collector to verify
     * its specific permission requirements.
     * @return true if all required permissions are granted
     */
    abstract fun isPermissionGranted(): Boolean

    /**
     * Main collection method that gathers the specific data.
     * This method should be implemented by each collector to gather
     * its specific type of data.
     * @return Map containing the collected data with appropriate keys
     */
    abstract suspend fun collect(): Map<String, Any>
}
