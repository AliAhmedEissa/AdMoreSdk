// File: com.seamlabs.admore/domain/usecase/SendEventUseCase.kt
package com.seamlabs.admore.domain.usecase

import com.seamlabs.admore.domain.repository.DeviceDataRepository
import com.seamlabs.admore.domain.repository.EventRepository
import com.seamlabs.admore.domain.repository.PermissionRepository
import javax.inject.Inject

/**
 * Use case for sending events with data.
 */
class SendEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val deviceDataRepository: DeviceDataRepository,
    private val permissionRepository: PermissionRepository
) {
    /**
     * Executes the use case.
     * @param eventName Name of the event
     * @param eventData Map of event data
     * @param uniqueKey The unique key for identifying the app
     */
    suspend fun execute(eventName: String, eventData: Map<String, Any>, uniqueKey: String) {
        // Create mutable map to store all collected data
        val combinedData = mutableMapOf<String, Any>()
        
        // Add event data first
        combinedData.putAll(eventData)
        combinedData["unique_key"] = uniqueKey

        // Get advertising ID first
        val adId = deviceDataRepository.getAdvertisingId()
        combinedData["advertising_id"] = adId ?: ""

        // Get all granted permissions
        val grantedPermissions = permissionRepository.getGrantedPermissions()
        
        // Create a set to track which collector types we've already processed
        val processedCollectorTypes = mutableSetOf<Class<*>>()
        
        // Collect data for each permission only once per collector type
        grantedPermissions.forEach { permission ->
            val permissionData = deviceDataRepository.collectDataForPermission(permission)
            
            // Only add data from collectors we haven't processed yet
            permissionData.forEach { (key, value) ->
                if (!processedCollectorTypes.contains(value.javaClass)) {
                    combinedData[key] = value
                    processedCollectorTypes.add(value.javaClass)
                }
            }
        }

        // Send the event
        eventRepository.sendEvent(eventName, combinedData)
    }
}