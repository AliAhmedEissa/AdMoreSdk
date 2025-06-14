package com.seamlabs.admore.sdk.data.repository

import com.seamlabs.admore.sdk.data.source.local.factory.CollectorFactory
import com.seamlabs.admore.sdk.domain.model.Permission
import com.seamlabs.admore.sdk.domain.repository.DeviceDataRepository

/**
 * Implementation of DeviceDataRepository.
 */
class DeviceDataRepositoryImpl(
    private val collectorFactory: CollectorFactory
) : DeviceDataRepository {

    // Cache for collector results
    private val collectorResults = mutableMapOf<Class<*>, Map<String, Any>>()

    override suspend fun initialize() {
        // Clear cache on initialization
        collectorResults.clear()
    }

    override suspend fun collectBaseData(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        // Collect data from base collectors
        collectorFactory.getBaseCollectors().forEach { collector ->
            val collectorType = collector.javaClass
            if (!collectorResults.containsKey(collectorType)) {
                collectorResults[collectorType] = collector.collect()
            }
            data.putAll(collectorResults[collectorType] ?: emptyMap())
        }

        return data
    }

    override suspend fun collectDataForPermission(permission: Permission): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        // Get all collectors that require this permission
        val collectors = collectorFactory.getCollectorsForPermission(permission)

        // Collect data from each collector only if not already collected
        collectors.forEach { collector ->
            val collectorType = collector.javaClass
            if (!collectorResults.containsKey(collectorType)) {
                collectorResults[collectorType] = collector.collect()
            }
            data.putAll(collectorResults[collectorType] ?: emptyMap())
        }

        return data
    }

    override suspend fun getAdvertisingId(): String? {
        return collectorFactory.getAdvertisingIdCollector().getAdvertisingId()
    }
}