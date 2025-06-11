// File: com.seamlabs.admore/data/source/local/PermissionChecker.kt
package com.seamlabs.admore.sdk.data.source.local

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.seamlabs.admore.sdk.domain.model.Permission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for checking permissions.
 */
@Singleton
class PermissionChecker @Inject constructor(
   private val context: Context
) {
    /**
     * Checks if a permission is granted.
     * @param permission The permission to check
     * @return true if the permission is granted, false otherwise
     */
    fun checkPermission(permission: Permission): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission.manifestPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if multiple permissions are granted.
     * @param permissions The permissions to check
     * @return true if all permissions are granted, false otherwise
     */
    fun checkPermissions(permissions: List<Permission>): Boolean {
        return permissions.all { checkPermission(it) }
    }
}