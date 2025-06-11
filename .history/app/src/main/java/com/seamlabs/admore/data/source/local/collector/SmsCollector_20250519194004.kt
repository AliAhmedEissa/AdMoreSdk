package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.seamlabs.admore.core.storage.ContentResolverUtils
import com.seamlabs.admore.data.source.local.model.SmsKeys
import com.seamlabs.admore.domain.model.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Collector for SMS data with improved device compatibility and error handling.
 * Designed to work with the existing PermissionRequiredCollector base class.
 */
class SmsCollector @Inject constructor(
    context: Context
) : PermissionRequiredCollector(
    context, setOf(Permission.SMS)
) {

    companion object {
        // Pre-KitKat constants for compatibility
        private const val PRE_KITKAT_CONTENT_SMS_URI = "content://sms"
        private const val PRE_KITKAT_CONTENT_CONVERSATIONS_URI = "content://sms/conversations"

        // Column names for pre-KitKat devices
        private const val COLUMN_ID = "_id"
        private const val COLUMN_THREAD_ID = "thread_id"
        private const val COLUMN_ADDRESS = "address"
        private const val COLUMN_BODY = "body"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_READ = "read"
        private const val COLUMN_SEEN = "seen"
        private const val COLUMN_SUBJECT = "subject"
        private const val COLUMN_SERVICE_CENTER = "service_center"
        private const val COLUMN_STATUS = "status"
        private const val COLUMN_MSG_COUNT = "msg_count"
        private const val COLUMN_SNIPPET = "snippet"

        // Max messages to fetch to avoid performance issues
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
            // Get SMS threads
            val threads = getSmsThreads()
            data[SmsKeys.THREADS.toKey()] = threads

            // Get SMS messages using ContentResolverUtils
            val messages = contentResolverUtils.safeQuerySms(
                sortOrder = "${SmsKeys.MESSAGE_DATE.toKey()} DESC"
            )
            
            // Transform the data to match SmsKeys
            val transformedMessages = messages.map { message ->
                mapOf(
                    SmsKeys.MESSAGE_ID.toKey() to (message["id"] as Long),
                    SmsKeys.MESSAGE_ADDRESS.toKey() to (message["address"] as String),
                    SmsKeys.MESSAGE_BODY.toKey() to (message["body"] as String),
                    SmsKeys.MESSAGE_DATE.toKey() to (message["date"] as Long)
                )
            }
            
            data[SmsKeys.MESSAGES.toKey()] = transformedMessages
        } catch (e: Exception) {
            // Log error but don't crash
            android.util.Log.e("SmsCollector", "Error collecting SMS: ${e.message}")
        }

        return@withContext data
    }

    /**
     * Get all SMS conversation threads
     */
    private fun getSmsThreads(): List<Map<String, Any>> {
        val threads = mutableListOf<Map<String, Any>>()

        try {
            val uri =
                Telephony.Sms.Conversations.CONTENT_URI

            context.contentResolver.query(
                uri, null, null, null, null
            )?.use { cursor ->
                val threadCount = cursor.count

                val idColumn = getColumnIndex(
                    cursor,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) Telephony.Sms.Conversations.THREAD_ID
                    else COLUMN_THREAD_ID
                )

                if (idColumn == -1) {
                    return emptyList()
                }

                while (cursor.moveToNext()) {
                    try {
                        threads.add(getThreadInfo(cursor))
                    } catch (e: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
        }

        return threads
    }

    /**
     * Extract thread information from cursor
     */
    private fun getThreadInfo(cursor: Cursor): Map<String, Any> {
        val threadMap = mutableMapOf<String, Any>()

        try {
            // Get column names based on API level
            val threadIdColumn = Telephony.Sms.Conversations.THREAD_ID

            val addressColumn = Telephony.Sms.Conversations.ADDRESS

            val messageCountColumn = Telephony.Sms.Conversations.MESSAGE_COUNT

            val snippetColumn = Telephony.Sms.Conversations.SNIPPET

            val dateColumn = Telephony.Sms.Conversations.DATE

            // Extract values with defaults
            threadMap[SmsKeys.THREAD_ID.toKey()] = getColumnLongValue(cursor, threadIdColumn, 0L)
            threadMap[SmsKeys.THREAD_CONTACT.toKey()] =
                getColumnStringValue(cursor, addressColumn, "Unknown")
            threadMap[SmsKeys.THREAD_MESSAGE_COUNT.toKey()] =
                getColumnIntValue(cursor, messageCountColumn, 0)
            threadMap[SmsKeys.THREAD_SNIPPET.toKey()] =
                getColumnStringValue(cursor, snippetColumn, "")
            threadMap[SmsKeys.THREAD_DATE.toKey()] = getColumnLongValue(cursor, dateColumn, 0L)
        } catch (e: Exception) {
        }

        return threadMap
    }

    /**
     * Get SMS messages with limit to avoid performance issues
     */
    private fun getSmsMessages(): List<Map<String, Any>> {
        val messages = mutableListOf<Map<String, Any>>()

        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Telephony.Sms.CONTENT_URI
            } else {
                Uri.parse(PRE_KITKAT_CONTENT_SMS_URI)
            }

            val sortOrder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) "${Telephony.Sms.DATE} DESC LIMIT $MAX_MESSAGES"
                else "$COLUMN_DATE DESC LIMIT $MAX_MESSAGES"

            context.contentResolver.query(
                uri, null, null, null, sortOrder
            )?.use { cursor ->
                val messageCount = cursor.count

                while (cursor.moveToNext()) {
                    try {
                        messages.add(getMessageInfo(cursor))
                    } catch (e: Exception) {
                    }

                    // Safety check to avoid processing too many messages
                    if (messages.size >= MAX_MESSAGES) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
        }

        return messages
    }

    /**
     * Extract message information from cursor with compatibility for all API levels
     */
    private fun getMessageInfo(cursor: Cursor): Map<String, Any> {
        val messageMap = mutableMapOf<String, Any>()

        try {
            // Get column names based on API level
            val idColumn = Telephony.Sms._ID

            val addressColumn = Telephony.Sms.ADDRESS

            val bodyColumn = Telephony.Sms.BODY

            val dateColumn = Telephony.Sms.DATE

            val typeColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) Telephony.Sms.TYPE else COLUMN_TYPE

            val readColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) Telephony.Sms.READ else COLUMN_READ

            val seenColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) Telephony.Sms.SEEN else COLUMN_SEEN

            val subjectColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) Telephony.Sms.SUBJECT else COLUMN_SUBJECT

            val serviceCenterColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) Telephony.Sms.SERVICE_CENTER else COLUMN_SERVICE_CENTER

            val statusColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) Telephony.Sms.STATUS else COLUMN_STATUS

            val threadIdColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) Telephony.Sms.THREAD_ID else COLUMN_THREAD_ID

            // Extract values safely with defaults
            messageMap[SmsKeys.MESSAGE_ID.toKey()] = getColumnLongValue(cursor, idColumn, 0L)
            messageMap[SmsKeys.MESSAGE_ADDRESS.toKey()] =
                getColumnStringValue(cursor, addressColumn, "Unknown")
            messageMap[SmsKeys.MESSAGE_BODY.toKey()] = getColumnStringValue(cursor, bodyColumn, "")
            messageMap[SmsKeys.MESSAGE_DATE.toKey()] = getColumnLongValue(cursor, dateColumn, 0L)
            messageMap[SmsKeys.MESSAGE_TYPE.toKey()] = getColumnIntValue(cursor, typeColumn, 0)
            messageMap[SmsKeys.MESSAGE_READ.toKey()] = getColumnIntValue(cursor, readColumn, 0) == 1
            messageMap[SmsKeys.MESSAGE_SEEN.toKey()] = getColumnIntValue(cursor, seenColumn, 0) == 1
            messageMap[SmsKeys.MESSAGE_SUBJECT.toKey()] =
                getColumnStringValue(cursor, subjectColumn, "")
            messageMap[SmsKeys.MESSAGE_SERVICE_CENTER.toKey()] =
                getColumnStringValue(cursor, serviceCenterColumn, "")
            messageMap[SmsKeys.MESSAGE_STATUS.toKey()] = getColumnIntValue(cursor, statusColumn, 0)
            messageMap[SmsKeys.MESSAGE_THREAD_ID.toKey()] =
                getColumnLongValue(cursor, threadIdColumn, 0L)
        } catch (e: Exception) {
        }

        return messageMap
    }

    // Utility methods for safely accessing cursor data

    /**
     * Safely get column index, returns -1 if column doesn't exist
     */
    private fun getColumnIndex(cursor: Cursor, columnName: String): Int {
        return try {
            cursor.getColumnIndex(columnName)
        } catch (e: Exception) {
            -1
        }
    }

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