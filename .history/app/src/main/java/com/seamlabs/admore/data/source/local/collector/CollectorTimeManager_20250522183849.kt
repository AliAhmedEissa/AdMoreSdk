package com.seamlabs.admore.data.source.local.collector

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages collection timing for different data collectors.
 * This class handles the timing and frequency of data collection
 * to prevent excessive collection and optimize resource usage.
 * 
 * Features:
 * 1. Tracks last collection time for each collector type
 * 2. Determines if collection should occur based on time intervals
 * 3. Updates collection timestamps
 */
@Singleton
class CollectorTimeManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("collector_timestamps", Context.MODE_PRIVATE)
    
    companion object {
        private const val WIFI_LAST_COLLECTION = "wifi_last_collection"
        private const val BLUETOOTH_LAST_COLLECTION = "bluetooth_last_collection"
        private const val ONE_DAY_MILLIS = 100 //24 * 60 * 60 * 1000L
    }

    private var lastWiCTime: Long = 0
    private var lastBhCTime: Long = 0
    private var lastLocationCTime: Long = 0
    private var lastContactCTime: Long = 0
    private var lastCalendarCTime: Long = 0
    private var lastSmsCTime: Long = 0
    private var lastNetworkInfoCTime: Long = 0
    private var lastDeviceInfoCTime: Long = 0
    private var lastAdvertisingIdCTime: Long = 0

    /**
     * Checks if WiFi data should be collected based on time interval.
     * @return true if enough time has passed since last collection
     */
    fun shouldCollectWiFi(): Boolean {
        return shouldCollect(WIFI_LAST_COLLECTION)
    }

    /**
     * Checks if Bluetooth data should be collected based on time interval.
     * @return true if enough time has passed since last collection
     */
    fun shouldCollectBh(): Boolean {
        return shouldCollect(BLUETOOTH_LAST_COLLECTION)
    }

    /**
     * Checks if location data should be collected based on time interval.
     * @return true if enough time has passed since last collection
     */
    fun shouldCollectLocation(): Boolean {
        return System.currentTimeMillis() - lastLocationCTime > 300000 // 5 minutes
    }

    /**
     * Checks if contact data should be collected based on time interval.
     * @return true if enough time has passed since last collection
     */
    fun shouldCollectContact(): Boolean {
        return System.currentTimeMillis() - lastContactCTime > 300000 // 5 minutes
    }

    /**
     * Checks if calendar data should be collected based on time interval.
     * @return true if enough time has passed since last collection
     */
    fun shouldCollectCalendar(): Boolean {
        return System.currentTimeMillis() - lastCalendarCTime > 300000 // 5 minutes
    }

    /**
     * Checks if SMS data should be collected based on time interval.
     * @return true if enough time has passed since last collection
     */
    fun shouldCollectSms(): Boolean {
        return System.currentTimeMillis() - lastSmsCTime > 300000 // 5 minutes
    }

    /**
     * Checks if network info should be collected based on time interval.
     * @return true if enough time has passed since last collection
     */
    fun shouldCollectNetworkInfo(): Boolean {
        return System.currentTimeMillis() - lastNetworkInfoCTime > 300000 // 5 minutes
    }

    /**
     * Checks if device info should be collected based on time interval.
     * @return true if enough time has passed since last collection
     */
    fun shouldCollectDeviceInfo(): Boolean {
        return System.currentTimeMillis() - lastDeviceInfoCTime > 300000 // 5 minutes
    }

    /**
     * Checks if advertising ID should be collected based on time interval.
     * @return true if enough time has passed since last collection
     */
    fun shouldCollectAdvertisingId(): Boolean {
        return System.currentTimeMillis() - lastAdvertisingIdCTime > 300000 // 5 minutes
    }

    /**
     * Updates the last WiFi collection timestamp.
     */
    fun updateWiCTime() {
        updateCollectionTime(WIFI_LAST_COLLECTION)
    }

    /**
     * Updates the last Bluetooth collection timestamp.
     */
    fun updateBhCTime() {
        updateCollectionTime(BLUETOOTH_LAST_COLLECTION)
    }

    /**
     * Updates the last location collection timestamp.
     */
    fun updateLocationCTime() {
        lastLocationCTime = System.currentTimeMillis()
    }

    /**
     * Updates the last contact collection timestamp.
     */
    fun updateContactCTime() {
        lastContactCTime = System.currentTimeMillis()
    }

    /**
     * Updates the last calendar collection timestamp.
     */
    fun updateCalendarCTime() {
        lastCalendarCTime = System.currentTimeMillis()
    }

    /**
     * Updates the last SMS collection timestamp.
     */
    fun updateSmsCTime() {
        lastSmsCTime = System.currentTimeMillis()
    }

    /**
     * Updates the last network info collection timestamp.
     */
    fun updateNetworkInfoCTime() {
        lastNetworkInfoCTime = System.currentTimeMillis()
    }

    /**
     * Updates the last device info collection timestamp.
     */
    fun updateDeviceInfoCTime() {
        lastDeviceInfoCTime = System.currentTimeMillis()
    }

    /**
     * Updates the last advertising ID collection timestamp.
     */
    fun updateAdvertisingIdCTime() {
        lastAdvertisingIdCTime = System.currentTimeMillis()
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