package com.seamlabs.admore.sdk.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.seamlabs.admore.sdk.data.source.local.model.NetworkKeys
import com.seamlabs.admore.sdk.domain.model.Permission

/**
 * Collector for network information.
 * Handles partial permission scenarios gracefully.
 */
class NetworkInfoCollector(
    context: Context
) : PermissionRequiredCollector(
    context, setOf(Permission.PHONE_STATE, Permission.NETWORK_STATE)
) {

    override fun isPermissionGranted(): Boolean {
        // Check if we have at least one of the required permissions
        return hasPhoneStatePermission() || hasNetworkStatePermission()
    }

    /**
     * Check if PHONE_STATE permission is granted
     */
    private fun hasPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if NETWORK_STATE permission is granted
     */
    private fun hasNetworkStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun collect(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        // Add permission status for debugging/analytics
        data[NetworkKeys.PHONE_STATE_PERMISSION.toKey()] = hasPhoneStatePermission()
        data[NetworkKeys.NETWORK_STATE_PERMISSION.toKey()] = hasNetworkStatePermission()

        // Collect network state info if we have ACCESS_NETWORK_STATE permission
        if (hasNetworkStatePermission()) {
            data.putAll(collectNetworkStateInfo())
        } else {
            // Add fallback values when permission is not granted
            data.putAll(getNetworkStatePermissionDeniedData())
        }

        // Collect cellular info if we have READ_PHONE_STATE permission
        if (hasPhoneStatePermission()) {
            data.putAll(collectCellularInfo())
        } else {
            // Add fallback values when permission is not granted
            data.putAll(getCellularPermissionDeniedData())
        }

        return data
    }

    /**
     * Collect network state information (requires ACCESS_NETWORK_STATE permission)
     */
    private fun collectNetworkStateInfo(): Map<String, Any> {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val data = mutableMapOf<String, Any>()

            // Basic connection info
            data[NetworkKeys.CONNECTION_TYPE.toKey()] = getConnectionType(connectivityManager)
            data[NetworkKeys.IS_CONNECTED.toKey()] = isNetworkAvailable(connectivityManager)

            // Get active network and its capabilities
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                data.putAll(getNetworkCapabilities(connectivityManager, activeNetwork))
                data.putAll(getNetworkInfo(connectivityManager, activeNetwork))
            } else {
                data.putAll(getNoActiveNetworkData())
            }

            data
        } catch (e: SecurityException) {
            // This shouldn't happen if we checked permissions correctly, but handle gracefully
            getNetworkStatePermissionDeniedData()
        }
    }

    /**
     * Collect cellular information (requires READ_PHONE_STATE permission)
     */
    private fun collectCellularInfo(): Map<String, Any> {
        return try {
            getCellularNetworkInfo()
        } catch (e: SecurityException) {
            getCellularPermissionDeniedData()
        }
    }

    /**
     * Fallback data when ACCESS_NETWORK_STATE permission is denied
     */
    private fun getNetworkStatePermissionDeniedData(): Map<String, Any> {
        return mapOf(
            NetworkKeys.CONNECTION_TYPE.toKey() to "permission_denied",
            NetworkKeys.IS_CONNECTED.toKey() to false,
            NetworkKeys.HAS_INTERNET.toKey() to false,
            NetworkKeys.HAS_VALIDATED.toKey() to false,
            NetworkKeys.IS_ACTIVE.toKey() to false,
            NetworkKeys.IS_AVAILABLE.toKey() to false
        )
    }

    /**
     * Fallback data when READ_PHONE_STATE permission is denied
     */
    private fun getCellularPermissionDeniedData(): Map<String, Any> {
        return mapOf(
            NetworkKeys.SUBTYPE.toKey() to -1,
            NetworkKeys.SUBTYPE_NAME.toKey() to "permission_denied",
            NetworkKeys.NETWORK_ID.toKey() to "permission_denied"
        )
    }

    /**
     * Data when no active network is available
     */
    private fun getNoActiveNetworkData(): Map<String, Any> {
        return mapOf(
            NetworkKeys.HAS_INTERNET.toKey() to false,
            NetworkKeys.HAS_VALIDATED.toKey() to false,
            NetworkKeys.IS_ACTIVE.toKey() to false,
            NetworkKeys.IS_AVAILABLE.toKey() to false,
            NetworkKeys.DOWNLOAD_SPEED.toKey() to 0,
            NetworkKeys.UPLOAD_SPEED.toKey() to 0,
            NetworkKeys.SIGNAL_STRENGTH.toKey() to 0
        )
    }

    private fun getConnectionType(connectivityManager: ConnectivityManager): String {
        return try {
            val network = connectivityManager.activeNetwork ?: return "none"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "none"

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "bluetooth"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) -> "wifi_aware"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> "lowpan"
                else -> "unknown"
            }
        } catch (e: SecurityException) {
            "permission_denied"
        }
    }

    private fun isNetworkAvailable(connectivityManager: ConnectivityManager): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: SecurityException) {
            false
        }
    }

    private fun getNetworkCapabilities(
        connectivityManager: ConnectivityManager,
        network: Network
    ): Map<String, Any> {
        return try {
            val capabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return emptyMap()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mapOf(
                    NetworkKeys.HAS_INTERNET.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    ),
                    NetworkKeys.HAS_VALIDATED.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    ),
                    NetworkKeys.HAS_CAPTIVE_PORTAL.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL
                    ),
                    NetworkKeys.HAS_NOT_RESTRICTED.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED
                    ),
                    NetworkKeys.HAS_NOT_ROAMING.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING
                    ),
                    NetworkKeys.HAS_NOT_METERED.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                    ),
                    NetworkKeys.HAS_NOT_SUSPENDED.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED
                    ),
                    NetworkKeys.HAS_NOT_VPN.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_VPN
                    ),
                    NetworkKeys.DOWNLOAD_SPEED.toKey() to capabilities.linkDownstreamBandwidthKbps,
                    NetworkKeys.UPLOAD_SPEED.toKey() to capabilities.linkUpstreamBandwidthKbps,
                    NetworkKeys.SIGNAL_STRENGTH.toKey() to capabilities.signalStrength
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mapOf(
                    NetworkKeys.HAS_INTERNET.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    ),
                    NetworkKeys.HAS_VALIDATED.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    ),
                    NetworkKeys.HAS_CAPTIVE_PORTAL.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL
                    ),
                    NetworkKeys.HAS_NOT_RESTRICTED.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED
                    ),
                    NetworkKeys.HAS_NOT_ROAMING.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING
                    ),
                    NetworkKeys.DOWNLOAD_SPEED.toKey() to 0,
                    NetworkKeys.UPLOAD_SPEED.toKey() to 0,
                    NetworkKeys.SIGNAL_STRENGTH.toKey() to 0
                )
            } else {
                mapOf(
                    NetworkKeys.HAS_INTERNET.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    ),
                    NetworkKeys.HAS_VALIDATED.toKey() to capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    ),
                    NetworkKeys.DOWNLOAD_SPEED.toKey() to 0,
                    NetworkKeys.UPLOAD_SPEED.toKey() to 0,
                    NetworkKeys.SIGNAL_STRENGTH.toKey() to 0
                )
            }
        } catch (e: SecurityException) {
            getNetworkCapabilitiesPermissionDeniedData()
        }
    }

    private fun getNetworkCapabilitiesPermissionDeniedData(): Map<String, Any> {
        return mapOf(
            NetworkKeys.HAS_INTERNET.toKey() to false,
            NetworkKeys.HAS_VALIDATED.toKey() to false,
            NetworkKeys.HAS_CAPTIVE_PORTAL.toKey() to false,
            NetworkKeys.HAS_NOT_RESTRICTED.toKey() to false,
            NetworkKeys.HAS_NOT_ROAMING.toKey() to false,
            NetworkKeys.HAS_NOT_METERED.toKey() to false,
            NetworkKeys.HAS_NOT_SUSPENDED.toKey() to false,
            NetworkKeys.HAS_NOT_VPN.toKey() to false,
            NetworkKeys.DOWNLOAD_SPEED.toKey() to -1,
            NetworkKeys.UPLOAD_SPEED.toKey() to -1,
            NetworkKeys.SIGNAL_STRENGTH.toKey() to -1
        )
    }

    private fun getNetworkInfo(
        connectivityManager: ConnectivityManager,
        network: Network
    ): Map<String, Any> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkInfo = connectivityManager.getNetworkInfo(network) ?: return emptyMap()

                mapOf(
                    NetworkKeys.IS_ACTIVE.toKey() to networkInfo.isConnected,
                    NetworkKeys.IS_AVAILABLE.toKey() to networkInfo.isAvailable,
                    NetworkKeys.IS_FAILOVER.toKey() to networkInfo.isFailover,
                    NetworkKeys.IS_ROAMING.toKey() to networkInfo.isRoaming,
                    NetworkKeys.REASON.toKey() to (networkInfo.reason ?: "unknown"),
                    NetworkKeys.EXTRA_INFO.toKey() to (networkInfo.extraInfo ?: "unknown")
                )
            } else {
                emptyMap()
            }
        } catch (e: SecurityException) {
            mapOf(
                NetworkKeys.IS_ACTIVE.toKey() to false,
                NetworkKeys.IS_AVAILABLE.toKey() to false,
                NetworkKeys.IS_FAILOVER.toKey() to false,
                NetworkKeys.IS_ROAMING.toKey() to false,
                NetworkKeys.REASON.toKey() to "permission_denied",
                NetworkKeys.EXTRA_INFO.toKey() to "permission_denied"
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCellularNetworkInfo(): Map<String, Any> {
        return try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            mapOf(
                NetworkKeys.SUBTYPE.toKey() to telephonyManager.networkType,
                NetworkKeys.SUBTYPE_NAME.toKey() to getNetworkTypeName(telephonyManager.networkType),
                NetworkKeys.NETWORK_ID.toKey() to (telephonyManager.networkOperator ?: "unknown")
            )
        } catch (e: SecurityException) {
            getCellularPermissionDeniedData()
        }
    }

    private fun getNetworkTypeName(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO rev. 0"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO rev. A"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO rev. B"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD"
            TelephonyManager.NETWORK_TYPE_IDEN -> "iDEN"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA"
            TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
            else -> "unknown"
        }
    }
}