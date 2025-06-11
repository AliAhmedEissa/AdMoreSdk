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
        private const val SMS_LAST_COLLECTION = "sms_last_collection"
        private const val CALENDAR_LAST_COLLECTION = "calendar_last_collection"
        private const val CONTACTS_LAST_COLLECTION = "contacts_last_collection"
        private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    }

    fun shouldCollectSms(): Boolean {
        return shouldCollect(SMS_LAST_COLLECTION)
    }

    fun shouldCollectCalendar(): Boolean {
        return shouldCollect(CALENDAR_LAST_COLLECTION)
    }

    fun shouldCollectContacts(): Boolean {
        return shouldCollect(CONTACTS_LAST_COLLECTION)
    }

    fun updateSmsCollectionTime() {
        updateCollectionTime(SMS_LAST_COLLECTION)
    }

    fun updateCalendarCollectionTime() {
        updateCollectionTime(CALENDAR_LAST_COLLECTION)
    }

    fun updateContactsCollectionTime() {
        updateCollectionTime(CONTACTS_LAST_COLLECTION)
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