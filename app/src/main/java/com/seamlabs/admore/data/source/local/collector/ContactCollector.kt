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
    private val contentResolverUtils = ContentResolverUtils(context)

    override fun isPermissionGranted(): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        } catch (e: SecurityException) {
            false
        } catch (e: Throwable) {
            false
        }
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
                try {
                    mapOf(
                        ContactKeys.CONTACT_ID.toKey() to (contact["id"] as? Long ?: 0L),
                        ContactKeys.CONTACT_NAME.toKey() to (contact["name"] as? String ?: "Unknown"),
                        ContactKeys.PHONE_NUMBERS.toKey() to (contact["phones"] as? List<String> ?: emptyList()),
                        ContactKeys.EMAIL_ADDRESSES.toKey() to (contact["emails"] as? List<String> ?: emptyList())
                    )
                } catch (e: Exception) {

                } catch (e: ClassCastException) {

                } catch (e: Throwable) {

                }
            }
            
            data[ContactKeys.CONTACTS.toKey()] = transformedContacts
        } catch (e: Exception) {
            // Silently handle error
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            data.clear()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        }

        return data
    }
}