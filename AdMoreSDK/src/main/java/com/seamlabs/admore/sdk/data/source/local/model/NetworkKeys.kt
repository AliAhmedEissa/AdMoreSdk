package com.seamlabs.admore.sdk.data.source.local.model

enum class NetworkKeys(val key: String) {
    // Basic connection info
    CONNECTION_TYPE("connection_type"),
    IS_CONNECTED("is_connected"),
    
    // Network capabilities
    HAS_INTERNET("has_internet"),
    HAS_VALIDATED("has_validated"),
    HAS_CAPTIVE_PORTAL("has_captive_portal"),
    HAS_NOT_RESTRICTED("has_not_restricted"),
    HAS_NOT_ROAMING("has_not_roaming"),
    HAS_NOT_METERED("has_not_metered"),
    HAS_NOT_SUSPENDED("has_not_suspended"),
    HAS_NOT_VPN("has_not_vpn"),
    
    // Network details
    DOWNLOAD_SPEED("download_speed"),
    UPLOAD_SPEED("upload_speed"),
    SIGNAL_STRENGTH("signal_strength"),
    NETWORK_ID("network_id"),
    SUBTYPE("subtype"),
    SUBTYPE_NAME("subtype_name"),
    EXTRA_INFO("extra_info"),
    REASON("reason"),
    FAILOVER("failover"),
    IS_ACTIVE("is_active"),
    IS_AVAILABLE("is_available"),
    IS_BLOCKED("is_blocked"),
    IS_FAILOVER("is_failover"),
    IS_ROAMING("is_roaming");

    fun toKey(): String = key
} 