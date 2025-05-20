package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.seamlabs.admore.core.storage.ContentResolverUtils
import com.seamlabs.admore.data.source.local.model.SmsKeys
import com.seamlabs.admore.domain.model.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Optimized collector for SMS data with improved efficiency and comprehensive data collection.
 * Designed to work with the existing PermissionRequiredCollector base class.
 */
class SmsCollector @Inject constructor(
    context: Context
) : PermissionRequiredCollector(
    context, setOf(Permission.SMS)
) {

    companion object {
        private const val TAG = "SmsCollector"

        // Pre-KitKat constants for compatibility
        private val SMS_URI = Telephony.Sms.CONTENT_URI

        private val CONVERSATIONS_URI = Telephony.Sms.Conversations.CONTENT_URI

        // Column mappings for compatibility
        private val COLUMN_MAPPINGS = mapOf(
            SmsKeys.THREAD_ID to Telephony.Sms.Conversations.THREAD_ID,
            SmsKeys.THREAD_CONTACT to Telephony.Sms.Conversations.ADDRESS,
            SmsKeys.THREAD_MESSAGE_COUNT to Telephony.Sms.Conversations.MESSAGE_COUNT,
            SmsKeys.THREAD_SNIPPET to Telephony.Sms.Conversations.SNIPPET,
            SmsKeys.THREAD_DATE to Telephony.Sms.Conversations.DATE,
            SmsKeys.MESSAGE_ID to Telephony.Sms._ID,
            SmsKeys.MESSAGE_ADDRESS to Telephony.Sms.ADDRESS,
            SmsKeys.MESSAGE_BODY to Telephony.Sms.BODY,
            SmsKeys.MESSAGE_DATE to Telephony.Sms.DATE,
            SmsKeys.MESSAGE_TYPE to Telephony.Sms.TYPE,
            SmsKeys.MESSAGE_READ to Telephony.Sms.READ,
            SmsKeys.MESSAGE_SEEN to Telephony.Sms.SEEN,
            SmsKeys.MESSAGE_SUBJECT to Telephony.Sms.SUBJECT,
            SmsKeys.MESSAGE_SERVICE_CENTER to Telephony.Sms.SERVICE_CENTER,
            SmsKeys.MESSAGE_STATUS to Telephony.Sms.STATUS,
            SmsKeys.MESSAGE_THREAD_ID to Telephony.Sms.THREAD_ID
        )

        // Increased message limit for more comprehensive data collection
        private const val MAX_MESSAGES = 50
    }

    private val contentResolverUtils = ContentResolverUtils(context)

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override suspend fun collect(): Map<String, Any> = withContext(Dispatchers.IO) {
        if (!isPermissionGranted()) {
            return@withContext emptyMap<String, Any>()
        }

        val data = mutableMapOf<String, Any>()

        try {
            // Get all SMS threads
            collectThreads(data)

            // Get SMS messages via direct query for more control and complete data
            collectMessages(data)

            // Get additional SMS metadata if available
            collectAdditionalData(data)

        } catch (e: Exception) {
            Log.e(TAG, "Error collecting SMS data: ${e.message}", e)
        }

        return@withContext data
    }

    /**
     * Collect all SMS conversation threads
     */
    private fun collectThreads(data: MutableMap<String, Any>) {
        try {
            val threads = queryContentResolver(
                CONVERSATIONS_URI,
                null,
                null,
                null,
                null,
                ::mapThreadData
            )

            data[SmsKeys.THREADS.toKey()] = threads
            Log.d(TAG, "Collected ${threads.size} SMS threads")
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting SMS threads: ${e.message}", e)
            data[SmsKeys.THREADS.toKey()] = emptyList<Map<String, Any>>()
        }
    }

    /**
     * Collect all SMS messages with improved efficiency
     */
    private suspend fun collectMessages(data: MutableMap<String, Any>) {
        try {
            // First try using ContentResolverUtils which might be more optimized for some devices
            val utilsMessages = try {
                contentResolverUtils.safeQuerySms(
                    sortOrder = "${getColumnName(SmsKeys.MESSAGE_DATE)} DESC"
                ).map { message ->
                    transformMessage(message)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error using ContentResolverUtils: ${e.message}")
                emptyList()
            }

            // If ContentResolverUtils failed or returned empty, use direct query as backup
            val messages = if (utilsMessages.isNotEmpty()) {
                utilsMessages
            } else {
                val sortOrder = "${getColumnName(SmsKeys.MESSAGE_DATE)} DESC LIMIT $MAX_MESSAGES"
                queryContentResolver(
                    SMS_URI,
                    null,
                    null,
                    null,
                    sortOrder,
                    ::mapMessageData
                )
            }

            data[SmsKeys.MESSAGES.toKey()] = messages
            Log.d(TAG, "Collected ${messages.size} SMS messages")
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting SMS messages: ${e.message}", e)
            data[SmsKeys.MESSAGES.toKey()] = emptyList<Map<String, Any>>()
        }
    }

    /**
     * Collect additional SMS metadata if available
     */
    private fun collectAdditionalData(data: MutableMap<String, Any>) {
        try {
            // Get counts of different message types
            val messagesList =
                data[SmsKeys.MESSAGES.toKey()] as? List<Map<String, Any>> ?: emptyList()

            // Calculate message type distribution
            val messageTypes = messagesList
                .groupBy { it[SmsKeys.MESSAGE_TYPE.toKey()] as? Int ?: 0 }
                .mapValues { it.value.size }

            data["message_type_counts"] = messageTypes

            // Add total message count stat
            data["total_message_count"] = messagesList.size

            // Get most active threads
            val threadsList =
                data[SmsKeys.THREADS.toKey()] as? List<Map<String, Any>> ?: emptyList()
            if (threadsList.isNotEmpty()) {
                val activeThreads = threadsList
                    .sortedByDescending { it[SmsKeys.THREAD_MESSAGE_COUNT.toKey()] as? Int ?: 0 }
                    .take(10)
                    .map {
                        mapOf(
                            "thread_id" to (it[SmsKeys.THREAD_ID.toKey()] ?: 0),
                            "contact" to (it[SmsKeys.THREAD_CONTACT.toKey()] ?: "Unknown"),
                            "message_count" to (it[SmsKeys.THREAD_MESSAGE_COUNT.toKey()] ?: 0)
                        )
                    }

                data["most_active_threads"] = activeThreads
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting additional SMS data: ${e.message}", e)
        }
    }

    /**
     * Transform a message from ContentResolverUtils format to our standard format
     */
    private fun transformMessage(message: Map<String, Any>): Map<String, Any> {
        return mapOf(
            SmsKeys.MESSAGE_ID.toKey() to (message["id"] as? Long ?: 0L),
            SmsKeys.MESSAGE_ADDRESS.toKey() to (message["address"] as? String ?: "Unknown"),
            SmsKeys.MESSAGE_BODY.toKey() to (message["body"] as? String ?: ""),
            SmsKeys.MESSAGE_DATE.toKey() to (message["date"] as? Long ?: 0L),
            SmsKeys.MESSAGE_TYPE.toKey() to (message["type"] as? Int ?: 0),
            SmsKeys.MESSAGE_READ.toKey() to ((message["read"] as? Int ?: 0) == 1),
            SmsKeys.MESSAGE_SEEN.toKey() to ((message["seen"] as? Int ?: 0) == 1),
            SmsKeys.MESSAGE_THREAD_ID.toKey() to (message["thread_id"] as? Long ?: 0L)
        )
    }

    /**
     * Generic function to query content resolver with error handling and mapping
     */
    private fun <T> queryContentResolver(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
        mapper: (Cursor) -> T
    ): List<T> {
        val results = mutableListOf<T>()

        try {
            context.contentResolver.query(
                uri, projection, selection, selectionArgs, sortOrder
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        results.add(mapper(cursor))
                    } catch (e: Exception) {
                        Log.w(TAG, "Error mapping cursor data: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying content resolver: ${e.message}", e)
        }

        return results
    }

    /**
     * Map SMS thread data from cursor to map
     */
    private fun mapThreadData(cursor: Cursor): Map<String, Any> {
        val threadMap = mutableMapOf<String, Any>()

        try {
            threadMap[SmsKeys.THREAD_ID.toKey()] = getColumnLongValue(
                cursor,
                getColumnName(SmsKeys.THREAD_ID),
                0L
            )
            threadMap[SmsKeys.THREAD_CONTACT.toKey()] = getColumnStringValue(
                cursor,
                getColumnName(SmsKeys.THREAD_CONTACT),
                "Unknown"
            )
            threadMap[SmsKeys.THREAD_MESSAGE_COUNT.toKey()] = getColumnIntValue(
                cursor,
                getColumnName(SmsKeys.THREAD_MESSAGE_COUNT),
                0
            )
            threadMap[SmsKeys.THREAD_SNIPPET.toKey()] = getColumnStringValue(
                cursor,
                getColumnName(SmsKeys.THREAD_SNIPPET),
                ""
            )
            threadMap[SmsKeys.THREAD_DATE.toKey()] = getColumnLongValue(
                cursor,
                getColumnName(SmsKeys.THREAD_DATE),
                0L
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error mapping thread data: ${e.message}")
        }

        return threadMap
    }

    /**
     * Map SMS message data from cursor to map
     */
    private fun mapMessageData(cursor: Cursor): Map<String, Any> {
        val messageMap = mutableMapOf<String, Any>()

        try {
            messageMap[SmsKeys.MESSAGE_ID.toKey()] = getColumnLongValue(
                cursor,
                getColumnName(SmsKeys.MESSAGE_ID),
                0L
            )
            messageMap[SmsKeys.MESSAGE_ADDRESS.toKey()] = getColumnStringValue(
                cursor,
                getColumnName(SmsKeys.MESSAGE_ADDRESS),
                "Unknown"
            )
            messageMap[SmsKeys.MESSAGE_BODY.toKey()] = getColumnStringValue(
                cursor,
                getColumnName(SmsKeys.MESSAGE_BODY),
                ""
            )
            messageMap[SmsKeys.MESSAGE_DATE.toKey()] = getColumnLongValue(
                cursor,
                getColumnName(SmsKeys.MESSAGE_DATE),
                0L
            )
            messageMap[SmsKeys.MESSAGE_TYPE.toKey()] = getColumnIntValue(
                cursor,
                getColumnName(SmsKeys.MESSAGE_TYPE),
                0
            )
            messageMap[SmsKeys.MESSAGE_READ.toKey()] = getColumnIntValue(
                cursor,
                getColumnName(SmsKeys.MESSAGE_READ),
                0
            ) == 1
            messageMap[SmsKeys.MESSAGE_SEEN.toKey()] = getColumnIntValue(
                cursor,
                getColumnName(SmsKeys.MESSAGE_SEEN),
                0
            ) == 1
            messageMap[SmsKeys.MESSAGE_SUBJECT.toKey()] = getColumnStringValue(
                cursor,
                getColumnName(SmsKeys.MESSAGE_SUBJECT),
                ""
            )
            messageMap[SmsKeys.MESSAGE_SERVICE_CENTER.toKey()] = getColumnStringValue(
                cursor,
                getColumnName(SmsKeys.MESSAGE_SERVICE_CENTER),
                ""
            )
            messageMap[SmsKeys.MESSAGE_STATUS.toKey()] = getColumnIntValue(
                cursor,
                getColumnName(SmsKeys.MESSAGE_STATUS),
                0
            )
            messageMap[SmsKeys.MESSAGE_THREAD_ID.toKey()] = getColumnLongValue(
                cursor,
                getColumnName(SmsKeys.MESSAGE_THREAD_ID),
                0L
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error mapping message data: ${e.message}")
        }

        return messageMap
    }

    /**
     * Get the actual column name for the given SmsKey based on device API level
     */
    private fun getColumnName(key: SmsKeys): String {
        return COLUMN_MAPPINGS[key] ?: key.name.lowercase()
    }

    // Utility methods for safely accessing cursor data

    /**
     * Safely get string value from cursor
     */
    private fun getColumnStringValue(
        cursor: Cursor,
        columnName: String,
        defaultValue: String
    ): String {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) cursor.getString(columnIndex) ?: defaultValue else defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Safely get int value from cursor
     */
    private fun getColumnIntValue(cursor: Cursor, columnName: String, defaultValue: Int): Int {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) cursor.getInt(columnIndex) else defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Safely get long value from cursor
     */
    private fun getColumnLongValue(cursor: Cursor, columnName: String, defaultValue: Long): Long {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) cursor.getLong(columnIndex) else defaultValue
        } catch (e: Exception) {
            defaultValue
        }

    }
}