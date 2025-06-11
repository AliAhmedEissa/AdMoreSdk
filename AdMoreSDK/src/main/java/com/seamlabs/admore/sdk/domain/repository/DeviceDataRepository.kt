// File: com.seamlabs.admore/domain/repository/DeviceDataRepository.kt
package com.seamlabs.admore.sdk.domain.repository

import com.seamlabs.admore.sdk.domain.model.Permission

/**
 * Repository interface for device data operations.
 */
interface DeviceDataRepository {
    /**
     * Initializes the repository.
     */
    suspend fun initialize()

    /**
     * Collects base device data that doesn't require permissions.
     * @return Map of collected data
     */
    suspend fun collectBaseData(): Map<String, Any>

    /**
     * Collects data for a specific permission.
     * @param permission The permission to collect data for
     * @return Map of collected data
     */
    suspend fun collectDataForPermission(permission: Permission): Map<String, Any>

    /**
     * Gets the device's advertising ID.
     * @return The advertising ID, or null if not available
     */
    suspend fun getAdvertisingId(): String?
}