/**
 * Helper class to safely access content providers with cursor callbacks
 */
package com.seamlabs.admore.core.storage

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.provider.CalendarContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class for safely querying content providers
 */
object ContentResolverUtils {

    /**
     * Safely query content resolver and process results with a cursor callback
     */
    inline fun <T> querySafely(
        contentResolver: ContentResolver,
        uri: Uri,
        projection: Array<String>? = null,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null,
        defaultValue: T,
        crossinline cursorProcessor: (Cursor) -> T
    ): T {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(
                uri, projection, selection, selectionArgs, sortOrder
            )

            if (cursor != null && cursor.moveToFirst()) {
                cursorProcessor(cursor)
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            defaultValue
        } finally {
            cursor?.close()
        }
    }

    /**
     * Safely get a column value from cursor
     */
    inline fun <T> getColumnValue(
        cursor: Cursor, columnName: String, defaultValue: T, valueExtractor: (Cursor, Int) -> T
    ): T {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) {
                valueExtractor(cursor, columnIndex)
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            defaultValue
        }
    }

    // Specialized extension functions
    fun Cursor.getStringOrDefault(columnName: String, defaultValue: String = ""): String {
        return getColumnValue(this, columnName, defaultValue) { cursor, index ->
            cursor.getString(index) ?: defaultValue
        }
    }

    fun Cursor.getIntOrDefault(columnName: String, defaultValue: Int = 0): Int {
        return getColumnValue(
            this,
            columnName,
            defaultValue
        ) { cursor, index -> cursor.getInt(index) }
    }

    fun Cursor.getLongOrDefault(columnName: String, defaultValue: Long = 0L): Long {
        return getColumnValue(this, columnName, defaultValue) { cursor, index ->
            cursor.getLong(
                index
            )
        }
    }

    fun Cursor.getBooleanOrDefault(columnName: String, defaultValue: Boolean = false): Boolean {
        return getColumnValue(this, columnName, defaultValue) { cursor, index ->
            cursor.getInt(index) == 1
        }
    }
}

/**
 * Utility class for safely handling ContentResolver operations
 */
class ContentResolverUtils(private val context: Context) {

    companion object {
        private const val TAG = "ContentResolverUtils"
        private const val BATCH_SIZE = 100 // Process data in batches to avoid memory issues
    }

    /**
     * Safely query content provider and process cursor data
     */
    suspend fun <T> safeQuery(
        uri: Uri,
        projection: Array<String>? = null,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null,
        processor: suspend (Cursor) -> T
    ): List<T> = withContext(Dispatchers.IO) {
        val results = mutableListOf<T>()
        var cursor: Cursor? = null
        
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.let {
                while (it.moveToNext()) {
                    try {
                        results.add(processor(it))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing cursor row: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying content provider: ${e.message}")
        } finally {
            cursor?.close()
        }
        
        results
    }

    /**
     * Safely query contacts with batch processing
     */
    suspend fun safeQueryContacts(
        projection: Array<String>? = null,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
    ): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Map<String, Any>>()
        var cursor: Cursor? = null
        
        try {
            cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.let {
                while (it.moveToNext()) {
                    try {
                        val contactId = it.getLong(it.getColumnIndex(ContactsContract.Contacts._ID))
                        val contactData = mutableMapOf<String, Any>()
                        
                        // Basic contact info
                        contactData["id"] = contactId
                        contactData["name"] = it.getString(
                            it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        ) ?: "Unknown"
                        
                        // Get detailed info in batches
                        contactData.putAll(getContactDetails(contactId))
                        results.add(contactData)
                        
                        // Process in batches to avoid memory issues
                        if (results.size >= BATCH_SIZE) {
                            // Process batch here if needed
                            results.clear()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing contact: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts: ${e.message}")
        } finally {
            cursor?.close()
        }
        
        results
    }

    /**
     * Safely query SMS messages with batch processing
     */
    suspend fun safeQuerySms(
        projection: Array<String>? = null,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
    ): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Map<String, Any>>()
        var cursor: Cursor? = null
        
        try {
            cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.let {
                while (it.moveToNext()) {
                    try {
                        val messageData = mutableMapOf<String, Any>()
                        
                        // Basic message info
                        messageData["id"] = it.getLong(it.getColumnIndex(Telephony.Sms._ID))
                        messageData["address"] = it.getString(
                            it.getColumnIndex(Telephony.Sms.ADDRESS)
                        ) ?: "Unknown"
                        messageData["body"] = it.getString(
                            it.getColumnIndex(Telephony.Sms.BODY)
                        ) ?: ""
                        messageData["date"] = it.getLong(it.getColumnIndex(Telephony.Sms.DATE))
                        
                        results.add(messageData)
                        
                        // Process in batches to avoid memory issues
                        if (results.size >= BATCH_SIZE) {
                            // Process batch here if needed
                            results.clear()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing SMS: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SMS: ${e.message}")
        } finally {
            cursor?.close()
        }
        
        results
    }

    /**
     * Safely query calendar events with batch processing
     */
    suspend fun safeQueryCalendarEvents(
        projection: Array<String>? = null,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
    ): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Map<String, Any>>()
        var cursor: Cursor? = null
        
        try {
            cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.let {
                while (it.moveToNext()) {
                    try {
                        val eventData = mutableMapOf<String, Any>()
                        
                        // Basic event info
                        eventData["id"] = it.getLong(it.getColumnIndex(CalendarContract.Events._ID))
                        eventData["title"] = it.getString(
                            it.getColumnIndex(CalendarContract.Events.TITLE)
                        ) ?: "Untitled"
                        eventData["description"] = it.getString(
                            it.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                        ) ?: ""
                        eventData["start_time"] = it.getLong(
                            it.getColumnIndex(CalendarContract.Events.DTSTART)
                        )
                        eventData["end_time"] = it.getLong(
                            it.getColumnIndex(CalendarContract.Events.DTEND)
                        )
                        
                        results.add(eventData)
                        
                        // Process in batches to avoid memory issues
                        if (results.size >= BATCH_SIZE) {
                            // Process batch here if needed
                            results.clear()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing calendar event: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying calendar events: ${e.message}")
        } finally {
            cursor?.close()
        }
        
        results
    }

    private suspend fun getContactDetails(contactId: Long): Map<String, Any> = withContext(Dispatchers.IO) {
        val details = mutableMapOf<String, Any>()
        var cursor: Cursor? = null
        
        try {
            // Get phone numbers
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )
            
            val phones = mutableListOf<String>()
            cursor?.let {
                while (it.moveToNext()) {
                    try {
                        phones.add(it.getString(
                            it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        ) ?: "")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing phone number: ${e.message}")
                    }
                }
            }
            details["phones"] = phones
            cursor?.close()

            // Get email addresses
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )
            
            val emails = mutableListOf<String>()
            cursor?.let {
                while (it.moveToNext()) {
                    try {
                        emails.add(it.getString(
                            it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                        ) ?: "")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing email: ${e.message}")
                    }
                }
            }
            details["emails"] = emails
            cursor?.close()

        } catch (e: Exception) {
            Log.e(TAG, "Error getting contact details: ${e.message}")
        } finally {
            cursor?.close()
        }
        
        details
    }
}