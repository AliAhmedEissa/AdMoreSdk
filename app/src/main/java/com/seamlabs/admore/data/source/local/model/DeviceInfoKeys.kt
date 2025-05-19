package com.seamlabs.admore.data.source.local.model

enum class DeviceInfoKeys(val key: String) {
    // Device Info
    DEVICE_MANUFACTURER("device_manufacturer"),
    DEVICE_MODEL("device_model"),
    DEVICE_BRAND("device_brand"),
    ANDROID_VERSION("android_version"),
    SDK_VERSION("sdk_version"),
    DEVICE_ID("device_id"),
    BUILD_TYPE("build_type"),
    BUILD_FINGERPRINT("build_fingerprint"),
    BUILD_TAGS("build_tags"),
    BUILD_PRODUCT("build_product"),
    BUILD_DEVICE("build_device"),
    BUILD_BOARD("build_board"),
    IS_EMULATOR("is_emulator"),
    DEVICE_LANGUAGE("device_language"),
    DEVICE_COUNTRY("device_country"),
    DEVICE_UNIQUE_ID("device_unique_id"),
    ADVERTISING_ID("advertising_id"),

    // Display Info
    SCREEN_WIDTH_PX("screen_width_px"),
    SCREEN_HEIGHT_PX("screen_height_px"),
    SCREEN_DENSITY("screen_density"),
    SCREEN_DPI("screen_dpi"),
    SCREEN_SCALED_DENSITY("screen_scaled_density"),
    SCREEN_IS_PORTRAIT("screen_is_portrait"),
    SCREEN_FONT_SCALE("screen_font_scale"),
    SCREEN_SIZE_CATEGORY("screen_size_category"),

    // Hardware Info
    RAM_TOTAL_MB("ram_total_mb"),
    RAM_AVAILABLE_MB("ram_available_mb"),
    RAM_LOW_MEMORY("ram_low_memory"),
    STORAGE_TOTAL_MB("storage_total_mb"),
    STORAGE_AVAILABLE_MB("storage_available_mb"),
    STORAGE_EXTERNAL_AVAILABLE("storage_external_available"),
    PROCESSOR_COUNT("processor_count"),
    SENSORS_COUNT("sensors_count"),

    // Battery Info
    BATTERY_PERCENTAGE("battery_percentage"),
    BATTERY_IS_CHARGING("battery_is_charging"),
    BATTERY_USB_CHARGING("battery_usb_charging"),
    BATTERY_AC_CHARGING("battery_ac_charging"),
    BATTERY_STATUS("battery_status"),
    BATTERY_HEALTH("battery_health"),
    BATTERY_TEMPERATURE("battery_temperature"),

    // App Info
    APP_PACKAGE_NAME("app_package_name"),
    APP_VERSION_NAME("app_version_name"),
    APP_VERSION_CODE("app_version_code"),
    APP_FIRST_INSTALL_TIME("app_first_install_time"),
    APP_LAST_UPDATE_TIME("app_last_update_time"),
    APP_TARGET_SDK("app_target_sdk"),

    // Time Info
    TIME_ZONE_ID("time_zone_id"),
    TIME_ZONE_DISPLAY_NAME("time_zone_display_name"),
    TIME_ZONE_OFFSET("time_zone_offset"),
    TIME_ZONE_HAS_DST("time_zone_has_dst"),
    TIME_ZONE_IN_DST("time_zone_in_dst"),
    DEVICE_TIME("device_time"),
    DEVICE_TIME_MILLIS("device_time_millis"),

    // System Features
    FEATURE_BLUETOOTH("feature_bluetooth"),
    FEATURE_CAMERA("feature_camera"),
    FEATURE_CAMERA_FLASH("feature_camera_flash"),
    FEATURE_CAMERA_FRONT("feature_camera_front"),
    FEATURE_FINGERPRINT("feature_fingerprint"),
    FEATURE_LOCATION("feature_location"),
    FEATURE_LOCATION_GPS("feature_location_gps"),
    FEATURE_LOCATION_NETWORK("feature_location_network"),
    FEATURE_MICROPHONE("feature_microphone"),
    FEATURE_NFC("feature_nfc"),
    FEATURE_SCREEN_LANDSCAPE("feature_screen_landscape"),
    FEATURE_SCREEN_PORTRAIT("feature_screen_portrait"),
    FEATURE_SENSOR_ACCELEROMETER("feature_sensor_accelerometer"),
    FEATURE_SENSOR_BAROMETER("feature_sensor_barometer"),
    FEATURE_SENSOR_COMPASS("feature_sensor_compass"),
    FEATURE_SENSOR_GYROSCOPE("feature_sensor_gyroscope"),
    FEATURE_SENSOR_LIGHT("feature_sensor_light"),
    FEATURE_SENSOR_PROXIMITY("feature_sensor_proximity"),
    FEATURE_SENSOR_STEP_COUNTER("feature_sensor_step_counter"),
    FEATURE_SENSOR_STEP_DETECTOR("feature_sensor_step_detector"),
    FEATURE_TOUCHSCREEN("feature_touchscreen"),
    FEATURE_TOUCHSCREEN_MULTITOUCH("feature_touchscreen_multitouch"),
    FEATURE_WIFI("feature_wifi"),
    FEATURE_WIFI_DIRECT("feature_wifi_direct");

    fun toKey(): String = key
} 