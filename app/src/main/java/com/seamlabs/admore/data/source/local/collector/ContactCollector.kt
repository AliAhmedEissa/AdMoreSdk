package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.seamlabs.admore.core.storage.ContentResolverUtils
import com.seamlabs.admore.data.source.local.model.ContactKeys
import com.seamlabs.admore.domain.model.Permission
import javax.inject.Inject

/**
 * Collector for contacts data.
 */
class ContactCollector @Inject constructor(
    context: Context
) : PermissionRequiredCollector(
    context,
    setOf(Permission.CONTACTS)
) {
    private val contentResolverUtils =
        ContentResolverUtils(context)

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override suspend fun collect(): Map<String, Any> {
        if (!isPermissionGranted()) {
            return emptyMap()
        }

        val data = mutableMapOf<String, Any>()
        
        try {
            // Get contacts using ContentResolverUtils
            val contacts = contentResolverUtils.safeQueryContacts()
            
            // Transform the data to match ContactKeys
            val transformedContacts = contacts.map { contact ->
                mapOf(
                    ContactKeys.CONTACT_ID.toKey() to (contact["id"] as Long),
                    ContactKeys.CONTACT_NAME.toKey() to (contact["name"] as String),
                    ContactKeys.PHONE_NUMBERS.toKey() to (contact["phones"] as List<String>),
                    ContactKeys.EMAIL_ADDRESSES.toKey() to (contact["emails"] as List<String>)
                )
            }
            
            data[ContactKeys.CONTACTS.toKey()] = transformedContacts
        } catch (e: Exception) {
            // Log error but don't crash
            android.util.Log.e("ContactCollector", "Error collecting contacts: ${e.message}")
        }

        return data
    }
}