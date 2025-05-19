package com.seamlabs.admore.data.source.local.model

enum class NetworkKeys(val key: String) {
    CONNECTION_TYPE("connection_type"),
    IS_CONNECTED("is_connected");

    fun toKey(): String = key
} 