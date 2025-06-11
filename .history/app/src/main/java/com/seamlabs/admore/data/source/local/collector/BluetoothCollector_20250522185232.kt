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

    override fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override suspend fun collect(): Map<String, Any> {
        if (!isPermissionGranted() || !timeManager.shouldCollectBh()) {
            return emptyMap()
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            return mapOf(BluetoothKeys.BLUETOOTH_ENABLED.toKey() to false)
        }

        val data = mutableMapOf<String, Any>()
        data[BluetoothKeys.BLUETOOTH_ENABLED.toKey()] = bluetoothAdapter?.isEnabled == true
        data[BluetoothKeys.BLUETOOTH_ADDRESS.toKey()] = getBluetoothAddress(bluetoothAdapter)
        
        // Get bonded devices
        data[BluetoothKeys.BLUETOOTH_DEVICES.toKey()] = getBondedDevices(bluetoothAdapter)
        
        // Scan for nearby devices if we have scan permission
        if (hasScanPermission()) {
            // Wait for scan results with timeout
            val scanResults = withTimeoutOrNull(8000) { // 8 second timeout
                scanForNearbyDevices()
            } ?: emptyList()
            data[BluetoothKeys.NEARBY_DEVICES.toKey()] = scanResults
        }

        // Only update collection time if we got scan results
        if (data.containsKey(BluetoothKeys.NEARBY_DEVICES.toKey())) {
            timeManager.updateBhCTime()
        }
        
        return data
    }

    private fun hasScanPermission(): Boolean {
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
                
                // Scan for 5 seconds
                GlobalScope.launch {
                    delay(5000)
                    if (isScanning) {
                        bluetoothLeScanner?.stopScan(scanCallback)
                        isScanning = false
                        
                        val devices = discoveredDevices.map { device ->
                            mapOf(
                                BluetoothKeys.DEVICE_NAME.toKey() to (device.name ?: "unknown"),
                                BluetoothKeys.DEVICE_ADDRESS.toKey() to device.address,
                                BluetoothKeys.DEVICE_TYPE.toKey() to device.type.toString(),
                                BluetoothKeys.DEVICE_UUIDS.toKey() to (device.uuids ?: 0)
                            )
                        }
                        continuation.resume(devices)
                    }
                }
            } catch (e: SecurityException) {
                continuation.resume(emptyList())
            }
        }
    }
}
