// File: com.seamlabs.admore/data/source/local/collector/PermissionRequiredCollector.kt
package com.seamlabs.admore.sdk.data.source.local.collector

import android.content.Context
import com.seamlabs.admore.sdk.domain.model.Permission

/**
 * Base class for collectors that require permissions.
 */
abstract class PermissionRequiredCollector(
    context: Context,
    val requiredPermissions: Set<Permission>  // Now accepts a set of permissions
) : BaseCollector(context) {

    // Primary permission for backward compatibility and collector factory
    val primaryPermission: Permission
        get() = requiredPermissions.first()

    /**
     * Checks if all required permissions are granted.
     * @return true if all permissions are granted, false otherwise
     */
    abstract fun isPermissionGranted(): Boolean
}

