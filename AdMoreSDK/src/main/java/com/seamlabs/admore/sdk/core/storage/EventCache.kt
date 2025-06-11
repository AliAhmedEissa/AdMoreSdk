// File: com.seamlabs.admore/core/storage/EventCache.kt
package com.seamlabs.admore.sdk.core.storage

import com.seamlabs.admore.sdk.domain.model.Event
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache for events.
 */
@Singleton
class EventCache @Inject constructor() {
    private val cachedEvents = mutableListOf<Event>()
    private val mutex = Mutex()
    
    /**
     * Adds an event to the cache.
     * @param event The event to add
     */
    suspend fun addEvent(event: Event) {
        mutex.withLock {
            cachedEvents.add(event)
        }
    }
    
    /**
     * Removes an event from the cache.
     * @param event The event to remove
     */
    suspend fun removeEvent(event: Event) {
        mutex.withLock {
            cachedEvents.remove(event)
        }
    }
    
    /**
     * Gets all events in the cache.
     * @return List of cached events
     */
    suspend fun getEvents(): List<Event> {
        return mutex.withLock {
            cachedEvents.toList()
        }
    }
    
    /**
     * Clears all events from the cache.
     */
    suspend fun clearEvents() {
        mutex.withLock {
            cachedEvents.clear()
        }
    }
}
