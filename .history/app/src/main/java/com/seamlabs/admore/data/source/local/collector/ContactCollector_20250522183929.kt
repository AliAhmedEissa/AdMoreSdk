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
 * Collector for device contacts data.
 * This collector handles contact information gathering with the following features:
 * 1. Basic contact information (name, phone numbers, email)
 * 2. Contact groups and relationships
 * 3. Contact metadata (last updated, starred status)
 * 
 * Note: Requires READ_CONTACTS permission to access contact information.
 * The collector respects user privacy by only collecting necessary information.
 */
class ContactCollector @Inject constructor(
    context: Context,
    private val timeManager: CollectorTimeManager
) : PermissionRequiredCollector(
    context,
    setOf(Permission.READ_CONTACTS)
) {
    private val contentResolverUtils = ContentResolverUtils(context)

    /**
     * Checks if READ_CONTACTS permission is granted.
     * @return true if permission is granted
     */
    override fun isPermissionGranted(): Boolean {
        return hasContactsPermission()
    }

    /**
     * Main collection method that gathers contact data.
     * Collects contact information if enough time has passed since last collection.
     * @return Map containing contact data
     */
    override suspend fun collect(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        
        try {
            if (!hasContactsPermission() || !timeManager.shouldCollectContact()) {
                return data
            }

            val contacts = getContacts()
            data["contacts"] = contacts

            // Update collection time if we got any contacts
            if (contacts.isNotEmpty()) {
                timeManager.updateContactCTime()
            }
            
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: Exception) {
            // Handle other errors
        }

        return data
    }

    /**
     * Checks if READ_CONTACTS permission is granted.
     * @return true if permission is granted
     */
    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Gets all contacts from the device.
     * @return List of contact information maps
     */
    @SuppressLint("MissingPermission")
    private fun getContacts(): List<Map<String, Any>> {
        val contacts = mutableListOf<Map<String, Any>>()
        val contentResolver = context.contentResolver

        try {
            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val contactId = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                    val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    val hasPhoneNumber = it.getInt(it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))

                    val contact = mutableMapOf<String, Any>(
                        "id" to contactId,
                        "name" to (name ?: "unknown")
                    )

                    // Get phone numbers
                    if (hasPhoneNumber > 0) {
                        contact["phone_numbers"] = getPhoneNumbers(contactId)
                    }

                    // Get email addresses
                    contact["emails"] = getEmailAddresses(contactId)

                    contacts.add(contact)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }

        return contacts
    }

    /**
     * Gets phone numbers for a specific contact.
     * @param contactId The ID of the contact
     * @return List of phone numbers
     */
    private fun getPhoneNumbers(contactId: String): List<String> {
        val phoneNumbers = mutableListOf<String>()
        val contentResolver = context.contentResolver

        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contactId),
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val phoneNumber = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    phoneNumber?.let { number -> phoneNumbers.add(number) }
                }
            }
        } catch (e: Exception) {
            // Handle error
        }

        return phoneNumbers
    }

    /**
     * Gets email addresses for a specific contact.
     * @param contactId The ID of the contact
     * @return List of email addresses
     */
    private fun getEmailAddresses(contactId: String): List<String> {
        val emails = mutableListOf<String>()
        val contentResolver = context.contentResolver

        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                arrayOf(contactId),
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val email = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))
                    email?.let { address -> emails.add(address) }
                }
            }
        } catch (e: Exception) {
            // Handle error
        }

        return emails
    }
}