package com.seamlabs.admore.sdk.domain.repository

/**
 * Repository interface for event operations.
 */
interface EventRepository {
    /**
     * Initializes the repository with the unique key.
     * @param uniqueKey The unique key for identifying the app
     */
    suspend fun initialize(uniqueKey: String)

    /**
     * Sends an event with data.
     * @param eventName Name of the event
     * @param eventData Map of event data
     */
    suspend fun sendEvent(eventName: String, eventData: Map<String, Any>)
}
