package com.seamlabs.admore.sdk.data.source.local.model

enum class WifiKeys(val key: String) {
    WIFI_ENABLED("wifi_enabled"),
    WIFI_SSID("wifi_ssid"),
    WIFI_BSSID("wifi_bssid"),
    WIFI_RSSI("wifi_rssi"),
    WIFI_LINK_SPEED("wifi_link_speed"),
    WIFI_FREQUENCY("wifi_frequency"),
    WIFI_MAC_ADDRESS("wifi_mac_address"),
    WIFI_IP_ADDRESS("wifi_ip_address"),
    WIFI_HIDDEN_SSID("wifi_hidden_ssid"),
    WIFI_NETWORK_ID("wifi_network_id"),
    NEARBY_NETWORKS("nearby_networks"),
    
    // Network info keys
    WIFI_CAPABILITIES("capabilities"),
    WIFI_CHANNEL_WIDTH("channel_width"),
    WIFI_STANDARD("wifi_standard");

    fun toKey(): String = key
} 