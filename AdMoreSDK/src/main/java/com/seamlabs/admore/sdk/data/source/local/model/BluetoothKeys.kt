package com.seamlabs.admore.sdk.data.source.local.model

enum class BluetoothKeys(val key: String) {
    BLUETOOTH_ENABLED("bluetooth_enabled"),
    BLUETOOTH_NAME("bluetooth_name"),
    BLUETOOTH_ADDRESS("bluetooth_address"),
    BLUETOOTH_DEVICES("bluetooth_devices"),
    NEARBY_DEVICES("nearby_devices"),
    
    // Device info keys
    DEVICE_NAME("name"),
    DEVICE_ADDRESS("address"),
    DEVICE_TYPE("type"),
    DEVICE_BOND_STATE("bond_state"),
    DEVICE_RSSI("rssi"),
    DEVICE_UUIDS("uuids");

    fun toKey(): String = key
} 