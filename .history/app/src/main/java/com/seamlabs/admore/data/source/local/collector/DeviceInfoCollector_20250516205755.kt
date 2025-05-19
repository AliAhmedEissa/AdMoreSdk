// File: com.seamlabs.admore/data/source/local/collector/DeviceInfoCollector.kt
package com.seamlabs.admore.data.source.local.collector

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.WindowManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

/**
 * Collector for basic device information.
 * Collects all available data that doesn't require runtime permissions.
 */
class DeviceInfoCollector @Inject constructor(
    context: Context
) : BaseCollector(context) {

    private val packageManager = context.packageManager
    private val packageName = context.packageName
    private val packageInfo = packageManager.getPackageInfo(packageName, 0)

    override suspend fun collect(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        // Basic device info
        data.putAll(getDeviceInfo())

        // Screen and display info
        data.putAll(getDisplayInfo())

        // Hardware info
        data.putAll(getHardwareInfo())

        // Battery info
        data.putAll(getBatteryInfo())

        // App info
        data.putAll(getAppInfo())

        // Time and locale info
        data.putAll(getTimeInfo())

        // System features
        data.putAll(getSystemFeatures())

        return data
    }

    private fun getDeviceInfo(): Map<String, Any> {
        return mapOf(
            "device_manufacturer" to Build.MANUFACTURER,
            "device_model" to Build.MODEL,
            "device_brand" to Build.BRAND,
            "android_version" to Build.VERSION.RELEASE,
            "sdk_version" to Build.VERSION.SDK_INT,
            "device_id" to Build.ID,
            "build_type" to Build.TYPE,
            "build_fingerprint" to Build.FINGERPRINT,
            "build_tags" to Build.TAGS,
            "build_product" to Build.PRODUCT,
            "build_device" to Build.DEVICE,
            "build_board" to Build.BOARD,
            "is_emulator" to isEmulator(),
            "device_language" to context.resources.configuration.locales[0].language,
            "device_country" to context.resources.configuration.locales[0].country,
            "device_unique_id" to getDeviceUniqueId()
        )
    }

    private fun getDisplayInfo(): Map<String, Any> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = context.resources.displayMetrics

        val configuration = context.resources.configuration
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        return mapOf(
            "screen_width_px" to metrics.widthPixels,
            "screen_height_px" to metrics.heightPixels,
            "screen_density" to metrics.density,
            "screen_dpi" to metrics.densityDpi,
            "screen_scaled_density" to metrics.scaledDensity,
            "screen_is_portrait" to isPortrait,
            "screen_font_scale" to configuration.fontScale,
            "screen_size_category" to getScreenSizeCategory(configuration)
        )
    }

    private fun getHardwareInfo(): Map<String, Any> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val sensors = mutableListOf<String>()
        sensorManager?.getSensorList(Sensor.TYPE_ALL)?.forEach { sensor ->
            sensors.add(sensor.name)
        }

        return mapOf(
            "ram_total_mb" to (memoryInfo.totalMem / (1024 * 1024)),
            "ram_available_mb" to (memoryInfo.availMem / (1024 * 1024)),
            "ram_low_memory" to memoryInfo.lowMemory,
            "storage_total_mb" to (getTotalInternalStorage() / (1024 * 1024)),
            "storage_available_mb" to (getAvailableInternalStorage() / (1024 * 1024)),
            "storage_external_available" to isExternalStorageAvailable(),
            "processor_count" to Runtime.getRuntime().availableProcessors(),
            "sensors_count" to sensors.size
        )
    }

    private fun getBatteryInfo(): Map<String, Any> {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percentage = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()) else -1f

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        return mapOf(
            "battery_percentage" to percentage,
            "battery_is_charging" to isCharging,
            "battery_usb_charging" to usbCharge,
            "battery_ac_charging" to acCharge,
            "battery_status" to getBatteryStatusString(status),
            "battery_health" to getBatteryHealthString(
                batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
            ),
            "battery_temperature" to (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1) / 10f
        )
    }

    private fun getAppInfo(): Map<String, Any> {
        return mapOf(
            "app_package_name" to packageName,
            "app_version_name" to packageInfo.versionName,
            "app_version_code" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            },
            "app_first_install_time" to packageInfo.firstInstallTime,
            "app_last_update_time" to packageInfo.lastUpdateTime,
            "app_target_sdk" to context.applicationInfo.targetSdkVersion
        ) as Map<String, Any>
    }

    private fun getTimeInfo(): Map<String, Any> {
        val timezone = TimeZone.getDefault()
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        return mapOf(
            "time_zone_id" to timezone.id,
            "time_zone_display_name" to timezone.displayName,
            "time_zone_offset" to timezone.rawOffset / (1000 * 60 * 60),
            "time_zone_has_dst" to timezone.useDaylightTime(),
            "time_zone_in_dst" to timezone.inDaylightTime(currentDate),
            "device_time" to dateFormat.format(currentDate),
            "device_time_millis" to System.currentTimeMillis()
        )
    }

    private fun getSystemFeatures(): Map<String, Any> {
        val features = mutableMapOf<String, Boolean>()

        val importantFeatures = listOf(
            PackageManager.FEATURE_BLUETOOTH,
            PackageManager.FEATURE_CAMERA,
            PackageManager.FEATURE_CAMERA_FLASH,
            PackageManager.FEATURE_CAMERA_FRONT,
            PackageManager.FEATURE_FINGERPRINT,
            PackageManager.FEATURE_LOCATION,
            PackageManager.FEATURE_LOCATION_GPS,
            PackageManager.FEATURE_LOCATION_NETWORK,
            PackageManager.FEATURE_MICROPHONE,
            PackageManager.FEATURE_NFC,
            PackageManager.FEATURE_SCREEN_LANDSCAPE,
            PackageManager.FEATURE_SCREEN_PORTRAIT,
            PackageManager.FEATURE_SENSOR_ACCELEROMETER,
            PackageManager.FEATURE_SENSOR_BAROMETER,
            PackageManager.FEATURE_SENSOR_COMPASS,
            PackageManager.FEATURE_SENSOR_GYROSCOPE,
            PackageManager.FEATURE_SENSOR_LIGHT,
            PackageManager.FEATURE_SENSOR_PROXIMITY,
            PackageManager.FEATURE_SENSOR_STEP_COUNTER,
            PackageManager.FEATURE_SENSOR_STEP_DETECTOR,
            PackageManager.FEATURE_TOUCHSCREEN,
            PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH,
            PackageManager.FEATURE_WIFI,
            PackageManager.FEATURE_WIFI_DIRECT
        )

        importantFeatures.forEach { feature ->
            val featureName = feature.substring(feature.lastIndexOf('.') + 1).lowercase()
            features["feature_$featureName"] = packageManager.hasSystemFeature(feature)
        }

        return features
    }

    private fun getDeviceUniqueId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        return if (androidId != null && androidId != "9774d56d682e549c" && androidId.length > 8) {
            androidId
        } else {
            // Fallback to a generated UUID that persists across app installations
            val fallbackId = UUID.nameUUIDFromBytes(
                (Build.BOARD + Build.BRAND + Build.DEVICE + Build.MANUFACTURER + Build.MODEL + Build.PRODUCT).toByteArray()
            ).toString()

            fallbackId
        }
    }

    private fun getTotalInternalStorage(): Long {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            stat.blockSizeLong * stat.blockCountLong
        } catch (e: Exception) {
            0L
        }
    }

    private fun getAvailableInternalStorage(): Long {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            stat.blockSizeLong * stat.availableBlocksLong
        } catch (e: Exception) {
            0L
        }
    }

    private fun isExternalStorageAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    private fun getScreenSizeCategory(configuration: Configuration): String {
        return when (configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> "small"
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> "normal"
            Configuration.SCREENLAYOUT_SIZE_LARGE -> "large"
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> "xlarge"
            else -> "undefined"
        }
    }

    private fun getBatteryStatusString(status: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            BatteryManager.BATTERY_STATUS_UNKNOWN -> "unknown"
            else -> "undefined"
        }
    }

    private fun getBatteryHealthString(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_COLD -> "cold"
            BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over_voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "unspecified_failure"
            BatteryManager.BATTERY_HEALTH_UNKNOWN -> "unknown"
            else -> "undefined"
        }
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }
}