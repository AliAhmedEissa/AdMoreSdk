// File: com.seamlabs.admore/data/model/EventRequest.kt
package com.seamlabs.admore.sdk.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing an event request to the API.
 */
data class EventRequest(
    @SerializedName("encryptedData")
    val data: String, // Encrypted data
)