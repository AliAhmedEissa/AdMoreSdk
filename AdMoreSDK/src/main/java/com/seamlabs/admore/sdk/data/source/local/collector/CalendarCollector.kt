package com.seamlabs.admore.sdk.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.seamlabs.admore.sdk.core.storage.ContentResolverUtils
import com.seamlabs.admore.sdk.data.source.local.model.CalendarKeys
import com.seamlabs.admore.sdk.domain.model.Permission

/**
 * Collector for calendar data.
 */
class CalendarCollector(
    context: Context
) : PermissionRequiredCollector(
    context, setOf(Permission.CALENDAR)
) {
    private val contentResolverUtils = ContentResolverUtils(context)

    override fun isPermissionGranted(): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        } catch (e: SecurityException) {
            false
        } catch (e: Throwable) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun collect(): Map<String, Any> {
        if (!isPermissionGranted()) {
            return emptyMap()
        }

        val data = mutableMapOf<String, Any>()

        try {
            // Get calendar events using ContentResolverUtils
            val events = contentResolverUtils.safeQueryCalendarEvents(
                sortOrder = "${CalendarKeys.EVENT_START_TIME.toKey()} DESC"
            )

            // Transform the data to match CalendarKeys
            val transformedEvents = events.mapNotNull { event ->
                try {
                    mapOf(
                        CalendarKeys.EVENT_ID.toKey() to (event["id"] as? Long ?: 0L),
                        CalendarKeys.EVENT_TITLE.toKey() to (event["title"] as? String
                            ?: "Untitled"),
                        CalendarKeys.EVENT_DESCRIPTION.toKey() to (event["description"] as? String
                            ?: ""),
                        CalendarKeys.EVENT_START_TIME.toKey() to (event["start_time"] as? Long
                            ?: 0L),
                        CalendarKeys.EVENT_END_TIME.toKey() to (event["end_time"] as? Long ?: 0L)
                    )
                } catch (e: Exception) {
                    null
                } catch (e: ClassCastException) {
                    null
                } catch (e: Throwable) {
                    null
                }
            }

            data[CalendarKeys.EVENTS.toKey()] = transformedEvents
        } catch (e: Exception) {
            // Silently handle error
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            data.clear()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        }

        return data
    }
}