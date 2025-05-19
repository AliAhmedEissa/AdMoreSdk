package com.seamlabs.admore.domain.usecase

import com.seamlabs.admore.domain.model.Permission
import com.seamlabs.admore.domain.repository.DeviceDataRepository
import com.seamlabs.admore.domain.repository.EventRepository
import com.seamlabs.admore.domain.repository.PermissionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class SendEventUseCaseTest {
    private val testDispatcher = TestCoroutineDispatcher()
    
    private val mockEventRepository: EventRepository = mock()
    private val mockDeviceDataRepository: DeviceDataRepository = mock()
    private val mockPermissionRepository: PermissionRepository = mock()
    
    private lateinit var useCase: SendEventUseCase
    
    @Before
    fun setup() {
        useCase = SendEventUseCase(
            mockEventRepository,
            mockDeviceDataRepository,
            mockPermissionRepository
        )
        
        // Set up mock responses
        runBlockingTest {
            whenever(mockDeviceDataRepository.collectBaseData()).thenReturn(
                mapOf("device_key" to "device_value")
            )
            
            whenever(mockDeviceDataRepository.getAdvertisingId()).thenReturn("test-ad-id")
            
            whenever(mockPermissionRepository.getGrantedPermissions()).thenReturn(
                listOf(Permission.LOCATION_FINE)
            )
            
            whenever(mockDeviceDataRepository.collectDataForPermission(Permission.LOCATION_FINE)).thenReturn(
                mapOf("lat" to 37.4219, "lng" to -122.0841)
            )
        }
    }
    
    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `execute should collect base device data`() = testDispatcher.runBlockingTest {
        // Arrange
        val eventName = "test_event"
        val eventData = mapOf("event_key" to "event_value")
        val uniqueKey = "test-key"
        
        // Act
        useCase.execute(eventName, eventData, uniqueKey)
        
        // Assert
        verify(mockDeviceDataRepository).collectBaseData()
    }
    
    @Test
    fun `execute should collect data for granted permissions`() = testDispatcher.runBlockingTest {
        // Arrange
        val eventName = "test_event"
        val eventData = mapOf("event_key" to "event_value")
        val uniqueKey = "test-key"
        
        // Act
        useCase.execute(eventName, eventData, uniqueKey)
        
        // Assert
        verify(mockPermissionRepository).getGrantedPermissions()
        verify(mockDeviceDataRepository).collectDataForPermission(Permission.LOCATION_FINE)
    }
    
    @Test
    fun `execute should get advertising ID`() = testDispatcher.runBlockingTest {
        // Arrange
        val eventName = "test_event"
        val eventData = mapOf("event_key" to "event_value")
        val uniqueKey = "test-key"
        
        // Act
        useCase.execute(eventName, eventData, uniqueKey)
        
        // Assert
        verify(mockDeviceDataRepository).getAdvertisingId()
    }
    
    @Test
    fun `execute should combine all data and send event`() = testDispatcher.runBlockingTest {
        // Arrange
        val eventName = "test_event"
        val eventData = mapOf("event_key" to "event_value")
        val uniqueKey = "test-key"
        
        // Set up capture for sent event data
        val dataCaptor = argumentCaptor<Map<String, Any>>()
        
        // Act
        useCase.execute(eventName, eventData, uniqueKey)
        
        // Assert
        verify(mockEventRepository).sendEvent(eq(eventName), dataCaptor.capture())
        
        val capturedData = dataCaptor.firstValue
        assert(capturedData.containsKey("device_key"))
        assert(capturedData.containsKey("lat"))
        assert(capturedData.containsKey("lng"))
        assert(capturedData.containsKey("event_key"))
        assert(capturedData.containsKey("advertising_id"))
        assert(capturedData.containsKey("unique_key"))
        
        assert(capturedData["device_key"] == "device_value")
        assert(capturedData["event_key"] == "event_value")
        assert(capturedData["advertising_id"] == "test-ad-id")
        assert(capturedData["unique_key"] == "test-key")
    }
}