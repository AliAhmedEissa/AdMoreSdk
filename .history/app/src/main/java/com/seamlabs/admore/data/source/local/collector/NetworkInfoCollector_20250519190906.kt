package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.seamlabs.admore.data.source.local.model.NetworkKeys
import com.seamlabs.admore.domain.model.Permission
import javax.inject.Inject

/**
 * Collector for network information.
 */
class NetworkInfoCollector @Inject constructor(
    context: Context
) : PermissionRequiredCollector(
    context,
    setOf(Permission.READ_PHONE_STATE)
) {

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun collect(): Map<String, Any> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val data = mutableMapOf<String, Any>()
        
        // Basic connection info
        data[NetworkKeys.CONNECTION_TYPE.toKey()] = getConnectionType(connectivityManager)
        data[NetworkKeys.IS_CONNECTED.toKey()] = isNetworkAvailable(connectivityManager)
        
        // Get active network and its capabilities
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork != null) {
            data.putAll(getNetworkCapabilities(connectivityManager, activeNetwork))
            data.putAll(getNetworkInfo(connectivityManager, activeNetwork))
        }
        
        // Get cellular network details if available and permission granted
        if (data[NetworkKeys.CONNECTION_TYPE.toKey()] == "cellular" && isPermissionGranted()) {
            data.putAll(getCellularNetworkInfo())
        }

        return data
    }

    private fun getConnectionType(connectivityManager: ConnectivityManager): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "none"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "none"
            
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "bluetooth"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) -> "wifi_aware"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> "lowpan"
                else -> "unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            
            @Suppress("DEPRECATION")
            return when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "wifi"
                ConnectivityManager.TYPE_MOBILE -> "cellular"
                ConnectivityManager.TYPE_ETHERNET -> "ethernet"
                ConnectivityManager.TYPE_BLUETOOTH -> "bluetooth"
                ConnectivityManager.TYPE_VPN -> "vpn"
                else -> "unknown"
            }
        }
    }

    private fun isNetworkAvailable(connectivityManager: ConnectivityManager): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private fun getNetworkCapabilities(connectivityManager: ConnectivityManager, network: Network): Map<String, Any> {
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return emptyMap()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mapOf(
                NetworkKeys.HAS_INTERNET.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                NetworkKeys.HAS_VALIDATED.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                NetworkKeys.HAS_CAPTIVE_PORTAL.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL),
                NetworkKeys.HAS_NOT_RESTRICTED.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED),
                NetworkKeys.HAS_NOT_ROAMING.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING),
                NetworkKeys.HAS_NOT_METERED.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                NetworkKeys.HAS_NOT_SUSPENDED.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED),
                NetworkKeys.HAS_NOT_VPN.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN),
                NetworkKeys.DOWNLOAD_SPEED.toKey() to capabilities.getLinkDownstreamBandwidthKbps(),
                NetworkKeys.UPLOAD_SPEED.toKey() to capabilities.getLinkUpstreamBandwidthKbps(),
                NetworkKeys.SIGNAL_STRENGTH.toKey() to capabilities.getSignalStrength()
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mapOf(
                    NetworkKeys.HAS_INTERNET.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                    NetworkKeys.HAS_VALIDATED.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                    NetworkKeys.HAS_CAPTIVE_PORTAL.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL),
                    NetworkKeys.HAS_NOT_RESTRICTED.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED),
                    NetworkKeys.HAS_NOT_ROAMING.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                )
            } else {
                mapOf(
                    NetworkKeys.HAS_INTERNET.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                    NetworkKeys.HAS_VALIDATED.toKey() to capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )
            }
        }
    }

    private fun getNetworkInfo(connectivityManager: ConnectivityManager, network: Network): Map<String, Any> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkInfo = connectivityManager.getNetworkInfo(network) ?: return emptyMap()
            
            return mapOf(
                NetworkKeys.IS_ACTIVE.toKey() to networkInfo.isConnected,
                NetworkKeys.IS_AVAILABLE.toKey() to networkInfo.isAvailable,
                NetworkKeys.IS_FAILOVER.toKey() to networkInfo.isFailover,
                NetworkKeys.IS_ROAMING.toKey() to networkInfo.isRoaming,
                NetworkKeys.REASON.toKey() to (networkInfo.reason ?: "unknown"),
                NetworkKeys.EXTRA_INFO.toKey() to (networkInfo.extraInfo ?: "unknown")
            )
        }
        return emptyMap()
    }

    @SuppressLint("MissingPermission")
    private fun getCellularNetworkInfo(): Map<String, Any> {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            mapOf(
                NetworkKeys.SUBTYPE.toKey() to telephonyManager.networkType,
                NetworkKeys.SUBTYPE_NAME.toKey() to getNetworkTypeName(telephonyManager.networkType),
                NetworkKeys.NETWORK_ID.toKey() to (telephonyManager.networkOperator ?: "unknown")
            )
        } catch (e: SecurityException) {
            mapOf(
                NetworkKeys.SUBTYPE.toKey() to -1,
                NetworkKeys.SUBTYPE_NAME.toKey() to "permission_denied",
                NetworkKeys.NETWORK_ID.toKey() to "permission_denied"
            )
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