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
        // Get base device data
        val deviceData = deviceDataRepository.collectBaseData()

        // Create mutable map to store all collected data
        val combinedData = mutableMapOf<String, Any>()
        combinedData.putAll(deviceData)
        combinedData.putAll(eventData)
        combinedData["unique_key"] = uniqueKey

        // Add data from each permission
        permissionRepository.getGrantedPermissions().forEach { permission ->
            val permissionData = deviceDataRepository.collectDataForPermission(permission)
            combinedData.putAll(permissionData)
        }

        // Get advertising ID
        val adId = deviceDataRepository.getAdvertisingId()
        combinedData["advertising_id"] = adId ?: ""

        // Send the event
        eventRepository.sendEvent(eventName, combinedData)
    }
}