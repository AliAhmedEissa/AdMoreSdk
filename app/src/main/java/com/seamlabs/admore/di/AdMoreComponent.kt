// File: com.seamlabs.admore/di/AdMoreComponent.kt
package com.seamlabs.admore.di

import android.content.Context
import com.seamlabs.admore.AdMoreSDK
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        NetworkModule::class,
        RepositoryModule::class,
        UseCaseModule::class,
        CollectorModule::class,
        EncryptionModule::class
    ]
)
interface AdMoreComponent {
    fun adMoreSDK(): AdMoreSDK

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun applicationContext(context: Context): Builder
        fun build(): AdMoreComponent
    }

    companion object {
        fun create(context: Context): AdMoreComponent {
            return DaggerAdMoreComponent.builder()
                .applicationContext(context)
                .build()
        }
    }
}