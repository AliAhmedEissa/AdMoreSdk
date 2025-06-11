package com.seamlabs.admore.sdk.data.source.local.collector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.seamlabs.admore.sdk.data.source.local.model.LocationKeys
import com.seamlabs.admore.sdk.domain.model.Permission
import javax.inject.Inject

/**
 * Collector for location data.
 */
class LocationCollector @Inject constructor(
    context: Context
) : PermissionRequiredCollector(
    context,
    setOf(Permission.LOCATION_FINE, Permission.LOCATION_COARSE)
) {

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun collect(): Map<String, Any> {
        if (!isPermissionGranted()) {
            return emptyMap()
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = getLastKnownLocation(locationManager)

        return if (location != null) {
            mapOf(
                LocationKeys.LATITUDE.toKey() to location.latitude as Any,
                LocationKeys.LONGITUDE.toKey() to location.longitude as Any,
                LocationKeys.ACCURACY.toKey() to location.accuracy as Any,
                LocationKeys.PROVIDER.toKey() to location.provider as Any,
                LocationKeys.LOCATION_TIME.toKey() to location.time
            )
        } else {
            emptyMap()
        }
    }

    private fun getLastKnownLocation(locationManager: LocationManager): Location? {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null

        for (provider in providers) {
            try {
                @Suppress("MissingPermission") // We already checked permissions
                val location = locationManager.getLastKnownLocation(provider) ?: continue

                if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                    bestLocation = location
                }
            } catch (e: SecurityException) {
                // Handle permission issue
            }
        }

        return bestLocation
    }
}
