// File: com.seamlabs.admore/data/source/local/collector/BaseCollector.kt
package com.seamlabs.admore.sdk.data.source.local.collector

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Base class for data collectors.
 */
abstract class BaseCollector(@ApplicationContext protected val context: Context) {
    /**
     * Collects data.
     * @return Map of collected data
     */
    abstract suspend fun collect(): Map<String, Any>
}
