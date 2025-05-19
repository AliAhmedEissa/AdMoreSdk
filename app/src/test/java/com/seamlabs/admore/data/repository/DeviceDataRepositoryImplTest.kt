package com.seamlabs.admore.data.repository

import com.seamlabs.admore.data.source.local.collector.AdvertisingIdCollector
import com.seamlabs.admore.data.source.local.collector.BaseCollector
import com.seamlabs.admore.data.source.local.collector.PermissionRequiredCollector
import com.seamlabs.admore.data.source.local.factory.CollectorFactory
import com.seamlabs.admore.domain.model.Permission
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
class DeviceDataRepositoryImplTest {
    private val testDispatcher = TestCoroutineDispatcher()
    
    private val mockCollectorFactory: CollectorFactory = mock()
    private val mockBaseCollector1: BaseCollector = mock()
    private val mockBaseCollector2: BaseCollector = mock()
    private val mockLocationCollector: PermissionRequiredCollector = mock()
    private val mockAdvertisingIdCollector: AdvertisingIdCollector = mock()
    
    private lateinit var repository: DeviceDataRepositoryImpl
    
    @Before
    fun setup() {
        repository = DeviceDataRepositoryImpl(mockCollectorFactory)
        
        // Set up mock responses
        runBlockingTest {
            whenever(mockCollectorFactory.getBaseCollectors()).thenReturn(
                listOf(mockBaseCollector1, mockBaseCollector2)
            )
            
            whenever(mockCollectorFactory.getCollectorForPermission(Permission.LOCATION_FINE))
                .thenReturn(mockLocationCollector)
            
            whenever(mockCollectorFactory.getAdvertisingIdCollector())
                .thenReturn(mockAdvertisingIdCollector)
            
            whenever(mockBaseCollector1.collect()).thenReturn(
                mapOf("key1" to "value1")
            )
            
            whenever(mockBaseCollector2.collect()).thenReturn(
                mapOf("key2" to "value2")
            )
            
            whenever(mockLocationCollector.collect()).thenReturn(
                mapOf("lat" to 37.4219, "lng" to -122.0841)
            )
            
            whenever(mockAdvertisingIdCollector.getAdvertisingId()).thenReturn("test-ad-id")
        }
    }
    
    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `initialize should not throw exception`() = testDispatcher.runBlockingTest {
        // Act & Assert (no exception)
        repository.initialize()
    }
    
    @Test
    fun `collectBaseData should get data from all base collectors`() = testDispatcher.runBlockingTest {
        // Act
        val result = repository.collectBaseData()
        
        // Assert
        verify(mockCollectorFactory).getBaseCollectors()
        verify(mockBaseCollector1).collect()
        verify(mockBaseCollector2).collect()
        
        assert(result.size == 2)
        assert(result.containsKey("key1"))
        assert(result.containsKey("key2"))
        assert(result["key1"] == "value1")
        assert(result["key2"] == "value2")
    }
    
    @Test
    fun `collectDataForPermission should get data from correct collector`() = testDispatcher.runBlockingTest {
        // Act
        val result = repository.collectDataForPermission(Permission.LOCATION_FINE)
        
        // Assert
        verify(mockCollectorFactory).getCollectorForPermission(Permission.LOCATION_FINE)
        verify(mockLocationCollector).collect()
        
        assert(result.size == 2)
        assert(result.containsKey("lat"))
        assert(result.containsKey("lng"))
        assert(result["lat"] == 37.4219)
        assert(result["lng"] == -122.0841)
    }
    
    @Test
    fun `collectDataForPermission should return empty map if no collector found`() = testDispatcher.runBlockingTest {
        // Arrange
        whenever(mockCollectorFactory.getCollectorForPermission(Permission.BLUETOOTH))
            .thenReturn(null)
        
        // Act
        val result = repository.collectDataForPermission(Permission.BLUETOOTH)
        
        // Assert
        verify(mockCollectorFactory).getCollectorForPermission(Permission.BLUETOOTH)
        assert(result.isEmpty())
    }
    
    @Test
    fun `getAdvertisingId should return ID from collector`() = testDispatcher.runBlockingTest {
        // Act
        val result = repository.getAdvertisingId()
        
        // Assert
        verify(mockCollectorFactory).getAdvertisingIdCollector()
        verify(mockAdvertisingIdCollector).getAdvertisingId()
        assert(result == "test-ad-id")
    }
}