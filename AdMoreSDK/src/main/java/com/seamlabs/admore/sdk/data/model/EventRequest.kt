package com.seamlabs.admore.sdk.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing an event request to the API.
 */
data class EventRequest(
    @SerializedName("encryptedData")
    val data: String,
)