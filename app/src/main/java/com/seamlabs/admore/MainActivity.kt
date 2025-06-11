package com.seamlabs.admore

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.seamlabs.admore.sdk.AdMoreSDK
import com.seamlabs.admore.sdk.presentation.callback.EventCallback
import com.seamlabs.admore.sdk.presentation.callback.InitCallback

import com.seamlabs.admore.ui.theme.AdMoreSDKTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdMoreSDKTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android", modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            // Initialize AdMore SDK with your unique key
            AdMoreSDK.initialize(this, "YOUR_UNIQUE_KEY", callback = object : InitCallback {
                override fun onSuccess() {
                    lifecycleScope.launch {
                        AdMoreSDK.sendEvent(eventName = "event_name",
                            data = mapOf("key" to "value"),
                            callback = object : EventCallback {
                                override fun onSuccess() {
                                    Log.d("myDebugData", "Event sent successfully")
                                }

                                override fun onError(error: Throwable) {
                                    Log.e("AdMoreSDK", "Failed to send event", error)
                                }
                            })
                    }

                }

                override fun onError(error: Throwable) {
                }


            })




        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AdMoreSDKTheme {
        Greeting("Android")
    }
}