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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Collector for WiFi data.
 */
class WifiCollector @Inject constructor(
    context: Context,
    private val timeManager: CollectorTimeManager
) : PermissionRequiredCollector(
    context,
    setOf(Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_WIFI_STATE)
) {

    private var wifiManager: WifiManager? = null
    private var isScanning = false

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED &&
        (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    override suspend fun collect(): Map<String, Any> {
        if (!isPermissionGranted() || !timeManager.shouldCollectWifi()) {
            return emptyMap()
        }

        val data = mutableMapOf<String, Any>()
        
        try {
            wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager?.connectionInfo

            // Basic WiFi state
            data[WifiKeys.WIFI_ENABLED.toKey()] = wifiManager?.isWifiEnabled == true
            
            // Current connection info
            if (wifiInfo != null) {
                data.putAll(getCurrentConnectionInfo(wifiInfo))
            }
            
            // Scan for nearby networks if we have location permission
            if (hasLocationPermission()) {
                // Wait for scan results with timeout
                val scanResults = withTimeoutOrNull(5000) { // 5 second timeout
                    scanForNearbyNetworks()
                } ?: emptyList()
                data[WifiKeys.NEARBY_NETWORKS.toKey()] = scanResults
            }

            // Only update collection time if we got scan results
            if (data.containsKey(WifiKeys.NEARBY_NETWORKS.toKey())) {
                timeManager.updateWifiCollectionTime()
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