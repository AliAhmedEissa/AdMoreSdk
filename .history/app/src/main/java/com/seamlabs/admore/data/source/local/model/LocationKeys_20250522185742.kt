package com.seamlabs.admore.data.source.local.model

enum class LocationKeys(val key: String) {
    LATITUDE("lat"), LONGITUDE("lng"), ACCURACY("accuracy"), PROVIDER("provider"), SPEED("speed"), BEARING(
        "bearing"
    ),
    ALTITUDE("altitude"), LOCATION_TIME("location_time");

    fun toKey(): String = key
} 