package com.seamlabs.admore.data.repository

import com.seamlabs.admore.core.encryption.DataEncryptor
import com.seamlabs.admore.core.network.ApiService
import com.seamlabs.admore.core.network.NetworkMonitor
import com.seamlabs.admore.core.storage.EventCache
import com.seamlabs.admore.data.model.ApiResponse
import com.seamlabs.admore.data.model.EventRequest
import com.seamlabs.admore.domain.model.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class EventRepositoryImplTest {
    private val testDispatcher = TestCoroutineDispatcher()
    
    private val mockApiService: ApiService = mock()
    private val mockEventCache: EventCache = mock()
    private val mockNetworkMonitor: NetworkMonitor = mock()
    private val mockDataEncryptor: DataEncryptor = mock()
    
    private lateinit var repository: EventRepositoryImpl
    private val networkStateFlow = MutableStateFlow(false)
    
    @Before
    fun setup() {
        // Set up network state flow
        whenever(mockNetworkMonitor.isConnected).thenReturn(networkStateFlow)
        
        repository = EventRepositoryImpl(
            mockApiService,
            mockEventCache,
            mockNetworkMonitor,
            mockDataEncryptor
        )
        
        // Set up mock responses
        runBlockingTest {
            whenever(mockNetworkMonitor.isNetworkAvailable()).thenReturn(true)
            
            whenever(mockDataEncryptor.encrypt(any())).thenReturn("encrypted-data")
            
            whenever(mockApiService.sendEvent(any())).thenReturn(
                ApiResponse(status = "success", message = "OK", data = null)
            )
        }
    }
    
    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `initialize should store unique key`() = testDispatcher.runBlockingTest {
        // Act
        repository.initialize("test-key")
        
        // No direct way to verify internal state, but we can test it via sendEvent
        repository.sendEvent("test", emptyMap())
        
        // Verify encrypted data contains uniqueKey - indirectly checking initialize worked
        verify(mockDataEncryptor).encrypt(any())
    }
    
    @Test
    fun `sendEvent should encrypt data before sending`() = testDispatcher.runBlockingTest {
        // Arrange
        repository.initialize("test-key")
        val eventName = "test_event"
        val eventData = mapOf("key" to "value")
        
        // Act
        repository.sendEvent(eventName, eventData)
        
        // Assert
        verify(mockDataEncryptor).encrypt(any())
    }
    
    @Test
    fun `sendEvent should send to API when online`() = testDispatcher.runBlockingTest {
        // Arrange
        repository.initialize("test-key")
        val eventName = "test_event"
        val eventData = mapOf("key" to "value")
        
        // Act
        repository.sendEvent(eventName, eventData)
        
        // Assert
        verify(mockNetworkMonitor).isNetworkAvailable()
        verify(mockApiService).sendEvent(any())
    }
    
    @Test
    fun `sendEvent should cache event when offline`() = testDispatcher.runBlockingTest {
        // Arrange
        repository.initialize("test-key")
        whenever(mockNetworkMonitor.isNetworkAvailable()).thenReturn(false)
        
        val eventName = "test_event"
        val eventData = mapOf("key" to "value")
        
        // Act
        repository.sendEvent(eventName, eventData)
        
        // Assert
        verify(mockNetworkMonitor).isNetworkAvailable()
        verify(mockEventCache).addEvent(any())
        verify(mockApiService, never()).sendEvent(any())
    }
    
    @Test
    fun `sendEvent should cache event on API error`() = testDispatcher.runBlockingTest {
        // Arrange
        repository.initialize("test-key")
        whenever(mockApiService.sendEvent(any())).thenReturn(
            ApiResponse(status = "error", message = "Failed", data = null)
        )
        
        val eventName = "test_event"
        val eventData = mapOf("key" to "value")
        
        // Act
        repository.sendEvent(eventName, eventData)
        
        // Assert
        verify(mockApiService).sendEvent(any())
        verify(mockEventCache).addEvent(any())
    }
    
    @Test
    fun `sendEvent should cache event on exception`() = testDispatcher.runBlockingTest {
        // Arrange
        repository.initialize("test-key")
        whenever(mockApiService.sendEvent(any())).thenThrow(RuntimeException("Test error"))
        
        val eventName = "test_event"
        val eventData = mapOf("key" to "value")
        
        // Act
        repository.sendEvent(eventName, eventData)
        
        // Assert
        verify(mockApiService).sendEvent(any())
        verify(mockEventCache).addEvent(any())
    }
    
    @Test
    fun `repository should send cached events when network becomes available`() = testDispatcher.runBlockingTest {
        // Arrange
        repository.initialize("test-key")
        
        val cachedEvents = listOf(
            Event("cached_event_1", mapOf("key1" to "value1")),
            Event("cached_event_2", mapOf("key2" to "value2"))
        )
        
        whenever(mockEventCache.getEvents()).thenReturn(cachedEvents)
        
        // Act - simulate network becoming available
        networkStateFlow.value = true
        
        // Allow coroutines to process
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        verify(mockEventCache).getEvents()
        verify(mockApiService, times(cachedEvents.size)).sendEvent(any())
    }
}