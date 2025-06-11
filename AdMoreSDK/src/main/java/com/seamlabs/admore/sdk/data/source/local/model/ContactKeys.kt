package com.seamlabs.admore.sdk.data.source.local.model

enum class ContactKeys(val key: String) {
    // Contact list
    CONTACTS("contacts"),
    
    // Contact info
    CONTACT_ID("contact_id"),
    CONTACT_NAME("name"),
    CONTACT_DISPLAY_NAME("display_name"),
    CONTACT_GIVEN_NAME("given_name"),
    CONTACT_FAMILY_NAME("family_name"),
    CONTACT_MIDDLE_NAME("middle_name"),
    CONTACT_PREFIX("prefix"),
    CONTACT_SUFFIX("suffix"),
    CONTACT_PHONETIC_NAME("phonetic_name"),
    
    // Contact details
    PHONE_NUMBERS("phone_numbers"),
    EMAIL_ADDRESSES("email_addresses"),
    POSTAL_ADDRESSES("postal_addresses"),
    ORGANIZATION("organization"),
    JOB_TITLE("job_title"),
    DEPARTMENT("department"),
    COMPANY("company"),
    WEBSITE("website"),
    NOTES("notes"),
    
    // Contact metadata
    CONTACT_LAST_UPDATED("last_updated"),
    CONTACT_STARRED("is_starred"),
    CONTACT_PINNED("is_pinned"),
    CONTACT_FAVORITE("is_favorite"),
    CONTACT_GROUP_MEMBERSHIP("group_membership");

    fun toKey(): String = key
} 