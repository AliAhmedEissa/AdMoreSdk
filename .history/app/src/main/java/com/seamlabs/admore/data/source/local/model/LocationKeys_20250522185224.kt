package com.seamlabs.admore.data.source.local.model

enum class LocationKeys(val key: String) {
    LATITUDE("lat"),
    LONGITUDE("lng"),
    ACCURACY("accuracy"),
    PROVIDER("provider"),
    LOCATION_TIME("location_time");

    fun toKey(): String = key
} 