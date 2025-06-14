package com.seamlabs.admore.sdk.data.source.local.factory

import com.seamlabs.admore.sdk.data.source.local.collector.AdvertisingIdCollector
import com.seamlabs.admore.sdk.data.source.local.collector.BaseCollector
import com.seamlabs.admore.sdk.data.source.local.collector.PermissionRequiredCollector
import com.seamlabs.admore.sdk.domain.model.Permission

/**
 * Implementation of CollectorFactory.
 */
class CollectorFactoryImpl(
    private val baseCollectors: List<BaseCollector>,
    private val permissionRequiredCollectors: List<PermissionRequiredCollector>
) : CollectorFactory {

    override fun getBaseCollectors(): List<BaseCollector> {
        return baseCollectors
    }

    override fun getCollectorsForPermission(permission: Permission): List<PermissionRequiredCollector> {
        return permissionRequiredCollectors.filter { it.requiredPermissions.contains(permission) }
            .distinctBy { it.javaClass }
    }

    override fun getPermissionRequiredCollectors(): List<PermissionRequiredCollector> {
        return permissionRequiredCollectors
    }

    override fun getAdvertisingIdCollector(): AdvertisingIdCollector {
        return baseCollectors.filterIsInstance<AdvertisingIdCollector>().first()
    }
}