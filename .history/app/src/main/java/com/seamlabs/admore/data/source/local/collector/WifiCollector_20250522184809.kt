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
 * Collector for device WiFi data.
 * This collector handles WiFi information gathering with the following features:
 * 1. Current WiFi connection details
 * 2. Available WiFi networks
 * 3. WiFi signal strength and frequency
 * 4. WiFi security information
 * 
 * Note: Requires ACCESS_FINE_LOCATION and ACCESS_WIFI_STATE permissions.
 * The collector respects user privacy by only collecting necessary information.
 */
class WifiCollector @Inject constructor(
    context: Context,
    private val timeManager: CollectorTimeManager
) : PermissionRequiredCollector(
    context,
    setOf(Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_WIFI_STATE)
) {
    private var wifiManager: WifiManager? = null

    /**
     * Checks if required permissions are granted.
     * @return true if all required permissions are granted
     */
    override fun isPermissionGranted(): Boolean {
        return hasWifiPermissions()
    }

    /**
     * Main collection method that gathers WiFi data.
     * Collects WiFi information if enough time has passed since last collection.
     * @return Map containing WiFi data
     */
    override suspend fun collect(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        
        try {
            if (!hasWifiPermissions() || !timeManager.shouldCollectWifi()) {
                return data
            }

            wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

            // Get current WiFi connection
            val currentConnection = getCurrentConnection()
            if (currentConnection != null) {
                data[WifiKeys.CURRENT_CONNECTION.toKey()] = currentConnection
            }

            // Get available networks
            val availableNetworks = getAvailableNetworks()
            data[WifiKeys.AVAILABLE_NETWORKS.toKey()] = availableNetworks

            // Update collection time if we got any data
            if (data.isNotEmpty()) {
                timeManager.updateWifiCTime()
            }
            
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: Exception) {
            // Handle other errors
        }

        return data
    }

    /**
     * Checks if required WiFi permissions are granted.
     * @return true if all required permissions are granted
     */
    private fun hasWifiPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Gets information about the current WiFi connection.
     * @return Map containing current connection details or null if not connected
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentConnection(): Map<String, Any>? {
        val connectionInfo = wifiManager?.connectionInfo ?: return null
        val ssid = connectionInfo.ssid?.removeSurrounding("\"") ?: return null

        return mapOf(
            WifiKeys.SSID.toKey() to ssid,
            WifiKeys.BSSID.toKey() to connectionInfo.bssid,
            WifiKeys.SIGNAL_STRENGTH.toKey() to connectionInfo.rssi,
            WifiKeys.FREQUENCY.toKey() to connectionInfo.frequency,
            WifiKeys.LINK_SPEED.toKey() to connectionInfo.linkSpeed,
            WifiKeys.IP_ADDRESS.toKey() to connectionInfo.ipAddress,
            WifiKeys.NETWORK_ID.toKey() to connectionInfo.networkId
        )
    }

    /**
     * Gets information about available WiFi networks.
     * @return List of available network information maps
     */
    @SuppressLint("MissingPermission")
    private fun getAvailableNetworks(): List<Map<String, Any>> {
        val networks = mutableListOf<Map<String, Any>>()
        val scanResults = wifiManager?.scanResults ?: return networks

        for (result in scanResults) {
            val network = mapOf(
                WifiKeys.SSID.toKey() to result.SSID,
                WifiKeys.BSSID.toKey() to result.BSSID,
                WifiKeys.SIGNAL_STRENGTH.toKey() to result.level,
                WifiKeys.FREQUENCY.toKey() to result.frequency,
                WifiKeys.CHANNEL_WIDTH.toKey() to result.channelWidth,
                WifiKeys.CAPABILITIES.toKey() to result.capabilities,
                WifiKeys.TIMESTAMP.toKey() to result.timestamp
            )
            networks.add(network)
        }

        return networks
    }
}