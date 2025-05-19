// File: com.seamlabs.admore/di/EncryptionModule.kt
package com.seamlabs.admore.di

import com.seamlabs.admore.core.encryption.DataEncryptor
import com.seamlabs.admore.core.encryption.X25519Encryptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EncryptionModule {

    @Provides
    @Singleton
    fun provideDataEncryptor(): DataEncryptor {
        return X25519Encryptor()
    }
}