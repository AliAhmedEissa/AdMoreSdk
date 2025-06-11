package com.seamlabs.admore.data.source.local.collector

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectorTimeManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("collector_timestamps", Context.MODE_PRIVATE)
    
    companion object {
        private const val WIFI_LAST_COLLECTION = "wifi_last_collection"
        private const val BLUETOOTH_LAST_COLLECTION = "bluetooth_last_collection"
        private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    }

    fun shouldCollectWifi(): Boolean {
        return shouldCollect(WIFI_LAST_COLLECTION)
    }

    fun shouldCollectBluetooth(): Boolean {
        return shouldCollect(BLUETOOTH_LAST_COLLECTION)
    }

    fun updateWifiCollectionTime() {
        updateCollectionTime(WIFI_LAST_COLLECTION)
    }

    fun updateBluetoothCollectionTime() {
        updateCollectionTime(BLUETOOTH_LAST_COLLECTION)
    }

    private fun shouldCollect(key: String): Boolean {
        val lastCollectionTime = prefs.getLong(key, 0L)
        val currentTime = System.currentTimeMillis()
        return currentTime - lastCollectionTime >= ONE_DAY_MILLIS
    }

    private fun updateCollectionTime(key: String) {
        prefs.edit().putLong(key, System.currentTimeMillis()).apply()
    }
} 