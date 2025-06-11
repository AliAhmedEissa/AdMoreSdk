// File: com.seamlabs.admore/data/source/local/collector/PermissionRequiredCollector.kt
package com.seamlabs.admore.data.source.local.collector

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.seamlabs.admore.domain.model.Permission

/**
 * Base class for collectors that require specific Android permissions.
 * This class extends BaseCollector and adds permission management functionality.
 * 
 * Features:
 * 1. Tracks required permissions for each collector
 * 2. Provides permission checking functionality
 * 3. Handles permission-related operations
 * 
 * @param context Application context
 * @param requiredPermissions Set of permissions required by the collector
 */
abstract class PermissionRequiredCollector(
    context: Context,
    private val requiredPermissions: Set<Permission>
) : BaseCollector(context) {

    // Primary permission for backward compatibility and collector factory
    val primaryPermission: Permission
        get() = requiredPermissions.first()

    /**
     * Checks if all required permissions are granted.
     * This method verifies that the application has been granted
     * all the permissions specified in requiredPermissions.
     * 
     * @return true if all required permissions are granted, false otherwise
     */
    override fun isPermissionGranted(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission.toManifestPermission()
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

