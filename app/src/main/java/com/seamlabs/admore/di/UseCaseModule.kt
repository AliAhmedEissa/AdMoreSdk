package com.seamlabs.admore.di

import com.seamlabs.admore.domain.repository.DeviceDataRepository
import com.seamlabs.admore.domain.repository.EventRepository
import com.seamlabs.admore.domain.repository.PermissionRepository
import com.seamlabs.admore.domain.usecase.CollectDeviceDataUseCase
import com.seamlabs.admore.domain.usecase.InitializeSDKUseCase
import com.seamlabs.admore.domain.usecase.SendEventUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideInitializeSDKUseCase(
        eventRepository: EventRepository,
        deviceDataRepository: DeviceDataRepository
    ): InitializeSDKUseCase {
        return InitializeSDKUseCase(eventRepository, deviceDataRepository)
    }

    @Provides
    @Singleton
    fun provideSendEventUseCase(
        eventRepository: EventRepository,
        deviceDataRepository: DeviceDataRepository,
        permissionRepository: PermissionRepository
    ): SendEventUseCase {
        return SendEventUseCase(eventRepository, deviceDataRepository, permissionRepository)
    }

    @Provides
    @Singleton
    fun provideCollectDeviceDataUseCase(
        deviceDataRepository: DeviceDataRepository,
        permissionRepository: PermissionRepository
    ): CollectDeviceDataUseCase {
        return CollectDeviceDataUseCase(deviceDataRepository, permissionRepository)
    }
}