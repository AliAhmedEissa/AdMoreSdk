package com.seamlabs.admore.data.source.local.collector

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AdvertisingIdCollectorTest {
    private val testDispatcher = TestCoroutineDispatcher()
    
    private val mockContext: Context = mock()
    private val mockAdInfo: AdvertisingIdClient.Info = mock()
    private lateinit var mockStatic: MockedStatic<AdvertisingIdClient>
    
    private lateinit var collector: AdvertisingIdCollector
    
    @Before
    fun setup() {
        // Set up static mock for AdvertisingIdClient
        mockStatic = Mockito.mockStatic(AdvertisingIdClient::class.java)
        mockStatic.`when`<AdvertisingIdClient.Info> { 
            AdvertisingIdClient.getAdvertisingIdInfo(mockContext) 
        }.thenReturn(mockAdInfo)
        
        whenever(mockAdInfo.id).thenReturn("test-advertising-id")
        
        collector = AdvertisingIdCollector(mockContext)
    }
    
    @After
    fun tearDown() {
        mockStatic.close()
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `collect should return advertising ID`() = testDispatcher.runBlockingTest {
        // Act
        val result = collector.collect()
        
        // Assert
        assert(result.containsKey("advertising_id"))
        assert(result["advertising_id"] == "test-advertising-id")
    }
    
    @Test
    fun `getAdvertisingId should return cached ID on second call`() = testDispatcher.runBlockingTest {
        // Act - first call
        val result1 = collector.getAdvertisingId()
        
        // Change the mock to return a different ID
        whenever(mockAdInfo.id).thenReturn("different-id")
        
        // Act - second call should use cached value
        val result2 = collector.getAdvertisingId()
        
        // Assert
        assert(result1 == "test-advertising-id")
        assert(result2 == "test-advertising-id") // Should still be the first value
    }
    
    @Test
    fun `getAdvertisingId should handle exception`() = testDispatcher.runBlockingTest {
        // Arrange
        mockStatic.`when`<AdvertisingIdClient.Info> { 
            AdvertisingIdClient.getAdvertisingIdInfo(mockContext) 
        }.thenThrow(RuntimeException("Test error"))
        
        // Act
        val result = collector.getAdvertisingId()
        
        // Assert
        assert(result == null)
    }
}