package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.seamlabs.admore.data.source.local.model.ContactKeys
import com.seamlabs.admore.domain.model.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Collector for contacts data with improved device compatibility.
 */
class ContactCollector @Inject constructor(
    context: Context
) : PermissionRequiredCollector(
    context, setOf(Permission.CONTACTS)
) {

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override suspend fun collect(): Map<String, Any> = withContext(Dispatchers.IO) {
        if (!isPermissionGranted()) {
            return@withContext emptyMap()
        }

        val data = mutableMapOf<String, Any>()
        val contacts = mutableListOf<Map<String, Any>>()

        try {
            // Query contacts
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI, null, null, null, null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                if (idColumn == -1) {
                    return@withContext emptyMap()
                }

                while (cursor.moveToNext()) {
                    try {
                        val contactId = cursor.getLong(idColumn)
                        contacts.add(getContactInfo(contactId, cursor))
                    } catch (e: Exception) {
                    }
                }
            }

            data[ContactKeys.CONTACTS.toKey()] = contacts
        } catch (e: Exception) {
        }

        return@withContext data
    }

    private fun getContactInfo(contactId: Long, cursor: Cursor): Map<String, Any> {
        val contactData = mutableMapOf<String, Any>()

        try {
            // Basic contact info
            contactData[ContactKeys.CONTACT_ID.toKey()] = contactId

            getColumnStringValue(cursor, ContactsContract.Contacts.DISPLAY_NAME)?.let {
                contactData[ContactKeys.CONTACT_NAME.toKey()] = it
            } ?: run {
                contactData[ContactKeys.CONTACT_NAME.toKey()] = "Unknown"
            }

            // Contact last updated timestamp only available on API 18+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                getColumnLongValue(
                    cursor,
                    ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
                )?.let {
                    contactData[ContactKeys.CONTACT_LAST_UPDATED.toKey()] = it
                }
            }

            getColumnIntValue(cursor, ContactsContract.Contacts.STARRED)?.let {
                contactData[ContactKeys.CONTACT_STARRED.toKey()] = it == 1
            }

            // Get detailed contact info
            contactData.putAll(getContactDetails(contactId))
            contactData.putAll(getContactPhones(contactId))
            contactData.putAll(getContactEmails(contactId))
            contactData.putAll(getContactAddresses(contactId))
            contactData.putAll(getContactOrganization(contactId))
        } catch (e: Exception) {
        }

        return contactData
    }

    private fun getContactDetails(contactId: Long): Map<String, Any> {
        val details = mutableMapOf<String, Any>()

        try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(
                    contactId.toString(),
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                ),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME
                    )?.let {
                        details[ContactKeys.CONTACT_GIVEN_NAME.toKey()] = it
                    } ?: run {
                        details[ContactKeys.CONTACT_GIVEN_NAME.toKey()] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME
                    )?.let {
                        details[ContactKeys.CONTACT_FAMILY_NAME.toKey()] = it
                    } ?: run {
                        details[ContactKeys.CONTACT_FAMILY_NAME.toKey()] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME
                    )?.let {
                        details[ContactKeys.CONTACT_MIDDLE_NAME.toKey()] = it
                    } ?: run {
                        details[ContactKeys.CONTACT_MIDDLE_NAME.toKey()] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredName.PREFIX
                    )?.let {
                        details[ContactKeys.CONTACT_PREFIX.toKey()] = it
                    } ?: run {
                        details[ContactKeys.CONTACT_PREFIX.toKey()] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredName.SUFFIX
                    )?.let {
                        details[ContactKeys.CONTACT_SUFFIX.toKey()] = it
                    } ?: run {
                        details[ContactKeys.CONTACT_SUFFIX.toKey()] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME
                    )?.let {
                        details[ContactKeys.CONTACT_PHONETIC_NAME.toKey()] = it
                    } ?: run {
                        details[ContactKeys.CONTACT_PHONETIC_NAME.toKey()] = ""
                    }
                }
            }
        } catch (e: Exception) {
        }

        return details
    }

    private fun getContactPhones(contactId: Long): Map<String, Any> {
        val phones = mutableListOf<Map<String, String>>()

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val phoneMap = mutableMapOf<String, String>()

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )?.let {
                        phoneMap["number"] = it
                    } ?: run {
                        phoneMap["number"] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.Phone.TYPE.toString()
                    )?.let {
                        phoneMap["type"] = it
                    } ?: run {
                        phoneMap["type"] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.Phone.LABEL
                    )?.let {
                        phoneMap["label"] = it
                    } ?: run {
                        phoneMap["label"] = ""
                    }

                    phones.add(phoneMap)
                }
            }
        } catch (e: Exception) {
        }

        return mapOf(ContactKeys.PHONE_NUMBERS.toKey() to phones)
    }

    private fun getContactEmails(contactId: Long): Map<String, Any> {
        val emails = mutableListOf<Map<String, String>>()

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val emailMap = mutableMapOf<String, String>()

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.Email.ADDRESS
                    )?.let {
                        emailMap["address"] = it
                    } ?: run {
                        emailMap["address"] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.Email.TYPE.toString()
                    )?.let {
                        emailMap["type"] = it
                    } ?: run {
                        emailMap["type"] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.Email.LABEL
                    )?.let {
                        emailMap["label"] = it
                    } ?: run {
                        emailMap["label"] = ""
                    }

                    emails.add(emailMap)
                }
            }
        } catch (e: Exception) {
        }

        return mapOf(ContactKeys.EMAIL_ADDRESSES.toKey() to emails)
    }

    private fun getContactAddresses(contactId: Long): Map<String, Any> {
        val addresses = mutableListOf<Map<String, String>>()

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                null,
                "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val addressMap = mutableMapOf<String, String>()

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredPostal.STREET
                    )?.let {
                        addressMap["street"] = it
                    } ?: run {
                        addressMap["street"] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredPostal.CITY
                    )?.let {
                        addressMap["city"] = it
                    } ?: run {
                        addressMap["city"] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredPostal.REGION
                    )?.let {
                        addressMap["state"] = it
                    } ?: run {
                        addressMap["state"] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE
                    )?.let {
                        addressMap["postcode"] = it
                    } ?: run {
                        addressMap["postcode"] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY
                    )?.let {
                        addressMap["country"] = it
                    } ?: run {
                        addressMap["country"] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredPostal.TYPE.toString()
                    )?.let {
                        addressMap["type"] = it
                    } ?: run {
                        addressMap["type"] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.StructuredPostal.LABEL
                    )?.let {
                        addressMap["label"] = it
                    } ?: run {
                        addressMap["label"] = ""
                    }

                    addresses.add(addressMap)
                }
            }
        } catch (e: Exception) {
        }

        return mapOf(ContactKeys.POSTAL_ADDRESSES.toKey() to addresses)
    }

    private fun getContactOrganization(contactId: Long): Map<String, Any> {
        val orgData = mutableMapOf<String, Any>()

        try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(
                    contactId.toString(),
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                ),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.Organization.COMPANY
                    )?.let {
                        orgData[ContactKeys.ORGANIZATION.toKey()] = it
                    } ?: run {
                        orgData[ContactKeys.ORGANIZATION.toKey()] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.Organization.TITLE
                    )?.let {
                        orgData[ContactKeys.JOB_TITLE.toKey()] = it
                    } ?: run {
                        orgData[ContactKeys.JOB_TITLE.toKey()] = ""
                    }

                    getColumnStringValue(
                        cursor,
                        ContactsContract.CommonDataKinds.Organization.DEPARTMENT
                    )?.let {
                        orgData[ContactKeys.DEPARTMENT.toKey()] = it
                    } ?: run {
                        orgData[ContactKeys.DEPARTMENT.toKey()] = ""
                    }
                }
            }
        } catch (e: Exception) {
        }

        return orgData
    }

    /**
     * Safe methods to get column values
     */
    private fun getColumnStringValue(cursor: Cursor, columnName: String): String? {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) cursor.getString(columnIndex) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getColumnIntValue(cursor: Cursor, columnName: String): Int? {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) cursor.getInt(columnIndex) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getColumnLongValue(cursor: Cursor, columnName: String): Long? {
        return try {
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex != -1) cursor.getLong(columnIndex) else null
        } catch (e: Exception) {
            null
        }
    }
}