package com.seamlabs.admore.data.source.local.model

enum class SmsKeys(val key: String) {
    // SMS list
    MESSAGES("messages"),
    
    // Message info
    MESSAGE_ID("message_id"),
    MESSAGE_ADDRESS("address"),
    MESSAGE_BODY("body"),
    MESSAGE_DATE("date"),
    MESSAGE_TYPE("type"),
    MESSAGE_READ("is_read"),
    MESSAGE_SEEN("is_seen"),
    MESSAGE_SUBJECT("subject"),
    MESSAGE_SERVICE_CENTER("service_center"),
    MESSAGE_STATUS("status"),
    MESSAGE_THREAD_ID("thread_id"),
    
    // Thread info
    THREADS("threads"),
    THREAD_CONTACT("contact"),
    THREAD_MESSAGE_COUNT("message_count"),
    THREAD_SNIPPET("snippet"),
    THREAD_DATE("date");

    fun toKey(): String = key
} 