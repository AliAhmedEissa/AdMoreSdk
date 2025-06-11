package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.seamlabs.admore.core.storage.ContentResolverUtils
import com.seamlabs.admore.data.source.local.model.CalendarKeys
import com.seamlabs.admore.domain.model.Permission
import javax.inject.Inject

/**
 * Collector for calendar data.
 */
class CalendarCollector @Inject constructor(
    context: Context,
    private val timeManager: CollectorTimeManager
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
        if (!isPermissionGranted() || !timeManager.shouldCollectCalendar()) {
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
                        CalendarKeys.EVENT_TITLE.toKey() to (event["title"] as? String ?: "Untitled"),
                        CalendarKeys.EVENT_DESCRIPTION.toKey() to (event["description"] as? String ?: ""),
                        CalendarKeys.EVENT_START_TIME.toKey() to (event["start_time"] as? Long ?: 0L),
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
            
            // Update collection time after successful collection
            timeManager.updateCalendarCollectionTime()
            
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