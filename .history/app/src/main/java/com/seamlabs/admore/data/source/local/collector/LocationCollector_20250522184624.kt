package com.seamlabs.admore.data.source.local.collector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.seamlabs.admore.data.source.local.model.LocationKeys
import com.seamlabs.admore.domain.model.Permission
import javax.inject.Inject

/**
 * Collector for device location data.
 * This collector handles location information gathering with the following features:
 * 1. Last known location from GPS and Network providers
 * 2. Location accuracy and timestamp
 * 3. Location provider status
 * 
 * Note: Requires location permissions (ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION)
 * and location services to be enabled on the device.
 */
class LocationCollector @Inject constructor(
    context: Context,
    private val timeManager: CollectorTimeManager
) : PermissionRequiredCollector(
    context,
    setOf(Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_COARSE_LOCATION)
) {

    private var locationManager: LocationManager? = null

    /**
     * Checks if location permissions are granted.
     * Either ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION is sufficient.
     * @return true if at least one location permission is granted
     */
    override fun isPermissionGranted(): Boolean {
        return hasLocationPermission()
    }

    /**
     * Main collection method that gathers location data.
     * Collects last known location from both GPS and Network providers
     * if available and if enough time has passed since last collection.
     * @return Map containing location data
     */
    override suspend fun collect(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        
        try {
            if (!hasLocationPermission() || !timeManager.shouldCollectLocation()) {
                return data
            }

            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Get last known location from GPS provider
            val gpsLocation = getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (gpsLocation != null) {
                data.putAll(getLocationData(gpsLocation, LocationManager.GPS_PROVIDER))
            }

            // Get last known location from Network provider
            val networkLocation = getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (networkLocation != null) {
                data.putAll(getLocationData(networkLocation, LocationManager.NETWORK_PROVIDER))
            }

            // Update collection time if we got any location data
            if (data.isNotEmpty()) {
                timeManager.updateLocationCTime()
            }
            
        } catch (e: SecurityException) {
            // Handle permission issues
        } catch (e: Exception) {
            // Handle other errors
        }

        return data
    }

    /**
     * Checks if any location permission is granted.
     * @return true if either ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION is granted
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
     * Gets the last known location from a specific provider.
     * @param provider The location provider (GPS or Network)
     * @return Last known Location object or null if not available
     */
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(provider: String): Location? {
        return try {
            locationManager?.getLastKnownLocation(provider)
        } catch (e: SecurityException) {
            null
        }
    }

    /**
     * Extracts location data from a Location object.
     * @param location The Location object to extract data from
     * @param provider The location provider name
     * @return Map containing location data with appropriate keys
     */
    private fun getLocationData(location: Location, provider: String): Map<String, Any> {
        return mapOf(
            LocationKeys.PROVIDER.toKey() to provider,
            LocationKeys.LATITUDE.toKey() to location.latitude,
            LocationKeys.LONGITUDE.toKey() to location.longitude,
            LocationKeys.ACCURACY.toKey() to location.accuracy,
            LocationKeys.LOCATION_TIME.toKey() to location.time,
            LocationKeys.SPEED.toKey() to location.speed,
            LocationKeys.BEARING.toKey() to location.bearing,
            LocationKeys.ALTITUDE.toKey() to location.altitude
        )
    }
}
