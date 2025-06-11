// File: com.seamlabs.admore/data/source/local/collector/WifiCollector.kt
package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.seamlabs.admore.data.source.local.model.WifiKeys
import com.seamlabs.admore.domain.model.Permission
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Collector for WiFi data.
 * This collector handles three main functionalities:
 * 1. Basic WiFi state (requires ACCESS_WIFI_STATE permission)
 * 2. Current connection info (requires ACCESS_WIFI_STATE and location permission)
 * 3. Nearby networks scanning (requires CHANGE_WIFI_STATE and location permission)
 */
class WifiCollector @Inject constructor(
    context: Context,
    private val timeManager: CollectorTimeManager
) : PermissionRequiredCollector(
    context,
    setOf(
        Permission.ACCESS_FINE_LOCATION,
        Permission.WIFI_STATE,
        Permission.CHANGE_WIFI_STATE
    )
) {

    private var wifiManager: WifiManager? = null
    private var isScanning = false

    /**
     * Checks if any of the required permissions are granted.
     * The collector can work with partial permissions, but each feature
     * will check for its specific required permissions.
     * @return true if at least one permission is granted
     */
    override fun isPermissionGranted(): Boolean {
        return hasWifiStatePermission() || hasChangeWifiStatePermission() || hasLocationPermission()
    }

    /**
     * Main collection method that gathers WiFi data based on available permissions.
     * Features are collected in order of least to most permission requirements:
     * 1. Basic state (ACCESS_WIFI_STATE)
     * 2. Connection info (ACCESS_WIFI_STATE + location)
     * 3. Nearby networks (CHANGE_WIFI_STATE + location)
     * @return Map containing collected WiFi data
     */
    override suspend fun collect(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        
        try {
            wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            // Basic WiFi state - only needs ACCESS_WIFI_STATE
            if (hasWifiStatePermission()) {
                data[WifiKeys.WIFI_ENABLED.toKey()] = wifiManager?.isWifiEnabled == true
            }
            
            // Current connection info - needs ACCESS_WIFI_STATE and location permission
            if (hasWifiStatePermission() && hasLocationPermission()) {
                val wifiInfo = wifiManager?.connectionInfo
                if (wifiInfo != null) {
                    data.putAll(getCurrentConnectionInfo(wifiInfo))
                }
            }
            
            // Scan for nearby networks - needs CHANGE_WIFI_STATE and location permission
            if (hasChangeWifiStatePermission() && hasLocationPermission() && timeManager.shouldCollectWifi()) {
                val scanResults = withTimeoutOrNull(5000) { // 5 second timeout
                    scanForNearbyNetworks()
                } ?: emptyList()
                data[WifiKeys.NEARBY_NETWORKS.toKey()] = scanResults
                
                // Only update collection time if we got scan results
                if (scanResults.isNotEmpty()) {
                    timeManager.updateWiCTime()
                }
            }
            
        } catch (e: Exception) {
            // Silently handle error
        } catch (e: OutOfMemoryError) {
            // Handle memory issues
            data.clear()
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
        } catch (e: Throwable) {
            // Handle any other unexpected errors
        }

        return data
    }

    /**
     * Checks if ACCESS_WIFI_STATE permission is granted.
     * This permission is required for basic WiFi state information.
     * @return true if permission is granted
     */
    private fun hasWifiStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if CHANGE_WIFI_STATE permission is granted.
     * This permission is required for scanning nearby networks.
     * @return true if permission is granted
     */
    private fun hasChangeWifiStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CHANGE_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if any location permission is granted.
     * Either ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION is sufficient.
     * Required for connection info and scanning.
     * @return true if either location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Gets current WiFi connection information.
     * Includes SSID, BSSID, signal strength, and other connection details.
     * @param wifiInfo The current WiFi connection info
     * @return Map containing connection details
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentConnectionInfo(wifiInfo: WifiInfo): Map<String, Any> {
        return mapOf(
            WifiKeys.WIFI_SSID.toKey() to getSsid(wifiInfo),
            WifiKeys.WIFI_BSSID.toKey() to getBssid(wifiInfo),
            WifiKeys.WIFI_RSSI.toKey() to wifiInfo.rssi,
            WifiKeys.WIFI_LINK_SPEED.toKey() to wifiInfo.linkSpeed,
            WifiKeys.WIFI_FREQUENCY.toKey() to wifiInfo.frequency,
            WifiKeys.WIFI_MAC_ADDRESS.toKey() to getMacAddress(wifiInfo),
            WifiKeys.WIFI_IP_ADDRESS.toKey() to getIpAddress(wifiInfo),
            WifiKeys.WIFI_HIDDEN_SSID.toKey() to wifiInfo.hiddenSSID,
            WifiKeys.WIFI_NETWORK_ID.toKey() to wifiInfo.networkId
        )
    }

    /**
     * Gets the SSID of the current WiFi connection.
     * Removes quotes if present in the SSID.
     * @param wifiInfo The current WiFi connection info
     * @return The SSID string
     */
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

    /**
     * Gets the BSSID (MAC address) of the current WiFi connection.
     * @param wifiInfo The current WiFi connection info
     * @return The BSSID string
     */
    private fun getBssid(wifiInfo: WifiInfo): String {
        return try {
            wifiInfo.bssid ?: "unknown"
        } catch (e: SecurityException) {
            "permission_error"
        }
    }

    /**
     * Gets the MAC address of the WiFi interface.
     * Returns "redacted_in_android_6_plus" for Android 6.0 and above
     * due to privacy restrictions.
     * @param wifiInfo The current WiFi connection info
     * @return The MAC address string
     */
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

    /**
     * Gets the IP address of the current WiFi connection.
     * Converts the integer IP to a human-readable format.
     * @param wifiInfo The current WiFi connection info
     * @return The IP address string
     */
    private fun getIpAddress(wifiInfo: WifiInfo): String {
        return try {
            val ip = wifiInfo.ipAddress
            if (ip == 0) return "unknown"
            String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff
            )
        } catch (e: SecurityException) {
            "permission_error"
        }
    }

    /**
     * Scans for nearby WiFi networks.
     * Uses different methods based on Android version:
     * - Android R and above: Uses ScanResultsCallback
     * - Below Android R: Uses BroadcastReceiver
     * @return List of nearby networks with their details
     */
    @SuppressLint("MissingPermission")
    private suspend fun scanForNearbyNetworks(): List<Map<String, Any>> {
        if (wifiManager == null) return emptyList()

        return suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val scanCallback = object : WifiManager.ScanResultsCallback() {
                    override fun onScanResultsAvailable() {
                        if (!continuation.isCompleted) {
                            val results = wifiManager?.scanResults?.map { result ->
                                mapOf(
                                    WifiKeys.WIFI_SSID.toKey() to result.SSID,
                                    WifiKeys.WIFI_BSSID.toKey() to result.BSSID,
                                    WifiKeys.WIFI_RSSI.toKey() to result.level,
                                    WifiKeys.WIFI_FREQUENCY.toKey() to result.frequency,
                                    WifiKeys.WIFI_CAPABILITIES.toKey() to result.capabilities,
                                    WifiKeys.WIFI_CHANNEL_WIDTH.toKey() to getChannelWidth(result),
                                    WifiKeys.WIFI_STANDARD.toKey() to getWifiStandard(result)
                                )
                            } ?: emptyList()
                            continuation.resume(results)
                        }
                    }
                }

                try {
                    isScanning = true
                    wifiManager?.registerScanResultsCallback(
                        context.mainExecutor,
                        scanCallback
                    )
                    wifiManager?.startScan()
                } catch (e: SecurityException) {
                    continuation.resume(emptyList())
                }
            } else {
                // For Android versions below R
                val receiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                            try {
                                context.unregisterReceiver(this)
                                isScanning = false
                                
                                if (!continuation.isCompleted) {
                                    val results = wifiManager?.scanResults?.map { result ->
                                        mapOf(
                                            WifiKeys.WIFI_SSID.toKey() to result.SSID,
                                            WifiKeys.WIFI_BSSID.toKey() to result.BSSID,
                                            WifiKeys.WIFI_RSSI.toKey() to result.level,
                                            WifiKeys.WIFI_FREQUENCY.toKey() to result.frequency,
                                            WifiKeys.WIFI_CAPABILITIES.toKey() to result.capabilities,
                                            WifiKeys.WIFI_CHANNEL_WIDTH.toKey() to getChannelWidth(result),
                                            WifiKeys.WIFI_STANDARD.toKey() to getWifiStandard(result)
                                        )
                                    } ?: emptyList()
                                    continuation.resume(results)
                                }
                            } catch (e: Exception) {
                                if (!continuation.isCompleted) {
                                    continuation.resume(emptyList())
                                }
                            }
                        }
                    }
                }

                try {
                    isScanning = true
                    context.registerReceiver(
                        receiver,
                        android.content.IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                    )
                    wifiManager?.startScan()
                } catch (e: SecurityException) {
                    continuation.resume(emptyList())
                }
            }
        }
    }

    /**
     * Gets the channel width of a WiFi network.
     * Only available on Android R and above.
     * @param result The scan result
     * @return The channel width string
     */
    private fun getChannelWidth(result: ScanResult): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (result.channelWidth) {
                ScanResult.CHANNEL_WIDTH_20MHZ -> "20MHz"
                ScanResult.CHANNEL_WIDTH_40MHZ -> "40MHz"
                ScanResult.CHANNEL_WIDTH_80MHZ -> "80MHz"
                ScanResult.CHANNEL_WIDTH_160MHZ -> "160MHz"
                ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80MHz+"
                else -> "unknown"
            }
        } else {
            "unknown"
        }
    }

    /**
     * Gets the WiFi standard of a network.
     * Only available on Android R and above.
     * @param result The scan result
     * @return The WiFi standard string
     */
    private fun getWifiStandard(result: ScanResult): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (result.wifiStandard) {
                ScanResult.WIFI_STANDARD_LEGACY -> "legacy"
                ScanResult.WIFI_STANDARD_11N -> "802.11n"
                ScanResult.WIFI_STANDARD_11AC -> "802.11ac"
                ScanResult.WIFI_STANDARD_11AX -> "802.11ax"
                else -> "unknown"
            }
        } else {
            "unknown"
        }
    }
}