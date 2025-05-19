// File: com.seamlabs.admore/data/repository/PermissionRepositoryImpl.kt
package com.seamlabs.admore.data.repository

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.seamlabs.admore.data.source.local.PermissionChecker
import com.seamlabs.admore.domain.model.Permission
import com.seamlabs.admore.domain.repository.PermissionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * Implementation of PermissionRepository.
 */
class PermissionRepositoryImpl @Inject constructor(
    private val context: Context,
    private val permissionChecker: PermissionChecker
) : PermissionRepository {
    private val permissionsFlow = MutableStateFlow<List<Permission>>(emptyList())

    init {
        // Initial check of all permissions
        updateGrantedPermissions()
    }

    override suspend fun getGrantedPermissions(): List<Permission> {
        // Update permissions before returning
        updateGrantedPermissions()
        return permissionsFlow.value
    }

    override fun observePermissionChanges(): Flow<List<Permission>> {
        return permissionsFlow
    }

    override suspend fun isPermissionGranted(permission: Permission): Boolean {
        return permissionChecker.checkPermission(permission)
    }

    private fun updateGrantedPermissions() {
        val grantedPermissions = Permission.entries.filter { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission.manifestPermission
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        permissionsFlow.value = grantedPermissions
    }
}