// File: com.seamlabs.admore/domain/usecase/InitializeSDKUseCase.kt
package com.seamlabs.admore.domain.usecase

import com.seamlabs.admore.domain.repository.DeviceDataRepository
import com.seamlabs.admore.domain.repository.EventRepository
import javax.inject.Inject

/**
 * Use case for initializing the SDK.
 */
class InitializeSDKUseCase @Inject constructor(
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