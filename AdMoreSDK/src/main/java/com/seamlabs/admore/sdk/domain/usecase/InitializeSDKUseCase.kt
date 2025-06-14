package com.seamlabs.admore.sdk.domain.usecase

import com.seamlabs.admore.sdk.domain.repository.DeviceDataRepository
import com.seamlabs.admore.sdk.domain.repository.EventRepository

/**
 * Use case for initializing the SDK.
 */
class InitializeSDKUseCase(
    private val eventRepository: EventRepository,
    private val deviceDataRepository: DeviceDataRepository
) {
    /**
     * Executes the use case.
     * @param uniqueKey The unique key for identifying the app
     */
    suspend fun execute(uniqueKey: String) {
        eventRepository.initialize(uniqueKey)
        deviceDataRepository.initialize()
    }
}