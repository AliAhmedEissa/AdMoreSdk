package com.seamlabs.admore.data.source.local.collector

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.util.DisplayMetrics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.*

@ExperimentalCoroutinesApi
class DeviceInfoCollectorTest {
    private val testDispatcher = TestCoroutineDispatcher()
    
    private val mockContext: Context = mock()
    private val mockResources: Resources = mock()
    private val mockConfiguration: Configuration = mock()
    private val mockDisplayMetrics: DisplayMetrics = mock()
    private val mockLocale = Locale.US
    
    private lateinit var collector: DeviceInfoCollector
    
    @Before
    fun setup() {
        // Set up context mocks
        whenever(mockContext.resources).thenReturn(mockResources)
        whenever(mockResources.configuration).thenReturn(mockConfiguration)
        whenever(mockResources.displayMetrics).thenReturn(mockDisplayMetrics)
        
        // Set up configuration
        whenever(mockConfiguration.locales).thenReturn(mockLocales() as LocaleList)
        
        collector = DeviceInfoCollector(mockContext)
    }
    
    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `collect should return device information`() = testDispatcher.runBlockingTest {
        // Act
        val result = collector.collect()
        
        // Assert
        assert(result.containsKey("device_manufacturer"))
        assert(result.containsKey("device_model"))
        assert(result.containsKey("device_brand"))
        assert(result.containsKey("android_version"))
        assert(result.containsKey("sdk_version"))
        assert(result.containsKey("device_language"))
        assert(result.containsKey("device_country"))
        assert(result.containsKey("is_emulator"))
        
        // Check actual values (depends on where test is running)
        assert(result["device_manufacturer"] == Build.MANUFACTURER)
        assert(result["device_model"] == Build.MODEL)
        assert(result["device_brand"] == Build.BRAND)
        assert(result["android_version"] == Build.VERSION.RELEASE)
        assert(result["sdk_version"] == Build.VERSION.SDK_INT)
        assert(result["device_language"] == "en")
        assert(result["device_country"] == "US")
    }
    
    @Test
    fun `isEmulator should detect emulator`() = testDispatcher.runBlockingTest {
        // This test depends on where it's running, so we can't make definitive assertions
        // We can just ensure the method runs without errors
        val result = collector.collect()
        assert(result.containsKey("is_emulator"))
        assert(result["is_emulator"] is Boolean)
    }
    
    // Helper method to mock LocaleList for API 24+
    private fun mockLocales(): Any {
        // Create a mock that works for the test
        class MockLocaleList {
            operator fun get(index: Int): Locale = mockLocale
        }
        return MockLocaleList()
    }
}