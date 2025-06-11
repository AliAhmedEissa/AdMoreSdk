package com.seamlabs.admore.sdk.domain.repository

import com.seamlabs.admore.sdk.domain.model.Permission
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for permission operations.
 */
interface PermissionRepository {
    /**
     * Gets a list of granted permissions.
     * @return List of granted permissions
     */
    suspend fun getGrantedPermissions(): List<Permission>

    /**
     * Observes permission changes.
     * @return Flow of lists of granted permissions
     */
    fun observePermissionChanges(): Flow<List<Permission>>

    /**
     * Checks if a permission is granted.
     * @param permission The permission to check
     * @return true if the permission is granted, false otherwise
     */
    suspend fun isPermissionGranted(permission: Permission): Boolean
}
