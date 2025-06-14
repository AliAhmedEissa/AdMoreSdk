package com.seamlabs.admore.sdk.di

import com.seamlabs.admore.sdk.domain.usecase.CollectDeviceDataUseCase
import com.seamlabs.admore.sdk.domain.usecase.InitializeSDKUseCase
import com.seamlabs.admore.sdk.domain.usecase.SendEventUseCase
import org.koin.dsl.module

val useCaseModule = module {

    single {
        InitializeSDKUseCase(get(), get())
    }

    single {
        SendEventUseCase(get(), get(), get())
    }

    single {
        CollectDeviceDataUseCase(get(), get())
    }
}