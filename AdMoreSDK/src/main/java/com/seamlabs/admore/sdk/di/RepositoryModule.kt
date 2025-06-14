package com.seamlabs.admore.sdk.di

import com.seamlabs.admore.sdk.data.repository.DeviceDataRepositoryImpl
import com.seamlabs.admore.sdk.data.repository.EventRepositoryImpl
import com.seamlabs.admore.sdk.data.repository.PermissionRepositoryImpl
import com.seamlabs.admore.sdk.domain.repository.DeviceDataRepository
import com.seamlabs.admore.sdk.domain.repository.EventRepository
import com.seamlabs.admore.sdk.domain.repository.PermissionRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {

    single<DeviceDataRepository> {
        DeviceDataRepositoryImpl(get())
    }

    single<EventRepository> {
        EventRepositoryImpl(get(), get(), get(), get())
    }

    single<PermissionRepository> {
        PermissionRepositoryImpl(androidContext(), get())
    }
}