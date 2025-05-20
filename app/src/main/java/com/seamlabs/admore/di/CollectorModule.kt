// File: com.seamlabs.admore/di/CollectorModule.kt
package com.seamlabs.admore.di

import android.content.Context
import com.seamlabs.admore.data.source.local.collector.AdvertisingIdCollector
import com.seamlabs.admore.data.source.local.collector.BaseCollector
import com.seamlabs.admore.data.source.local.collector.BluetoothCollector
import com.seamlabs.admore.data.source.local.collector.CalendarCollector
import com.seamlabs.admore.data.source.local.collector.ContactCollector
import com.seamlabs.admore.data.source.local.collector.DeviceInfoCollector
import com.seamlabs.admore.data.source.local.collector.LocationCollector
import com.seamlabs.admore.data.source.local.collector.NetworkInfoCollector
import com.seamlabs.admore.data.source.local.collector.PermissionRequiredCollector
import com.seamlabs.admore.data.source.local.collector.SmsCollector
import com.seamlabs.admore.data.source.local.collector.WifiCollector
import com.seamlabs.admore.data.source.local.factory.CollectorFactory
import com.seamlabs.admore.data.source.local.factory.CollectorFactoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class CollectorModule {

    @Provides
    @Singleton
    fun provideDeviceInfoCollector(context: Context): DeviceInfoCollector {
        return DeviceInfoCollector(context)
    }

    @Provides
    @Singleton
    fun provideAdvertisingIdCollector(context: Context): AdvertisingIdCollector {
        return AdvertisingIdCollector(context)
    }

    @Provides
    @Singleton
    fun provideNetworkInfoCollector(context: Context): NetworkInfoCollector {
        return NetworkInfoCollector(context)
    }

    @Provides
    @Singleton
    fun provideLocationCollector(context: Context): LocationCollector {
        return LocationCollector(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothCollector(context: Context): BluetoothCollector {
        return BluetoothCollector(context)
    }

    @Provides
    @Singleton
    fun provideWifiCollector(context: Context): WifiCollector {
        return WifiCollector(context)
    }

    @Provides
    @Singleton
    fun provideSMSCollector(context: Context): SmsCollector {
        return SmsCollector(context)
    }

    @Provides
    @Singleton
    fun provideContactsCollector(context: Context): ContactCollector {
        return ContactCollector(context)
    }

    @Provides
    @Singleton
    fun provideCalendarCollector(context: Context): CalendarCollector {
        return CalendarCollector(context)
    }

    @Provides
    @Singleton
    fun provideBaseCollectors(
        deviceInfoCollector: DeviceInfoCollector,
        advertisingIdCollector: AdvertisingIdCollector,
        networkInfoCollector: NetworkInfoCollector
    ): List<@JvmSuppressWildcards BaseCollector> {
        return listOf(
            deviceInfoCollector,
            advertisingIdCollector,
            networkInfoCollector
        )
    }

    @Provides
    @Singleton
    fun providePermissionRequiredCollectors(
        locationCollector: LocationCollector,
        bluetoothCollector: BluetoothCollector,
        wifiCollector: WifiCollector,
        contactCollector: ContactCollector,
        calendarCollector: CalendarCollector,
        smsCollector: SmsCollector
    ): List<@JvmSuppressWildcards PermissionRequiredCollector> {
        return listOf(
            locationCollector,
            bluetoothCollector,
            wifiCollector,
            contactCollector,
            calendarCollector,
            smsCollector
        )
    }

    @Provides
    @Singleton
    fun provideCollectorFactory(
        baseCollectors: List<@JvmSuppressWildcards BaseCollector>,
        permissionRequiredCollectors: List<@JvmSuppressWildcards PermissionRequiredCollector>
    ): CollectorFactory {
        return CollectorFactoryImpl(baseCollectors, permissionRequiredCollectors)
    }
}