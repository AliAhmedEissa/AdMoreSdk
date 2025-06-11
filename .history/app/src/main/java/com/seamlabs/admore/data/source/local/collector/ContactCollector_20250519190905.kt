package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
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
    setOf(Permission.READ_CONTACTS)
) {

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
        val contacts = mutableListOf<Map<String, Any>>()

        // Query contacts
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                contacts.add(getContactInfo(contactId, cursor))
            }
        }

        data[ContactKeys.CONTACTS.toKey()] = contacts
        return data
    }

    private fun getContactInfo(contactId: Long, cursor: Cursor): Map<String, Any> {
        val contactData = mutableMapOf<String, Any>()

        // Basic contact info
        contactData[ContactKeys.CONTACT_ID.toKey()] = contactId
        contactData[ContactKeys.CONTACT_NAME.toKey()] = cursor.getString(
            cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
        ) ?: "Unknown"
        contactData[ContactKeys.CONTACT_LAST_UPDATED.toKey()] = cursor.getLong(
            cursor.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
        )
        contactData[ContactKeys.CONTACT_STARRED.toKey()] = cursor.getInt(
            cursor.getColumnIndex(ContactsContract.Contacts.STARRED)
        ) == 1

        // Get detailed contact info
        contactData.putAll(getContactDetails(contactId))
        contactData.putAll(getContactPhones(contactId))
        contactData.putAll(getContactEmails(contactId))
        contactData.putAll(getContactAddresses(contactId))
        contactData.putAll(getContactOrganization(contactId))

        return contactData
    }

    private fun getContactDetails(contactId: Long): Map<String, Any> {
        val details = mutableMapOf<String, Any>()
        
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                details[ContactKeys.CONTACT_GIVEN_NAME.toKey()] = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
                ) ?: ""
                details[ContactKeys.CONTACT_FAMILY_NAME.toKey()] = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)
                ) ?: ""
                details[ContactKeys.CONTACT_MIDDLE_NAME.toKey()] = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)
                ) ?: ""
                details[ContactKeys.CONTACT_PREFIX.toKey()] = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PREFIX)
                ) ?: ""
                details[ContactKeys.CONTACT_SUFFIX.toKey()] = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)
                ) ?: ""
                details[ContactKeys.CONTACT_PHONETIC_NAME.toKey()] = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME)
                ) ?: ""
            }
        }

        return details
    }

    private fun getContactPhones(contactId: Long): Map<String, Any> {
        val phones = mutableListOf<Map<String, String>>()
        
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                phones.add(mapOf(
                    "number" to (cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    ) ?: ""),
                    "type" to cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    ) ?: "",
                    "label" to cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                    ) ?: ""
                ))
            }
        }

        return mapOf(ContactKeys.PHONE_NUMBERS.toKey() to phones)
    }

    private fun getContactEmails(contactId: Long): Map<String, Any> {
        val emails = mutableListOf<Map<String, String>>()
        
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                emails.add(mapOf(
                    "address" to (cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    ) ?: ""),
                    "type" to cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
                    ) ?: "",
                    "label" to cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL)
                    ) ?: ""
                ))
            }
        }

        return mapOf(ContactKeys.EMAIL_ADDRESSES.toKey() to emails)
    }

    private fun getContactAddresses(contactId: Long): Map<String, Any> {
        val addresses = mutableListOf<Map<String, String>>()
        
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                addresses.add(mapOf(
                    "street" to (cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)
                    ) ?: ""),
                    "city" to (cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)
                    ) ?: ""),
                    "state" to (cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)
                    ) ?: ""),
                    "postcode" to (cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)
                    ) ?: ""),
                    "country" to (cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)
                    ) ?: ""),
                    "type" to cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)
                    ) ?: "",
                    "label" to cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL)
                    ) ?: ""
                ))
            }
        }

        return mapOf(ContactKeys.POSTAL_ADDRESSES.toKey() to addresses)
    }

    private fun getContactOrganization(contactId: Long): Map<String, Any> {
        val orgData = mutableMapOf<String, Any>()
        
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                orgData[ContactKeys.ORGANIZATION.toKey()] = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
                ) ?: ""
                orgData[ContactKeys.JOB_TITLE.toKey()] = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)
                ) ?: ""
                orgData[ContactKeys.DEPARTMENT.toKey()] = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DEPARTMENT)
                ) ?: ""
            }
        }

        return orgData
    }
} 