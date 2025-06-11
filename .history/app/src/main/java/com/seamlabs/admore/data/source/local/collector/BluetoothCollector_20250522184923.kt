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
 * Collector for device Bluetooth data.
 * This collector handles Bluetooth information gathering with the following features:
 * 1. Bluetooth adapter state and capabilities
 * 2. Paired devices information
 * 3. Available devices scanning
 * 4. Bluetooth connection status
 * 
 * Note: Requires BLUETOOTH and BLUETOOTH_ADMIN permissions.
 * For Android 12+ (API 31+), also requires BLUETOOTH_SCAN and BLUETOOTH_CONNECT.
 * The collector respects user privacy by only collecting necessary information.
 */
class BluetoothCollector @Inject constructor(
    context: Context,
    private val timeManager: CollectorTimeManager
) : PermissionRequiredCollector(
    context,
    setOf(
        Permission.BLUETOOTH,
        Permission.BLUETOOTH_ADMIN,
        Permission.BLUETOOTH_SCAN,
        Permission.BLUETOOTH_CONNECT
    )
) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    private var isScanning = false

    /**
     * Checks if required Bluetooth permissions are granted.
     * @return true if all required permissions are granted
     */
    override fun isPermissionGranted(): Boolean {
        return hasBluetoothPermissions()
    }

    /**
     * Main collection method that gathers Bluetooth data.
     * Collects Bluetooth information if enough time has passed since last collection.
     * @return Map containing Bluetooth data
     */
    override suspend fun collect(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        
        try {
            if (!hasBluetoothPermissions() || !timeManager.shouldCollectBluetooth()) {
                return data
            }

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                return data
            }

            // Get Bluetooth adapter state
            data[BluetoothKeys.ADAPTER_STATE.toKey()] = getAdapterState()

            // Get paired devices
            val pairedDevices = getPairedDevices()
            data[BluetoothKeys.PAIRED_DEVICES.toKey()] = pairedDevices

            // Get available devices
            val availableDevices = getAvailableDevices()
            data[BluetoothKeys.AVAILABLE_DEVICES.toKey()] = availableDevices

            // Update collection time if we got any data
            if (data.isNotEmpty()) {
                timeManager.updateBluetoothCTime()
            }
            
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: Exception) {
            // Handle other errors
        }

        return data
    }

    /**
     * Checks if required Bluetooth permissions are granted.
     * @return true if all required permissions are granted
     */
    private fun hasBluetoothPermissions(): Boolean {
        val hasBasicPermissions = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADMIN
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return hasBasicPermissions &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }

        return hasBasicPermissions
    }

    /**
     * Gets the current state of the Bluetooth adapter.
     * @return Map containing adapter state information
     */
    private fun getAdapterState(): Map<String, Any> {
        return mapOf(
            BluetoothKeys.IS_ENABLED.toKey() to (bluetoothAdapter?.isEnabled == true),
            BluetoothKeys.NAME.toKey() to (bluetoothAdapter?.name ?: "unknown"),
            BluetoothKeys.ADDRESS.toKey() to (bluetoothAdapter?.address ?: "unknown"),
            BluetoothKeys.SCAN_MODE.toKey() to getScanMode(),
            BluetoothKeys.BONDED_DEVICE_COUNT.toKey() to (bluetoothAdapter?.bondedDevices?.size ?: 0)
        )
    }

    /**
     * Gets the current scan mode of the Bluetooth adapter.
     * @return String representing the scan mode
     */
    private fun getScanMode(): String {
        return when (bluetoothAdapter?.scanMode) {
            BluetoothAdapter.SCAN_MODE_NONE -> "none"
            BluetoothAdapter.SCAN_MODE_CONNECTABLE -> "connectable"
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> "discoverable"
            else -> "unknown"
        }
    }

    /**
     * Gets information about paired Bluetooth devices.
     * @return List of paired device information maps
     */
    @SuppressLint("MissingPermission")
    private fun getPairedDevices(): List<Map<String, Any>> {
        val devices = mutableListOf<Map<String, Any>>()
        val pairedDevices = bluetoothAdapter?.bondedDevices ?: return devices

        for (device in pairedDevices) {
            val deviceInfo = mapOf(
                BluetoothKeys.DEVICE_NAME.toKey() to (device.name ?: "unknown"),
                BluetoothKeys.DEVICE_ADDRESS.toKey() to (device.address ?: "unknown"),
                BluetoothKeys.DEVICE_TYPE.toKey() to getDeviceType(device),
                BluetoothKeys.BOND_STATE.toKey() to getBondState(device),
                BluetoothKeys.DEVICE_CLASS.toKey() to device.bluetoothClass.deviceClass
            )
            devices.add(deviceInfo)
        }

        return devices
    }

    /**
     * Gets information about available Bluetooth devices.
     * @return List of available device information maps
     */
    @SuppressLint("MissingPermission")
    private fun getAvailableDevices(): List<Map<String, Any>> {
        val devices = mutableListOf<Map<String, Any>>()
        val availableDevices = bluetoothAdapter?.bondedDevices ?: return devices

        for (device in availableDevices) {
            val deviceInfo = mapOf(
                BluetoothKeys.DEVICE_NAME.toKey() to (device.name ?: "unknown"),
                BluetoothKeys.DEVICE_ADDRESS.toKey() to (device.address ?: "unknown"),
                BluetoothKeys.DEVICE_TYPE.toKey() to getDeviceType(device),
                BluetoothKeys.RSSI.toKey() to device.rssi,
                BluetoothKeys.DEVICE_CLASS.toKey() to device.bluetoothClass.deviceClass
            )
            devices.add(deviceInfo)
        }

        return devices
    }

    /**
     * Gets the type of a Bluetooth device.
     * @param device The Bluetooth device
     * @return String representing the device type
     */
    private fun getDeviceType(device: BluetoothDevice): String {
        return when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "classic"
            BluetoothDevice.DEVICE_TYPE_LE -> "le"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "dual"
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "unknown"
            else -> "unknown"
        }
    }

    /**
     * Gets the bond state of a Bluetooth device.
     * @param device The Bluetooth device
     * @return String representing the bond state
     */
    private fun getBondState(device: BluetoothDevice): String {
        return when (device.bondState) {
            BluetoothDevice.BOND_NONE -> "none"
            BluetoothDevice.BOND_BONDING -> "bonding"
            BluetoothDevice.BOND_BONDED -> "bonded"
            else -> "unknown"
        }
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
