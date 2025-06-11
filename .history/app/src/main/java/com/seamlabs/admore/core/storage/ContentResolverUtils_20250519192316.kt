/**
 * Helper class to safely access content providers with cursor callbacks
 */
package com.seamlabs.admore.core.storage

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri

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