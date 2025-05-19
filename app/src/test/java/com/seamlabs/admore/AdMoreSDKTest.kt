package com.seamlabs.admore

import android.content.Context
import com.seamlabs.admore.core.logger.Logger
import com.seamlabs.admore.domain.usecase.CollectDeviceDataUseCase
import com.seamlabs.admore.domain.usecase.InitializeSDKUseCase
import com.seamlabs.admore.domain.usecase.SendEventUseCase
import com.seamlabs.admore.presentation.callback.EventCallback
import com.seamlabs.admore.presentation.callback.InitCallback
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.lang.Exception

@ExperimentalCoroutinesApi
class AdMoreSDKTest {
    private val testDispatcher = TestCoroutineDispatcher()
    
    private val mockContext: Context = mock()
    private val mockInitializeUseCase: InitializeSDKUseCase = mock()
    private val mockSendEventUseCase: SendEventUseCase = mock()
    private val mockCollectDeviceDataUseCase: CollectDeviceDataUseCase = mock()
    private val mockLogger: Logger = mock()
    private val mockInitCallback: InitCallback = mock()
    private val mockEventCallback: EventCallback = mock()
    
    private lateinit var sdk: AdMoreSDK
    
    @Before
    fun setup() {
        // Create SDK instance with mocked dependencies
        sdk = AdMoreSDK(
            mockContext,
            mockInitializeUseCase,
            mockSendEventUseCase,
            mockCollectDeviceDataUseCase,
            mockLogger
        )
    }
    
    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `initialize should call InitializeSDKUseCase`() = testDispatcher.runBlockingTest {
        // Arrange
        val uniqueKey = "test-key"
        
        // Act
        sdk.initialize(uniqueKey, mockInitCallback)
        
        // Assert
        verify(mockInitializeUseCase).execute(uniqueKey)
    }
    
    @Test
    fun `initialize should call CollectDeviceDataUseCase`() = testDispatcher.runBlockingTest {
        // Arrange
        val uniqueKey = "test-key"
        val deviceData = mapOf("device_key" to "value")
        whenever(mockCollectDeviceDataUseCase.execute()).thenReturn(deviceData)
        
        // Act
        sdk.initialize(uniqueKey, mockInitCallback)
        
        // Assert
        verify(mockCollectDeviceDataUseCase).execute()
    }
    
    @Test
    fun `initialize should send initial event`() = testDispatcher.runBlockingTest {
        // Arrange
        val uniqueKey = "test-key"
        val deviceData = mapOf("device_key" to "value")
        whenever(mockCollectDeviceDataUseCase.execute()).thenReturn(deviceData)
        
        // Act
        sdk.initialize(uniqueKey, mockInitCallback)
        
        // Assert
        verify(mockSendEventUseCase).execute("sdk_initialized", deviceData, uniqueKey)
    }
    
    @Test
    fun `initialize should call callback onSuccess when successful`() = testDispatcher.runBlockingTest {
        // Arrange
        val uniqueKey = "test-key"
        val deviceData = mapOf("device_key" to "value")
        whenever(mockCollectDeviceDataUseCase.execute()).thenReturn(deviceData)
        
        // Act
        sdk.initialize(uniqueKey, mockInitCallback)
        
        // Assert
        verify(mockInitCallback).onSuccess()
    }
    
    @Test
    fun `initialize should call callback onError when exception occurs`() = testDispatcher.runBlockingTest {
        // Arrange
        val uniqueKey = "test-key"
        val testException = Exception("Test error")
        whenever(mockInitializeUseCase.execute(any())).thenThrow(testException)
        
        // Act
        sdk.initialize(uniqueKey, mockInitCallback)
        
        // Assert
        verify(mockInitCallback).onError(testException)
    }
    
    @Test
    fun `sendEvent should call SendEventUseCase`() = testDispatcher.runBlockingTest {
        // Arrange
        val uniqueKey = "test-key"
        val eventName = "test_event"
        val eventData = mapOf("key" to "value")
        
        // Initialize SDK first
        whenever(mockCollectDeviceDataUseCase.execute()).thenReturn(emptyMap())
        sdk.initialize(uniqueKey, null)
        
        // Act
        sdk.sendEvent(eventName, eventData, mockEventCallback)
        
        // Assert
        verify(mockSendEventUseCase).execute(eventName, eventData, uniqueKey)
    }
    
    @Test
    fun `sendEvent should call callback onSuccess when successful`() = testDispatcher.runBlockingTest {
        // Arrange
        val uniqueKey = "test-key"
        val eventName = "test_event"
        val eventData = mapOf("key" to "value")
        
        // Initialize SDK first
        whenever(mockCollectDeviceDataUseCase.execute()).thenReturn(emptyMap())
        sdk.initialize(uniqueKey, null)
        
        // Act
        sdk.sendEvent(eventName, eventData, mockEventCallback)
        
        // Assert
        verify(mockEventCallback).onSuccess()
    }
    
    @Test
    fun `sendEvent should call callback onError when exception occurs`() = testDispatcher.runBlockingTest {
        // Arrange
        val uniqueKey = "test-key"
        val eventName = "test_event"
        val eventData = mapOf("key" to "value")
        val testException = Exception("Test error")
        
        // Initialize SDK first
        whenever(mockCollectDeviceDataUseCase.execute()).thenReturn(emptyMap())
        sdk.initialize(uniqueKey, null)
        
        // Set up exception for sendEvent
        whenever(mockSendEventUseCase.execute(any(), any(), any())).thenThrow(testException)
        
        // Act
        sdk.sendEvent(eventName, eventData, mockEventCallback)
        
        // Assert
        verify(mockEventCallback).onError(testException)
    }
    
    @Test
    fun `sendEvent should fail if SDK not initialized`() = testDispatcher.runBlockingTest {
        // Arrange
        val eventName = "test_event"
        val eventData = mapOf("key" to "value")
        
        // Act
        sdk.sendEvent(eventName, eventData, mockEventCallback)
        
        // Assert
        verify(mockEventCallback).onError(any())
        verify(mockLogger).error(eq("Failed to send event: SDK not initialized"), any())
    }
    
    @Test
    fun `isInitialized should return correct state`() {
        // Initial state
        assert(!sdk.isInitialized())
        
        // After initialization
        testDispatcher.runBlockingTest {
            val uniqueKey = "test-key"
            whenever(mockCollectDeviceDataUseCase.execute()).thenReturn(emptyMap())
            sdk.initialize(uniqueKey, null)
        }
        
        assert(sdk.isInitialized())
    }
}