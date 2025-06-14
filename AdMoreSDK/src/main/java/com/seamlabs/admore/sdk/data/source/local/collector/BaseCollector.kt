package com.seamlabs.admore.sdk.data.source.local.collector

import android.content.Context

/**
 * Base class for data collectors.
 */
abstract class BaseCollector(protected val context: Context) {
    /**
     * Collects data.
     * @return Map of collected data
     */
    abstract suspend fun collect(): Map<String, Any>
}
