package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.seamlabs.admore.data.source.local.model.CalendarKeys
import com.seamlabs.admore.domain.model.Permission
import javax.inject.Inject

/**
 * Collector for calendar data.
 */
class CalendarCollector @Inject constructor(
    context: Context
) : PermissionRequiredCollector(
    context,
    setOf(Permission.READ_CALENDAR)
) {

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override suspend fun collect(): Map<String, Any> {
        if (!isPermissionGranted()) {
            return emptyMap()
        }

        val data = mutableMapOf<String, Any>()
        val calendars = mutableListOf<Map<String, Any>>()

        // Query calendars
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                calendars.add(getCalendarInfo(cursor))
            }
        }

        data[CalendarKeys.CALENDARS.toKey()] = calendars
        return data
    }

    private fun getCalendarInfo(cursor: Cursor): Map<String, Any> {
        val calendarId = cursor.getLong(cursor.getColumnIndex(CalendarContract.Calendars._ID))
        val calendarData = mutableMapOf<String, Any>()

        // Basic calendar info
        calendarData[CalendarKeys.CALENDAR_ID.toKey()] = calendarId
        calendarData[CalendarKeys.CALENDAR_NAME.toKey()] = cursor.getString(
            cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        ) ?: "Unknown"
        calendarData[CalendarKeys.CALENDAR_OWNER.toKey()] = cursor.getString(
            cursor.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT)
        ) ?: "Unknown"
        calendarData[CalendarKeys.CALENDAR_COLOR.toKey()] = cursor.getInt(
            cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
        )
        calendarData[CalendarKeys.CALENDAR_ACCESS_LEVEL.toKey()] = cursor.getInt(
            cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
        )
        calendarData[CalendarKeys.CALENDAR_VISIBLE.toKey()] = cursor.getInt(
            cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)
        ) == 1
        calendarData[CalendarKeys.CALENDAR_SYNC_ENABLED.toKey()] = cursor.getInt(
            cursor.getColumnIndex(CalendarContract.Calendars.SYNC_EVENTS)
        ) == 1

        // Get events for this calendar
        val events = getCalendarEvents(calendarId)
        calendarData[CalendarKeys.EVENTS.toKey()] = events

        return calendarData
    }

    private fun getCalendarEvents(calendarId: Long): List<Map<String, Any>> {
        val events = mutableListOf<Map<String, Any>>()
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())

        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            null,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                events.add(getEventInfo(cursor))
            }
        }

        return events
    }

    private fun getEventInfo(cursor: Cursor): Map<String, Any> {
        return mapOf(
            CalendarKeys.EVENT_ID.toKey() to cursor.getLong(
                cursor.getColumnIndex(CalendarContract.Events._ID)
            ),
            CalendarKeys.EVENT_TITLE.toKey() to (cursor.getString(
                cursor.getColumnIndex(CalendarContract.Events.TITLE)
            ) ?: "Untitled"),
            CalendarKeys.EVENT_DESCRIPTION.toKey() to (cursor.getString(
                cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
            ) ?: ""),
            CalendarKeys.EVENT_LOCATION.toKey() to (cursor.getString(
                cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
            ) ?: ""),
            CalendarKeys.EVENT_START_TIME.toKey() to cursor.getLong(
                cursor.getColumnIndex(CalendarContract.Events.DTSTART)
            ),
            CalendarKeys.EVENT_END_TIME.toKey() to cursor.getLong(
                cursor.getColumnIndex(CalendarContract.Events.DTEND)
            ),
            CalendarKeys.EVENT_ALL_DAY.toKey() to (cursor.getInt(
                cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
            ) == 1),
            CalendarKeys.EVENT_ORGANIZER.toKey() to (cursor.getString(
                cursor.getColumnIndex(CalendarContract.Events.ORGANIZER)
            ) ?: "")
        )
    }
} 