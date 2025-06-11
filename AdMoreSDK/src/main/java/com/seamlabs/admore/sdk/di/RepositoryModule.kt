// File: com.seamlabs.admore/di/RepositoryModule.kt
package com.seamlabs.admore.sdk.di

import com.seamlabs.admore.sdk.data.repository.DeviceDataRepositoryImpl
import com.seamlabs.admore.sdk.data.repository.EventRepositoryImpl
import com.seamlabs.admore.sdk.data.repository.PermissionRepositoryImpl
import com.seamlabs.admore.sdk.domain.repository.DeviceDataRepository
import com.seamlabs.admore.sdk.domain.repository.EventRepository
import com.seamlabs.admore.sdk.domain.repository.PermissionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDeviceDataRepository(impl: DeviceDataRepositoryImpl): DeviceDataRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(impl: PermissionRepositoryImpl): PermissionRepository
}