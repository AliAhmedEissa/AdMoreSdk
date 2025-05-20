// File: com.seamlabs.admore/data/source/local/factory/CollectorFactoryImpl.kt
package com.seamlabs.admore.data.source.local.factory

import com.seamlabs.admore.data.source.local.collector.AdvertisingIdCollector
import com.seamlabs.admore.data.source.local.collector.BaseCollector
import com.seamlabs.admore.data.source.local.collector.PermissionRequiredCollector
import com.seamlabs.admore.domain.model.Permission
import javax.inject.Inject

/**
 * Implementation of CollectorFactory.
 */
class CollectorFactoryImpl @Inject constructor(
    private val baseCollectors: List<@JvmSuppressWildcards BaseCollector>,
    private val permissionRequiredCollectors: List<@JvmSuppressWildcards PermissionRequiredCollector>
) : CollectorFactory {

    override fun getBaseCollectors(): List<BaseCollector> {
        return baseCollectors
    }

    override fun getCollectorsForPermission(permission: Permission): List<PermissionRequiredCollector> {
        return permissionRequiredCollectors.filter { it.requiredPermissions.contains(permission) }
    }

    override fun getPermissionRequiredCollectors(): List<PermissionRequiredCollector> {
        return permissionRequiredCollectors
    }

    override fun getAdvertisingIdCollector(): AdvertisingIdCollector {
        return baseCollectors.filterIsInstance<AdvertisingIdCollector>().first()
    }
}