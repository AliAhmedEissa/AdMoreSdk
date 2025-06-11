package com.seamlabs.admore.sdk.data.source.local.model

enum class CalendarKeys(val key: String) {
    // Calendar list
    CALENDARS("calendars"),
    
    // Calendar info
    CALENDAR_ID("calendar_id"),
    CALENDAR_NAME("calendar_name"),
    CALENDAR_OWNER("calendar_owner"),
    CALENDAR_COLOR("calendar_color"),
    CALENDAR_ACCESS_LEVEL("access_level"),
    CALENDAR_VISIBLE("is_visible"),
    CALENDAR_SYNC_ENABLED("is_sync_enabled"),
    
    // Event info
    EVENTS("events"),
    EVENT_ID("event_id"),
    EVENT_TITLE("event_title"),
    EVENT_DESCRIPTION("event_description"),
    EVENT_LOCATION("event_location"),
    EVENT_START_TIME("start_time"),
    EVENT_END_TIME("end_time"),
    EVENT_ALL_DAY("is_all_day"),
    EVENT_REMINDER("reminder"),
    EVENT_ORGANIZER("organizer"),
    EVENT_ATTENDEES("attendees");

    fun toKey(): String = key
} 