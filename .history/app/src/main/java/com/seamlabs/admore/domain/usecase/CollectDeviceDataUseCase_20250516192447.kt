// File: com.seamlabs.admore/domain/usecase/CollectDeviceDataUseCase.kt
package com.seamlabs.admore.domain.usecase

import com.seamlabs.admore.domain.repository.DeviceDataRepository
import com.seamlabs.admore.domain.repository.PermissionRepository
import javax.inject.Inject

/**
 * Use case for collecting all available device data.
 */
class CollectDeviceDataUseCase @Inject constructor(
    private val deviceDataRepository: DeviceDataRepository,
    private val permissionRepository: PermissionRepository
) {
    /**
     * Executes the use case.
     * @return Map of all collected device data
     */
    suspend fun execute(): Map<String, Any> {
        // Get base device data
        val baseData = deviceDataRepository.collectBaseData()

        // Create mutable map to store all collected data
        val combinedData = mutableMapOf<String, Any>()
        combinedData.putAll(baseData)

        // Add data from each permission
        permissionRepository.getGrantedPermissions().forEach { permission ->
            val permissionData = deviceDataRepository.collectDataForPermission(permission)
            combinedData.putAll(permissionData)
        }

        // Add advertising ID if available
        val adId = deviceDataRepository.getAdvertisingId()
        adId?.let {
            combinedData["advertising_id"] = it
        }

        return combinedData
    }
}