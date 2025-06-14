package com.seamlabs.admore.sdk

import android.content.Context
import com.seamlabs.admore.sdk.core.logger.Logger
import com.seamlabs.admore.sdk.domain.usecase.CollectDeviceDataUseCase
import com.seamlabs.admore.sdk.domain.usecase.InitializeSDKUseCase
import com.seamlabs.admore.sdk.domain.usecase.SendEventUseCase
import com.seamlabs.admore.sdk.presentation.callback.EventCallback
import com.seamlabs.admore.sdk.presentation.callback.InitCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Main entry point for the AdMore SDK.
 * This class provides methods for initializing the SDK and sending events.
 */
class AdMoreSDK : KoinComponent {
    private val initializeSDKUseCase: InitializeSDKUseCase by inject()
    private val sendEventUseCase: SendEventUseCase by inject()
    private val collectDeviceDataUseCase: CollectDeviceDataUseCase by inject()
    private val logger: Logger by inject()

    private val sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isInitialized = false
    private var uniqueKey: String? = null

    companion object {
        @Volatile
        private var instance: AdMoreSDK? = null

        /**
         * Initializes the SDK with a context and unique key.
         * @param context Application context
         * @param uniqueKey The unique key for identifying the app
         * @param callback Optional callback for initialization status
         */
        fun initialize(context: Context, uniqueKey: String, callback: InitCallback? = null) {
            getInstance(context).initialize(uniqueKey, callback)
        }

        /**
         * Sends an event with custom data.
         * @param eventName Name of the event
         * @param data Map of key-value pairs representing event data
         * @param callback Optional callback for event sending status
         */
        fun sendEvent(eventName: String, data: Map<String, Any>, callback: EventCallback? = null) {
            instance?.sendEvent(eventName, data, callback) ?:
            throw IllegalStateException("AdMoreSDK not initialized. Call initialize() first.")
        }

        /**
         * Checks if the SDK is initialized.
         * @return true if the SDK is initialized, false otherwise
         */
        fun isInitialized(): Boolean {
            return instance?.isInitialized() ?: false
        }

        private fun getInstance(context: Context): AdMoreSDK {
            return instance ?: synchronized(this) {
                instance ?: run {
                    // Initialize Koin if not already initialized
                    val koinApp = org.koin.core.context.GlobalContext.getOrNull()
                    if (koinApp == null) {
                        org.koin.core.context.startKoin {
                            androidContext(context.applicationContext)
                            modules(com.seamlabs.admore.sdk.di.adMoreModules)
                        }
                    }
                    AdMoreSDK().also { instance = it }
                }
            }
        }
    }

    // Internal implementation of initialize
    internal fun initialize(uniqueKey: String, callback: InitCallback? = null) {
        if (isInitialized) {
            logger.warn("SDK already initialized. Ignoring duplicate initialization.")
            callback?.onSuccess()
            return
        }

        this.uniqueKey = uniqueKey

        sdkScope.launch {
            try {
                initializeSDKUseCase.execute(uniqueKey)
                isInitialized = true

                // Collect and send initial device data
                val deviceData = collectDeviceDataUseCase.execute()
                sendEventUseCase.execute("sdk_initialized", deviceData, uniqueKey)

                callback?.onSuccess()
            } catch (e: Exception) {
                logger.error("Failed to initialize SDK", e)
                callback?.onError(e)
            }
        }
    }

    // Internal implementation of sendEvent
    internal fun sendEvent(eventName: String, data: Map<String, Any>, callback: EventCallback? = null) {
        if (!isInitialized || uniqueKey == null) {
            val error = IllegalStateException("SDK not initialized. Call initialize() first.")
            logger.error("Failed to send event: SDK not initialized", error)
            callback?.onError(error)
            return
        }

        sdkScope.launch {
            try {
                sendEventUseCase.execute(eventName, data, uniqueKey!!)
                callback?.onSuccess()
            } catch (e: Exception) {
                logger.error("Failed to send event", e)
                callback?.onError(e)
            }
        }
    }

    // Internal implementation of isInitialized
    internal fun isInitialized(): Boolean {
        return isInitialized
    }
}