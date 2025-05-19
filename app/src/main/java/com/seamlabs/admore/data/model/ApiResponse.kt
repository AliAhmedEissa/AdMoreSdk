// File: com.seamlabs.admore/data/model/ApiResponse.kt
package com.seamlabs.admore.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing an API response.
 */
data class ApiResponse<T>(
    @SerializedName("status")
    val status: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: T?
) {
    val isSuccessful: Boolean
        get() = status == "success"
}