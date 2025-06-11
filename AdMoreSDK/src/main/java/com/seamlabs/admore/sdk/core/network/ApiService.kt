package com.seamlabs.admore.sdk.core.network

import com.seamlabs.admore.sdk.data.model.ApiResponse
import com.seamlabs.admore.sdk.data.model.EventRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interface for API service.
 */
interface ApiService {
    /**
     * Sends an event to the API.
     * @param eventRequest The event request
     * @return ApiResponse with success/failure information
     */
    @POST("api/datalake/records/encrypted")
    suspend fun sendEvent(@Body eventRequest: EventRequest): ApiResponse<Unit>
}