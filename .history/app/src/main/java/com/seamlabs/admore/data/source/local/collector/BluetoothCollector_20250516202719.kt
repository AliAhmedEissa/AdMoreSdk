// File: com.seamlabs.admore/data/source/local/collector/BluetoothCollector.kt
package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.seamlabs.admore.domain.model.Permission
import javax.inject.Inject

/**
 * Collector for Bluetooth data.
 */
class BluetoothCollector @Inject constructor(
    context: Context
) : PermissionRequiredCollector(context, Permission.BLUETOOTH) {

    override fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override suspend fun collect(): Map<String, Any> {
        if (!isPermissionGranted()) {
            return emptyMap()
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        return mapOf(
            "bluetooth_enabled" to (bluetoothAdapter?.isEnabled == true),
            "bluetooth_name" to (bluetoothAdapter?.name ?: "unknown"),
            "bluetooth_address" to getBluetoothAddress(bluetoothAdapter),
            "bluetooth_devices" to getConnectedDevices(bluetoothAdapter)
        )
    }

    private fun getBluetoothAddress(bluetoothAdapter: BluetoothAdapter?): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                "redacted_in_android_12_plus"
            } else {
                @Suppress("DEPRECATION")
                bluetoothAdapter?.address ?: "unknown"
            }
        } catch (e: SecurityException) {
            "permission_error"
        }
    }

    private fun getConnectedDevices(bluetoothAdapter: BluetoothAdapter?): List<Map<String, String>> {
        if (bluetoothAdapter == null) return emptyList()

        val devices = mutableListOf<Map<String, String>>()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                @Suppress("MissingPermission") // We already checked permissions
                bluetoothAdapter.bondedDevices?.forEach { device ->
                    devices.add(
                        mapOf(
                            "name" to (device.name ?: "unknown"),
                            "address" to device.address,
                            "type" to device.type.toString()
                        )
                    )
                }
            } else {
                @Suppress("MissingPermission", "DEPRECATION") // We already checked permissions
                bluetoothAdapter.bondedDevices?.forEach { device ->
                    devices.add(
                        mapOf(
                            "name" to (device.name ?: "unknown"),
                            "address" to device.address,
                            "type" to device.type.toString()
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Handle permission error
        }

        return devices
    }
}
