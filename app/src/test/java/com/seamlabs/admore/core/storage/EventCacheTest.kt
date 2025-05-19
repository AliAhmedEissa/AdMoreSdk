package com.seamlabs.admore.core.storage

import com.seamlabs.admore.domain.model.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class EventCacheTest {
    private val testDispatcher = TestCoroutineDispatcher()
    
    private lateinit var eventCache: EventCache
    
    @Before
    fun setup() {
        eventCache = EventCache()
    }
    
    @After
    fun tearDown() {
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `addEvent should add event to cache`() = testDispatcher.runBlockingTest {
        // Arrange
        val event = Event("test_event", mapOf("key" to "value"))
        
        // Act
        eventCache.addEvent(event)
        val events = eventCache.getEvents()
        
        // Assert
        assert(events.size == 1)
        assert(events[0] == event)
    }
    
    @Test
    fun `removeEvent should remove event from cache`() = testDispatcher.runBlockingTest {
        // Arrange
        val event1 = Event("event1", mapOf("key1" to "value1"))
        val event2 = Event("event2", mapOf("key2" to "value2"))
        
        // Add events
        eventCache.addEvent(event1)
        eventCache.addEvent(event2)
        
        // Act
        eventCache.removeEvent(event1)
        val events = eventCache.getEvents()
        
        // Assert
        assert(events.size == 1)
        assert(events[0] == event2)
    }
    
    @Test
    fun `getEvents should return all events`() = testDispatcher.runBlockingTest {
        // Arrange
        val event1 = Event("event1", mapOf("key1" to "value1"))
        val event2 = Event("event2", mapOf("key2" to "value2"))
        
        // Add events
        eventCache.addEvent(event1)
        eventCache.addEvent(event2)
        
        // Act
        val events = eventCache.getEvents()
        
        // Assert
        assert(events.size == 2)
        assert(events.contains(event1))
        assert(events.contains(event2))
    }
    
    @Test
    fun `clearEvents should remove all events`() = testDispatcher.runBlockingTest {
        // Arrange
        val event1 = Event("event1", mapOf("key1" to "value1"))
        val event2 = Event("event2", mapOf("key2" to "value2"))
        
        // Add events
        eventCache.addEvent(event1)
        eventCache.addEvent(event2)
        
        // Act
        eventCache.clearEvents()
        val events = eventCache.getEvents()
        
        // Assert
        assert(events.isEmpty())
    }
    
    @Test
    fun `cache should maintain event order`() = testDispatcher.runBlockingTest {
        // Arrange
        val event1 = Event("event1", mapOf("key1" to "value1"))
        val event2 = Event("event2", mapOf("key2" to "value2"))
        val event3 = Event("event3", mapOf("key3" to "value3"))
        
        // Add events
        eventCache.addEvent(event1)
        eventCache.addEvent(event2)
        eventCache.addEvent(event3)
        
        // Act
        val events = eventCache.getEvents()
        
        // Assert
        assert(events.size == 3)
        assert(events[0] == event1)
        assert(events[1] == event2)
        assert(events[2] == event3)
    }
}