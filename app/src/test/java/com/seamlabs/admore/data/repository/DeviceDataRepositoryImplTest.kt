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
    private val mockLocationCollector1: PermissionRequiredCollector = mock()
    private val mockLocationCollector2: PermissionRequiredCollector = mock()
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
            
            whenever(mockCollectorFactory.getCollectorsForPermission(Permission.LOCATION_FINE))
                .thenReturn(listOf(mockLocationCollector1, mockLocationCollector2))
            
            whenever(mockCollectorFactory.getAdvertisingIdCollector())
                .thenReturn(mockAdvertisingIdCollector)
            
            whenever(mockBaseCollector1.collect()).thenReturn(
                mapOf("key1" to "value1")
            )
            
            whenever(mockBaseCollector2.collect()).thenReturn(
                mapOf("key2" to "value2")
            )
            
            whenever(mockLocationCollector1.collect()).thenReturn(
                mapOf("lat" to 37.4219, "lng" to -122.0841)
            )
            
            whenever(mockLocationCollector2.collect()).thenReturn(
                mapOf("altitude" to 100.0, "accuracy" to 10.0)
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
    fun `collectDataForPermission should get data from all collectors requiring that permission`() = testDispatcher.runBlockingTest {
        // Act
        val result = repository.collectDataForPermission(Permission.LOCATION_FINE)
        
        // Assert
        verify(mockCollectorFactory).getCollectorsForPermission(Permission.LOCATION_FINE)
        verify(mockLocationCollector1).collect()
        verify(mockLocationCollector2).collect()
        
        assert(result.size == 4)
        assert(result.containsKey("lat"))
        assert(result.containsKey("lng"))
        assert(result.containsKey("altitude"))
        assert(result.containsKey("accuracy"))
        assert(result["lat"] == 37.4219)
        assert(result["lng"] == -122.0841)
        assert(result["altitude"] == 100.0)
        assert(result["accuracy"] == 10.0)
    }
    
    @Test
    fun `collectDataForPermission should return empty map if no collectors found`() = testDispatcher.runBlockingTest {
        // Arrange
        whenever(mockCollectorFactory.getCollectorsForPermission(Permission.BLUETOOTH))
            .thenReturn(emptyList())
        
        // Act
        val result = repository.collectDataForPermission(Permission.BLUETOOTH)
        
        // Assert
        verify(mockCollectorFactory).getCollectorsForPermission(Permission.BLUETOOTH)
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