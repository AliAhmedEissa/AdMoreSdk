// File: com.seamlabs.admore/data/source/local/collector/BluetoothCollector.kt
package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.seamlabs.admore.data.source.local.model.BluetoothKeys
import com.seamlabs.admore.domain.model.Permission
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Collector for Bluetooth data.
 * This collector handles three main functionalities:
 * 1. Basic Bluetooth state (requires BLUETOOTH permission)
 * 2. Bonded devices (requires BLUETOOTH_CONNECT permission)
 * 3. Nearby devices scanning (requires BLUETOOTH_SCAN permission)
 * 
 * Note: On Android 12 (API 31) and above, the permissions are split into:
 * - BLUETOOTH_CONNECT: For connecting to devices and accessing device info
 * - BLUETOOTH_SCAN: For scanning nearby devices
 * Below Android 12, only BLUETOOTH permission is required.
 */
class BluetoothCollector @Inject constructor(
    context: Context,
    private val timeManager: CollectorTimeManager
) : PermissionRequiredCollector(
    context,
    setOf(Permission.BLUETOOTH, Permission.BLUETOOTH_ADMIN, Permission.BLUETOOTH_SCAN, Permission.BLUETOOTH_CONNECT)
) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    private var isScanning = false

    /**
     * Checks if any of the required permissions are granted.
     * The collector can work with partial permissions, but each feature
     * will check for its specific required permissions.
     * @return true if at least one permission is granted
     */
    override fun isPermissionGranted(): Boolean {
        return hasBluetoothPermission() || hasBluetoothConnectPermission() || hasBluetoothScanPermission()
    }

    /**
     * Main collection method that gathers Bluetooth data based on available permissions.
     * Features are collected in order of least to most permission requirements:
     * 1. Basic state (BLUETOOTH)
     * 2. Bonded devices (BLUETOOTH_CONNECT)
     * 3. Nearby devices (BLUETOOTH_SCAN)
     * @return Map containing collected Bluetooth data
     */
    override suspend fun collect(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter == null) {
                return mapOf(BluetoothKeys.BLUETOOTH_ENABLED.toKey() to false)
            }

            // Basic Bluetooth state - only needs BLUETOOTH permission
            if (hasBluetoothPermission()) {
                data[BluetoothKeys.BLUETOOTH_ENABLED.toKey()] = bluetoothAdapter?.isEnabled == true
                data[BluetoothKeys.BLUETOOTH_ADDRESS.toKey()] = getBluetoothAddress(bluetoothAdapter)
            }
            
            // Get bonded devices - needs BLUETOOTH_CONNECT permission
            if (hasBluetoothConnectPermission()) {
                data[BluetoothKeys.BLUETOOTH_DEVICES.toKey()] = getBondedDevices(bluetoothAdapter)
            }
            
            // Scan for nearby devices - needs BLUETOOTH_SCAN permission
            if (hasBluetoothScanPermission() && timeManager.shouldCollectBh()) {
                val scanResults = withTimeoutOrNull(5000) { // 5 second timeout
                    scanForNearbyDevices()
                } ?: emptyList()
                data[BluetoothKeys.NEARBY_DEVICES.toKey()] = scanResults
                
                // Only update collection time if we got scan results
                if (scanResults.isNotEmpty()) {
                    timeManager.updateBhCTime()
                }
            }
            
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: Exception) {
            // Handle other errors
        }

        return data
    }

    /**
     * Checks if basic Bluetooth permission is granted.
     * On Android 12+: BLUETOOTH_CONNECT
     * Below Android 12: BLUETOOTH
     * @return true if permission is granted
     */
    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if Bluetooth connect permission is granted.
     * On Android 12+: BLUETOOTH_CONNECT
     * Below Android 12: BLUETOOTH
     * Required for accessing bonded devices.
     * @return true if permission is granted
     */
    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if Bluetooth scan permission is granted.
     * On Android 12+: BLUETOOTH_SCAN
     * Below Android 12: BLUETOOTH
     * Required for scanning nearby devices.
     * @return true if permission is granted
     */
    private fun hasBluetoothScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Gets the Bluetooth address of the adapter.
     * Returns "redacted_in_android_12_plus" for Android 12 and above
     * due to privacy restrictions.
     * @param bluetoothAdapter The Bluetooth adapter
     * @return The Bluetooth address string
     */
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

    /**
     * Gets the list of bonded (paired) Bluetooth devices.
     * Includes device name, address, type, and bond state.
     * @param bluetoothAdapter The Bluetooth adapter
     * @return List of bonded devices with their details
     */
    private fun getBondedDevices(bluetoothAdapter: BluetoothAdapter?): List<Map<String, String>> {
        if (bluetoothAdapter == null) return emptyList()

        val devices = mutableListOf<Map<String, String>>()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                @Suppress("MissingPermission") // We already checked permissions
                bluetoothAdapter.bondedDevices?.forEach { device ->
                    devices.add(
                        mapOf(
                            BluetoothKeys.DEVICE_NAME.toKey() to (device.name ?: "unknown"),
                            BluetoothKeys.DEVICE_ADDRESS.toKey() to device.address,
                            BluetoothKeys.DEVICE_TYPE.toKey() to device.type.toString(),
                            BluetoothKeys.DEVICE_BOND_STATE.toKey() to device.bondState.toString()
                        )
                    )
                }
            } else {
                @Suppress("MissingPermission", "DEPRECATION") // We already checked permissions
                bluetoothAdapter.bondedDevices?.forEach { device ->
                    devices.add(
                        mapOf(
                            BluetoothKeys.DEVICE_NAME.toKey() to (device.name ?: "unknown"),
                            BluetoothKeys.DEVICE_ADDRESS.toKey() to device.address,
                            BluetoothKeys.DEVICE_TYPE.toKey() to device.type.toString(),
                            BluetoothKeys.DEVICE_BOND_STATE.toKey() to device.bondState.toString()
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Handle permission error
        }

        return devices
    }

    /**
     * Scans for nearby Bluetooth devices.
     * Uses Bluetooth LE Scanner for Android 5.0 and above.
     * Collects device name, address, type, and UUIDs.
     * @return List of nearby devices with their details
     */
    @SuppressLint("MissingPermission")
    private suspend fun scanForNearbyDevices(): List<Map<String, Any>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return emptyList()
        }

        discoveredDevices.clear()
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        return suspendCancellableCoroutine { continuation ->
            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)
                    result.device?.let { device ->
                        discoveredDevices.add(device)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    super.onScanFailed(errorCode)
                    if (!continuation.isCompleted) {
                        continuation.resume(emptyList())
                    }
                }
            }

            try {
                isScanning = true
                bluetoothLeScanner?.startScan(scanCallback)
            } catch (e: SecurityException) {
                continuation.resume(emptyList())
            }
        }
    }
}
