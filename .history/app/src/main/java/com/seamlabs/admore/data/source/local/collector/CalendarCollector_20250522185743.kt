package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.seamlabs.admore.core.storage.ContentResolverUtils
import com.seamlabs.admore.data.source.local.model.CalendarKeys
import com.seamlabs.admore.domain.model.Permission
import javax.inject.Inject

/**
 * Collector for device calendar data.
 * This collector handles calendar information gathering with the following features:
 * 1. Calendar events and reminders
 * 2. Calendar metadata (name, color, access level)
 * 3. Event details (title, time, location, attendees)
 * 
 * Note: Requires READ_CALENDAR permission to access calendar information.
 * The collector respects user privacy by only collecting necessary information.
 */
class CalendarCollector @Inject constructor(
    context: Context,
    private val timeManager: CollectorTimeManager
) : PermissionRequiredCollector(
    context,
    setOf(Permission.READ_CALENDAR)
) {
    private val contentResolverUtils = ContentResolverUtils(context)

    /**
     * Checks if READ_CALENDAR permission is granted.
     * @return true if permission is granted
     */
    override fun isPermissionGranted(): Boolean {
        return hasCalendarPermission()
    }

    /**
     * Main collection method that gathers calendar data.
     * Collects calendar information if enough time has passed since last collection.
     * @return Map containing calendar data
     */
    override suspend fun collect(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        
        try {
            if (!hasCalendarPermission() || !timeManager.shouldCollectCalendar()) {
                return data
            }

            val calendars = getCalendars()
            data[CalendarKeys.CALENDARS.toKey()] = calendars

            // Update collection time if we got any calendars
            if (calendars.isNotEmpty()) {
                timeManager.updateCalendarCTime()
            }
            
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: Exception) {
            // Handle other errors
        }

        return data
    }

    /**
     * Checks if READ_CALENDAR permission is granted.
     * @return true if permission is granted
     */
    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Gets all calendars and their events from the device.
     * @return List of calendar information maps
     */
    @SuppressLint("MissingPermission")
    private fun getCalendars(): List<Map<String, Any>> {
        val calendars = mutableListOf<Map<String, Any>>()
        val contentResolver = context.contentResolver

        try {
            // Get all calendars
            val calendarCursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                null,
                null,
                null,
                null
            )

            calendarCursor?.use {
                while (it.moveToNext()) {
                    val calendarId = it.getLong(it.getColumnIndex(CalendarContract.Calendars._ID))
                    val name = it.getString(it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                    val color = it.getInt(it.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR))
                    val accessLevel = it.getInt(it.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL))

                    val calendar = mutableMapOf<String, Any>(
                        CalendarKeys.CALENDAR_ID.toKey() to calendarId,
                        CalendarKeys.CALENDAR_NAME.toKey() to (name ?: "unknown"),
                        CalendarKeys.CALENDAR_COLOR.toKey() to color,
                        CalendarKeys.CALENDAR_ACCESS_LEVEL.toKey() to accessLevel,
                        CalendarKeys.EVENTS.toKey() to getEvents(calendarId)
                    )

                    calendars.add(calendar)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }

        return calendars
    }

    /**
     * Gets events for a specific calendar.
     * @param calendarId The ID of the calendar
     * @return List of event information maps
     */
    private fun getEvents(calendarId: Long): List<Map<String, Any>> {
        val events = mutableListOf<Map<String, Any>>()
        val contentResolver = context.contentResolver

        try {
            val eventCursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                null,
                CalendarContract.Events.CALENDAR_ID + " = ?",
                arrayOf(calendarId.toString()),
                null
            )

            eventCursor?.use {
                while (it.moveToNext()) {
                    val eventId = it.getLong(it.getColumnIndex(CalendarContract.Events._ID))
                    val title = it.getString(it.getColumnIndex(CalendarContract.Events.TITLE))
                    val startTime = it.getLong(it.getColumnIndex(CalendarContract.Events.DTSTART))
                    val endTime = it.getLong(it.getColumnIndex(CalendarContract.Events.DTEND))
                    val location = it.getString(it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION))
                    val description = it.getString(it.getColumnIndex(CalendarContract.Events.DESCRIPTION))

                    val event = mapOf(
                        CalendarKeys.EVENT_ID.toKey() to eventId,
                        CalendarKeys.EVENT_TITLE.toKey() to (title ?: "unknown"),
                        CalendarKeys.EVENT_START_TIME.toKey() to startTime,
                        CalendarKeys.EVENT_END_TIME.toKey() to endTime,
                        CalendarKeys.EVENT_LOCATION.toKey() to (location ?: ""),
                        CalendarKeys.EVENT_DESCRIPTION.toKey() to (description ?: ""),
                        CalendarKeys.EVENT_ATTENDEES.toKey() to getAttendees(eventId)
                    )

                    events.add(event)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }

        return events
    }

    /**
     * Gets attendees for a specific event.
     * @param eventId The ID of the event
     * @return List of attendee information maps
     */
    private fun getAttendees(eventId: Long): List<Map<String, Any>> {
        val attendees = mutableListOf<Map<String, Any>>()
        val contentResolver = context.contentResolver

        try {
            val attendeeCursor = contentResolver.query(
                CalendarContract.Attendees.CONTENT_URI,
                null,
                CalendarContract.Attendees.EVENT_ID + " = ?",
                arrayOf(eventId.toString()),
                null
            )

            attendeeCursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(it.getColumnIndex(CalendarContract.Attendees.ATTENDEE_NAME))
                    val email = it.getString(it.getColumnIndex(CalendarContract.Attendees.ATTENDEE_EMAIL))
                    val status = it.getInt(it.getColumnIndex(CalendarContract.Attendees.ATTENDEE_STATUS))

                    val attendee = mapOf(
                        CalendarKeys.ATTENDEE_NAME.toKey() to (name ?: "unknown"),
                        CalendarKeys.ATTENDEE_EMAIL.toKey() to (email ?: ""),
                        CalendarKeys.ATTENDEE_STATUS.toKey() to status
                    )

                    attendees.add(attendee)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }

        return attendees
    }
}