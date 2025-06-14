package com.seamlabs.admore.sdk.data.source.local.factory

import com.seamlabs.admore.sdk.data.source.local.collector.AdvertisingIdCollector
import com.seamlabs.admore.sdk.data.source.local.collector.BaseCollector
import com.seamlabs.admore.sdk.data.source.local.collector.PermissionRequiredCollector
import com.seamlabs.admore.sdk.domain.model.Permission

/**
 * Interface for factory that creates data collectors.
 */
interface CollectorFactory {
    /**
     * Gets all base collectors that don't require permissions.
     * @return List of base collectors
     */
    fun getBaseCollectors(): List<BaseCollector>

    /**
     * Gets collectors that require a specific permission.
     * @param permission The permission to get collectors for
     * @return List of PermissionRequiredCollector that require the permission
     */
    fun getCollectorsForPermission(permission: Permission): List<PermissionRequiredCollector>

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