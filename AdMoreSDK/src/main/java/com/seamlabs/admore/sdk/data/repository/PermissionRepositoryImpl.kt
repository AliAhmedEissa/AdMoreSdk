package com.seamlabs.admore.sdk.data.repository

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.seamlabs.admore.sdk.data.source.local.PermissionChecker
import com.seamlabs.admore.sdk.domain.model.Permission
import com.seamlabs.admore.sdk.domain.repository.PermissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Implementation of PermissionRepository.
 */
class PermissionRepositoryImpl(
    private val context: Context, private val permissionChecker: PermissionChecker
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
                context, permission.manifestPermission
            ) == PackageManager.PERMISSION_GRANTED
        }

        permissionsFlow.value = grantedPermissions
    }
}