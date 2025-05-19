package com.seamlabs.admore.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.mockito.Mockito

/**
 * Utility functions for Mockito in Kotlin tests.
 */

/**
 * Returns a mock Flow that emits the given values.
 */
fun <T> mockFlow(vararg values: T): Flow<T> = flow {
    values.forEach { emit(it) }
}

///**
// * Mock a suspended function with Mockito.
// */
//suspend fun <T> whenever(methodCall: suspend () -> T): Mockito.stubber<T> {
//    return Mockito.`when`(methodCall)
//}