package com.seamlabs.admore.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class NetworkMonitorTest {
    private val testDispatcher = TestCoroutineDispatcher()
    
    private val mockContext: Context = mock()
    private val mockConnectivityManager: ConnectivityManager = mock()
    private val mockNetwork: Network = mock()
    private val mockNetworkCapabilities: NetworkCapabilities = mock()
    
    @Captor
    private lateinit var networkCallbackCaptor: ArgumentCaptor<ConnectivityManager.NetworkCallback>
    
    private lateinit var networkMonitor: NetworkMonitor
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Set up context mock
        whenever(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(mockConnectivityManager)
        
        // Set up connectivity manager
        whenever(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
        whenever(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
            .thenReturn(mockNetworkCapabilities)
        
        // Set up network capabilities
        whenever(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(true)
        
        // Create network monitor (which registers callback)
        networkMonitor = NetworkMonitor(mockContext)
        
        // Capture network callback
        verify(mockConnectivityManager).registerNetworkCallback(
            any(NetworkRequest::class.java),
            networkCallbackCaptor.capture()
        )
    }
    
    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `isNetworkAvailable should return true when network is available`() {
        // Arrange
        whenever(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
        whenever(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
            .thenReturn(mockNetworkCapabilities)
        whenever(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(true)
        
        // Act
        val result = networkMonitor.isNetworkAvailable()
        
        // Assert
        assert(result)
    }
    
    @Test
    fun `isNetworkAvailable should return false when network is not available`() {
        // Arrange
        whenever(mockConnectivityManager.activeNetwork).thenReturn(null)
        
        // Act
        val result = networkMonitor.isNetworkAvailable()
        
        // Assert
        assert(!result)
    }
    
    @Test
    fun `isNetworkAvailable should return false when network has no internet capability`() {
        // Arrange
        whenever(mockConnectivityManager.activeNetwork).thenReturn(mockNetwork)
        whenever(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
            .thenReturn(mockNetworkCapabilities)
        whenever(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(false)
        
        // Act
        val result = networkMonitor.isNetworkAvailable()
        
        // Assert
        assert(!result)
    }
    
    @Test
    fun `isConnected flow should emit true when network becomes available`() = testDispatcher.runBlockingTest {
        // Arrange
        val callback = networkCallbackCaptor.value
        val connectivityValues = mutableListOf<Boolean>()
        
        // Act
        val job = launch {
            networkMonitor.isConnected.collect { connectivityValues.add(it) }
        }
        
        // Simulate network becoming available
        callback.onAvailable(mockNetwork)
        
        // Clean up
        job.cancel()
        
        // Assert
        assert(connectivityValues.last())
    }
    
    @Test
    fun `isConnected flow should emit false when network is lost`() = testDispatcher.runBlockingTest {
        // Arrange
        val callback = networkCallbackCaptor.value
        val connectivityValues = mutableListOf<Boolean>()
        
        // Set up isNetworkAvailable to return false after network loss
        whenever(mockConnectivityManager.activeNetwork).thenReturn(null)
        
        // Act
        val job = launch {
            networkMonitor.isConnected.collect { connectivityValues.add(it) }
        }
        
        // Simulate network becoming available then lost
        callback.onAvailable(mockNetwork)
        callback.onLost(mockNetwork)
        
        // Clean up
        job.cancel()
        
        // Assert
        assert(!connectivityValues.last())
    }
}