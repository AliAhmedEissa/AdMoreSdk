// File: com.seamlabs.admore/data/repository/DeviceDataRepositoryImpl.kt
package com.seamlabs.admore.data.repository

import com.seamlabs.admore.data.source.local.factory.CollectorFactory
import com.seamlabs.admore.domain.model.Permission
import com.seamlabs.admore.domain.repository.DeviceDataRepository
import javax.inject.Inject

/**
 * Implementation of DeviceDataRepository.
 */
class DeviceDataRepositoryImpl @Inject constructor(
    private val collectorFactory: CollectorFactory
) : DeviceDataRepository {

    override suspend fun initialize() {
        // Initialize collectors if needed
    }

    override suspend fun collectBaseData(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        
        // Collect data from base collectors
        collectorFactory.getBaseCollectors().forEach { collector ->
            data.putAll(collector.collect())
        }
        
        return data
    }

    override suspend fun collectDataForPermission(permission: Permission): Map<String, Any> {
        // Get collectors that require this permission
        val collector = collectorFactory.getCollectorForPermission(permission) ?: return emptyMap()
        
        // Collect data
        return collector.collect()
    }

    override suspend fun getAdvertisingId(): String? {
        return collectorFactory.getAdvertisingIdCollector().getAdvertisingId()
    }
}