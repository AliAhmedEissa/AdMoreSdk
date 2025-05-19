package com.seamlabs.admore.data.source.local.model

enum class BluetoothKeys(val key: String) {
    BLUETOOTH_ENABLED("bluetooth_enabled"),
    BLUETOOTH_NAME("bluetooth_name"),
    BLUETOOTH_ADDRESS("bluetooth_address"),
    BLUETOOTH_DEVICES("bluetooth_devices"),
    
    // Device info keys
    DEVICE_NAME("name"),
    DEVICE_ADDRESS("address"),
    DEVICE_TYPE("type");

    fun toKey(): String = key
} 