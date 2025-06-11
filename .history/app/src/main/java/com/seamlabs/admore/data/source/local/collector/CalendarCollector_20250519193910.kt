package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.seamlabs.admore.data.source.local.model.CalendarKeys
import com.seamlabs.admore.domain.model.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Collector for calendar data with improved device compatibility.
 */
class CalendarCollector @Inject constructor(
    context: Context
) : PermissionRequiredCollector(
    context, setOf(Permission.CALENDAR)
) {

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override suspend fun collect(): Map<String, Any> = withContext(Dispatchers.IO) {
        if (!isPermissionGranted()) {
            return@withContext emptyMap()
        }

        val data = mutableMapOf<String, Any>()
        val calendars = mutableListOf<Map<String, Any>>()

        try {
            // Query calendars
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI, null, null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        calendars.add(getCalendarInfo(cursor))
                    } catch (e: Exception) {
                    }
                }
            }

            data[CalendarKeys.CALENDARS.toKey()] = calendars
        } catch (e: Exception) {
        }

        return@withContext data
    }

    private fun getCalendarInfo(cursor: Cursor): Map<String, Any> {
        val calendarData = mutableMapOf<String, Any>()
        try {
            // Get column indices safely
            val idColumnIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val calendarId = cursor.getLong(idColumnIndex)
            calendarData[CalendarKeys.CALENDAR_ID.toKey()] = calendarId

            // Get other fields safely
            getColumnStringValue(cursor, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)?.let {
                calendarData[CalendarKeys.CALENDAR_NAME.toKey()] = it
            }

            getColumnStringValue(cursor, CalendarContract.Calendars.OWNER_ACCOUNT)?.let {
                calendarData[CalendarKeys.CALENDAR_OWNER.toKey()] = it
            }

            getColumnIntValue(cursor, CalendarContract.Calendars.CALENDAR_COLOR)?.let {
                calendarData[CalendarKeys.CALENDAR_COLOR.toKey()] = it
            }

            getColumnIntValue(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)?.let {
                calendarData[CalendarKeys.CALENDAR_ACCESS_LEVEL.toKey()] = it
            }

            getColumnIntValue(cursor, CalendarContract.Calendars.VISIBLE)?.let {
                calendarData[CalendarKeys.CALENDAR_VISIBLE.toKey()] = it == 1
            }

            getColumnIntValue(cursor, CalendarContract.Calendars.SYNC_EVENTS)?.let {
                calendarData[CalendarKeys.CALENDAR_SYNC_ENABLED.toKey()] = it == 1
            }

            // Get events for this calendar
            val events = getCalendarEvents(calendarId)
            calendarData[CalendarKeys.EVENTS.toKey()] = events
        } catch (e: Exception) {
        }

        return calendarData
    }

    private fun getCalendarEvents(calendarId: Long): List<Map<String, Any>> {
        val events = mutableListOf<Map<String, Any>>()
        try {
            val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
            val selectionArgs = arrayOf(calendarId.toString())

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI, null, selection, selectionArgs, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        events.add(getEventInfo(cursor))
                    } catch (e: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
        }

        return events
    }

    private fun getEventInfo(cursor: Cursor): Map<String, Any> {
        val eventMap = mutableMapOf<String, Any>()

        try {
            getColumnLongValue(cursor, CalendarContract.Events._ID)?.let {
                eventMap[CalendarKeys.EVENT_ID.toKey()] = it
            }

            getColumnStringValue(cursor, CalendarContract.Events.TITLE)?.let {
                eventMap[CalendarKeys.EVENT_TITLE.toKey()] = it
            } ?: run {
                eventMap[CalendarKeys.EVENT_TITLE.toKey()] = "Untitled"
            }

            getColumnStringValue(cursor, CalendarContract.Events.DESCRIPTION)?.let {
                eventMap[CalendarKeys.EVENT_DESCRIPTION.toKey()] = it
            } ?: run {
                eventMap[CalendarKeys.EVENT_DESCRIPTION.toKey()] = ""
            }

            getColumnStringValue(cursor, CalendarContract.Events.EVENT_LOCATION)?.let {
                eventMap[CalendarKeys.EVENT_LOCATION.toKey()] = it
            } ?: run {
                eventMap[CalendarKeys.EVENT_LOCATION.toKey()] = ""
            }

            getColumnLongValue(cursor, CalendarContract.Events.DTSTART)?.let {
                eventMap[CalendarKeys.EVENT_START_TIME.toKey()] = it
            }

            getColumnLongValue(cursor, CalendarContract.Events.DTEND)?.let {
                eventMap[CalendarKeys.EVENT_END_TIME.toKey()] = it
            }

            getColumnIntValue(cursor, CalendarContract.Events.ALL_DAY)?.let {
                eventMap[CalendarKeys.EVENT_ALL_DAY.toKey()] = it == 1
            }

            getColumnStringValue(cursor, CalendarContract.Events.ORGANIZER)?.let {
                eventMap[CalendarKeys.EVENT_ORGANIZER.toKey()] = it
            } ?: run {
                eventMap[CalendarKeys.EVENT_ORGANIZER.toKey()] = ""
            }
        } catch (e: Exception) {
        }

        return eventMap
    }

    /**
     * Safe methods to get column values
     */
    private fun getColumnStringValue(cursor: Cursor, columnName: String): String? {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) cursor.getString(columnIndex) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getColumnIntValue(cursor: Cursor, columnName: String): Int? {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) cursor.getInt(columnIndex) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getColumnLongValue(cursor: Cursor, columnName: String): Long? {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) cursor.getLong(columnIndex) else null
        } catch (e: Exception) {
            null
        }
    }
}