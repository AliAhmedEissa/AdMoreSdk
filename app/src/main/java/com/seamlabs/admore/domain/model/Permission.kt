package com.seamlabs.admore.domain.model
/**
 * Enum representing Android permissions.
 */
enum class Permission(val manifestPermission: String) {
    LOCATION_FINE("android.permission.ACCESS_FINE_LOCATION"),
    LOCATION_COARSE("android.permission.ACCESS_COARSE_LOCATION"),
    BLUETOOTH("android.permission.BLUETOOTH"),
    BLUETOOTH_ADMIN("android.permission.BLUETOOTH_ADMIN"),
    BLUETOOTH_CONNECT("android.permission.BLUETOOTH_CONNECT"),
    BLUETOOTH_SCAN("android.permission.BLUETOOTH_SCAN"),
    WIFI_STATE("android.permission.ACCESS_WIFI_STATE"),
    NETWORK_STATE("android.permission.ACCESS_NETWORK_STATE"),
    STORAGE_READ("android.permission.READ_EXTERNAL_STORAGE"),
    STORAGE_WRITE("android.permission.WRITE_EXTERNAL_STORAGE"),
    CAMERA("android.permission.CAMERA"),
    CONTACTS("android.permission.READ_CONTACTS"),
    PHONE_STATE("android.permission.READ_PHONE_STATE"),
    CALENDAR("android.permission.READ_CALENDAR")
}