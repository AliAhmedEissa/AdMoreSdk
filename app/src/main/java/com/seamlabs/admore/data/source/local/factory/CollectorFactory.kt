// File: com.seamlabs.admore/data/source/local/factory/CollectorFactory.kt
package com.seamlabs.admore.data.source.local.factory

import com.seamlabs.admore.data.source.local.collector.AdvertisingIdCollector
import com.seamlabs.admore.data.source.local.collector.BaseCollector
import com.seamlabs.admore.data.source.local.collector.PermissionRequiredCollector
import com.seamlabs.admore.domain.model.Permission

/**
 * Interface for factory that creates data collectors.
 */
interface CollectorFactory {
    /**
     * Gets all base collectors that don't require permissions.
     * @return List of base collectors
     */
    fun getBaseCollectors(): List<@JvmSuppressWildcards BaseCollector>

    /**
     * Gets a collector for a specific permission.
     * @param permission The permission to get a collector for
     * @return PermissionRequiredCollector for the permission, or null if none exists
     */
    fun getCollectorForPermission(permission: Permission): PermissionRequiredCollector?

    /**
     * Gets all permission-required collectors.
     * @return List of permission-required collectors
     */
    fun getPermissionRequiredCollectors(): List<PermissionRequiredCollector>

    /**
     * Gets the advertising ID collector.
     * @return AdvertisingIdCollector
     */
    fun getAdvertisingIdCollector(): AdvertisingIdCollector


}