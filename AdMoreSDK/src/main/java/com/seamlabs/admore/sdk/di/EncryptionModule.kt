package com.seamlabs.admore.sdk.di

import com.seamlabs.admore.sdk.core.encryption.DataEncryptor
import com.seamlabs.admore.sdk.core.encryption.X25519Encryptor
import org.koin.dsl.module

val encryptionModule = module {
    single<DataEncryptor> { X25519Encryptor() }
}