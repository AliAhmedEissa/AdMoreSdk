package com.seamlabs.admore.domain.usecase

import com.seamlabs.admore.domain.model.Permission
import com.seamlabs.admore.domain.repository.DeviceDataRepository
import com.seamlabs.admore.domain.repository.PermissionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CollectDeviceDataUseCaseTest {
    private val testDispatcher = TestCoroutineDispatcher()
    
    private val mockDeviceDataRepository: DeviceDataRepository = mock()
    private val mockPermissionRepository: PermissionRepository = mock()
    
    private lateinit var useCase: CollectDeviceDataUseCase
    
    @Before
    fun setup() {
        useCase = CollectDeviceDataUseCase(
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
        // Act
        val result = useCase.execute()
        
        // Assert
        verify(mockDeviceDataRepository).collectBaseData()
        assert(result.containsKey("device_key"))
        assert(result["device_key"] == "device_value")
    }
    
    @Test
    fun `execute should collect data for granted permissions`() = testDispatcher.runBlockingTest {
        // Act
        val result = useCase.execute()
        
        // Assert
        verify(mockPermissionRepository).getGrantedPermissions()
        verify(mockDeviceDataRepository).collectDataForPermission(Permission.LOCATION_FINE)
        assert(result.containsKey("lat"))
        assert(result.containsKey("lng"))
    }
    
    @Test
    fun `execute should add advertising ID if available`() = testDispatcher.runBlockingTest {
        // Act
        val result = useCase.execute()
        
        // Assert
        verify(mockDeviceDataRepository).getAdvertisingId()
        assert(result.containsKey("advertising_id"))
        assert(result["advertising_id"] == "test-ad-id")
    }
    
    @Test
    fun `execute should handle null advertising ID`() = testDispatcher.runBlockingTest {
        // Arrange
        whenever(mockDeviceDataRepository.getAdvertisingId()).thenReturn(null)
        
        // Act
        val result = useCase.execute()
        
        // Assert
        verify(mockDeviceDataRepository).getAdvertisingId()
        assert(!result.containsKey("advertising_id"))
    }
}