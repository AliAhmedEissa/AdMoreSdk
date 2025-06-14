package com.seamlabs.admore.sdk.di

import com.seamlabs.admore.sdk.BuildConfig
import com.seamlabs.admore.sdk.core.network.ApiService
import com.seamlabs.admore.sdk.core.network.CertificatePinner
import com.seamlabs.admore.sdk.core.network.NetworkMonitor
import com.seamlabs.admore.sdk.core.network.RetryInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {

    single<HttpLoggingInterceptor> {
        HttpLoggingInterceptor().apply {
            // Only enable detailed logging in debug builds to avoid performance issues
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS // Changed from BODY to HEADERS to avoid stream issues
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    single { CertificatePinner() }

    single { RetryInterceptor() }

    single<OkHttpClient> {
        OkHttpClient.Builder().apply {
            // Add retry interceptor first, then logging interceptor
            addInterceptor(get<RetryInterceptor>())

            // Only add logging interceptor in debug builds
          //  if (BuildConfig.DEBUG) {
                addInterceptor(get<HttpLoggingInterceptor>())
       //     }

            // Only add certificate pinner if it's not empty (i.e., not for IP addresses)
            val certificatePinner = get<CertificatePinner>().getPinner()
            if (certificatePinner.pins.isNotEmpty()) {
                certificatePinner(certificatePinner)
            }

            connectTimeout(30L, TimeUnit.SECONDS)
            readTimeout(30L, TimeUnit.SECONDS)
            writeTimeout(30L, TimeUnit.SECONDS)

            // Add connection pooling for better performance
            connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))

            // Disable automatic retries to avoid conflicts with our custom retry interceptor
            retryOnConnectionFailure(false)
        }.build()
    }

    single<Retrofit> {
        // Construct the base URL properly
        val baseUrl = if (BuildConfig.host.startsWith("http://") || BuildConfig.host.startsWith("https://")) {
            // If host already contains scheme, use it directly
            BuildConfig.host.let { if (it.endsWith("/")) it else "$it/" }
        } else {
            // If host doesn't contain scheme, add http:// and trailing slash
            "http://${BuildConfig.host}/"
        }

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<ApiService> {
        get<Retrofit>().create(ApiService::class.java)
    }

    single { NetworkMonitor(androidContext()) }
}