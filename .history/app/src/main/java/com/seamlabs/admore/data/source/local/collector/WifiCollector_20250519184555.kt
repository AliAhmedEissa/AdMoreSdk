// File: com.seamlabs.admore/data/source/local/collector/WifiCollector.kt
package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.seamlabs.admore.domain.model.Permission
import javax.inject.Inject

/**
 * Collector for WiFi data.
 */
class WifiCollector @Inject constructor(
    context: Context
) : PermissionRequiredCollector(
    context,
    setOf(Permission.WIFI_STATE, Permission.LOCATION_FINE)
) {

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun collect(): Map<String, Any> {
        if (!isPermissionGranted()) {
            return emptyMap()
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        return mapOf(
            "wifi_enabled" to wifiManager.isWifiEnabled,
            "wifi_ssid" to getSsid(wifiInfo),
            "wifi_bssid" to getBssid(wifiInfo),
            "wifi_rssi" to wifiInfo.rssi,
            "wifi_link_speed" to wifiInfo.linkSpeed,
            "wifi_mac_address" to getMacAddress(wifiInfo)
        )
    }

    private fun getSsid(wifiInfo: WifiInfo): String {
        return try {
            val ssid = wifiInfo.ssid
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid.substring(1, ssid.length - 1)
            } else {
                ssid
            }
        } catch (e: SecurityException) {
            "permission_error"
        }
    }


    private fun getBssid(wifiInfo: WifiInfo): String {
        return try {
            wifiInfo.bssid ?: "unknown"
        } catch (e: SecurityException) {
            "permission_error"
        }
    }

    private fun getMacAddress(wifiInfo: WifiInfo): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                "redacted_in_android_6_plus"
            } else {
                @Suppress("DEPRECATION")
                wifiInfo.macAddress
            }
        } catch (e: SecurityException) {
            "permission_error"
        }
    }
}