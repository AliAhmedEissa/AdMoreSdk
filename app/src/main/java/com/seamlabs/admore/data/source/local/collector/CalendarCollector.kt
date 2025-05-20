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
    context: Context
) : PermissionRequiredCollector(
    context, setOf(Permission.CALENDAR)
) {
    private val contentResolverUtils =
      ContentResolverUtils(context)

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
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
            val transformedEvents = events.map { event ->
                mapOf(
                    CalendarKeys.EVENT_ID.toKey() to (event["id"] as Long),
                    CalendarKeys.EVENT_TITLE.toKey() to (event["title"] as String),
                    CalendarKeys.EVENT_DESCRIPTION.toKey() to (event["description"] as String),
                    CalendarKeys.EVENT_START_TIME.toKey() to (event["start_time"] as Long),
                    CalendarKeys.EVENT_END_TIME.toKey() to (event["end_time"] as Long)
                )
            }

            data[CalendarKeys.EVENTS.toKey()] = transformedEvents
        } catch (e: Exception) {
            // Log error but don't crash
            android.util.Log.e(
                "CalendarCollector",
                "Error collecting calendar events: ${e.message}"
            )
        }

        return data
    }
}