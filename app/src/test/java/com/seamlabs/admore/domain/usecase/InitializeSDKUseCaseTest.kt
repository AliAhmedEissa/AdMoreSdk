package com.seamlabs.admore.domain.usecase

import com.seamlabs.admore.domain.repository.DeviceDataRepository
import com.seamlabs.admore.domain.repository.EventRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class InitializeSDKUseCaseTest {
    private val testDispatcher = TestCoroutineDispatcher()
    
    private val mockEventRepository: EventRepository = mock()
    private val mockDeviceDataRepository: DeviceDataRepository = mock()
    
    private lateinit var useCase: InitializeSDKUseCase
    
    @Before
    fun setup() {
        useCase = InitializeSDKUseCase(mockEventRepository, mockDeviceDataRepository)
    }
    
    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `execute should initialize repositories`() = testDispatcher.runBlockingTest {
        // Arrange
        val uniqueKey = "test-key"
        
        // Act
        useCase.execute(uniqueKey)
        
        // Assert
        verify(mockEventRepository).initialize(uniqueKey)
        verify(mockDeviceDataRepository).initialize()
    }
}