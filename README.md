# AdMore SDK for Android

[![Maven Central](https://img.shields.io/maven-central/v/com.seamlabs/admore)](https://search.maven.org/artifact/com.seamlabs/admore)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

AdMore SDK is a lightweight analytics and data collection library for Android that allows you to easily track events and collect device information in your application.

## Features

- 📱 Simple and lightweight integration
- 🔒 End-to-end data encryption
- 📊 Automatic device data collection
- 🔄 Offline caching with automatic retries
- 📍 Permission-aware data collection
- 🔋 Minimal battery and performance impact

## Installation

### Gradle

Add the dependency to your app's `build.gradle` file:

```gradle
dependencies {
    implementation 'com.seamlabs:admore:1.0.0'
}
```

### Maven

```xml
<dependency>
  <groupId>com.seamlabs</groupId>
  <artifactId>admore</artifactId>
  <version>1.0.0</version>
  <type>aar</type>
</dependency>
```

## Permissions

Add these permissions to your AndroidManifest.xml:

```xml
<!-- Required permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

## Quick Start

### 1. Initialize the SDK

Initialize AdMore SDK in your Application class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize with your unique key
        AdMoreSDK.initialize(
            context = this,
            uniqueKey = "YOUR_UNIQUE_KEY_HERE"
        )
    }
}
```

Don't forget to register your Application class in AndroidManifest.xml:

```xml
<application
    android:name=".MyApplication"
    ...
```

### 2. Track Events

After initialization, you can track events anywhere in your app:

```kotlin
// Track a simple event
AdMoreSDK.sendEvent(
    eventName = "button_click",
    data = mapOf("button_id" to "sign_up_button")
)

// Track event with multiple properties
AdMoreSDK.sendEvent(
    eventName = "purchase_completed",
    data = mapOf(
        "product_id" to "com.example.premium",
        "price" to 9.99,
        "currency" to "USD",
        "payment_method" to "credit_card"
    )
)
```

### 3. Using Callbacks (Optional)

For more control, you can use callbacks to handle success or failure:

```kotlin
AdMoreSDK.sendEvent(
    eventName = "level_complete",
    data = mapOf(
        "level_id" to "level_5",
        "score" to 1250,
        "time_seconds" to 45
    ),
    callback = object : EventCallback {
        override fun onSuccess() {
            Log.d("MyApp", "Event tracked successfully")
        }

        override fun onError(error: Throwable) {
            Log.e("MyApp", "Failed to track event", error)
        }
    }
)
```

You can also use callbacks when initializing the SDK:

```kotlin
AdMoreSDK.initialize(
    context = this,
    uniqueKey = "YOUR_UNIQUE_KEY_HERE",
    callback = object : InitCallback {
        override fun onSuccess() {
            Log.d("MyApp", "SDK initialized successfully")
        }

        override fun onError(error: Throwable) {
            Log.e("MyApp", "Failed to initialize SDK", error)
        }
    }
)
```

## Supported Data Types

AdMore SDK supports the following data types for event properties:

| Type | Example |
|------|---------|
| String | `"user_name" to "John Doe"` |
| Int | `"items_count" to 5` |
| Long | `"user_id" to 123456789L` |
| Float | `"completion" to 0.75f` |
| Double | `"price" to 9.99` |
| Boolean | `"is_premium" to true` |

Example with mixed types:

```kotlin
AdMoreSDK.sendEvent(
    eventName = "user_action",
    data = mapOf(
        "user_id" to "abc123",              // String
        "premium" to true,                  // Boolean
        "level" to 5,                       // Int
        "playtime_hours" to 2.5,            // Double
        "items_collected" to 147L,          // Long
        "completion_percentage" to 85.5f    // Float
    )
)
```

## Common Use Cases

### Tracking Screen Views

```kotlin
fun trackScreenView(screenName: String) {
    AdMoreSDK.sendEvent(
        eventName = "screen_view",
        data = mapOf("screen_name" to screenName)
    )
}

// Usage
trackScreenView("HomeScreen")
trackScreenView("ProductDetails")
```

### Tracking User Actions

```kotlin
fun trackButtonClick(buttonId: String) {
    AdMoreSDK.sendEvent(
        eventName = "button_click",
        data = mapOf("button_id" to buttonId)
    )
}

// Usage
trackButtonClick("login_button")
```

### Tracking E-commerce Events

```kotlin
fun trackPurchase(productId: String, price: Double, currency: String) {
    AdMoreSDK.sendEvent(
        eventName = "purchase",
        data = mapOf(
            "product_id" to productId,
            "price" to price,
            "currency" to currency,
            "timestamp" to System.currentTimeMillis()
        )
    )
}

// Usage
trackPurchase("premium_subscription", 9.99, "USD")
```

### Tracking App Performance

```kotlin
fun trackAppStart(startupTimeMs: Long) {
    AdMoreSDK.sendEvent(
        eventName = "app_start",
        data = mapOf(
            "startup_time_ms" to startupTimeMs,
            "is_cold_start" to true
        )
    )
}

// Usage
val startTime = System.currentTimeMillis()
// ... app initialization ...
val startupTime = System.currentTimeMillis() - startTime
trackAppStart(startupTime)
```


## Predefined Event Keys

While you can use any key names for your events, the following keys have special meaning:

| Key | Description | Example Value |
|-----|-------------|--------------|
| `user_id` | Unique identifier for the user | `"abc123"` |
| `session_id` | Identifier for the current session | `"xyz789"` |
| `screen_name` | Name of the current screen | `"checkout"` |
| `event_category` | Category of the event | `"purchase"` |
| `item_id` | Identifier for an item | `"prod456"` |
| `currency` | Currency code for monetary values | `"USD"` |
| `value` | Numeric value for the event | `99.99` |

## Best Practices

### 1. Initialize Early

Initialize the SDK in your Application class to ensure it's ready as soon as your app starts.

### 2. Use Meaningful Event Names

Choose descriptive event names for better analytics:

```kotlin
// Good
AdMoreSDK.sendEvent("purchase_completed", data)

// Avoid
AdMoreSDK.sendEvent("event1", data)
```

### 3. Be Consistent with Event Structure

Use a consistent structure for similar events:

```kotlin
// All screen views use the same format
AdMoreSDK.sendEvent("screen_view", mapOf("screen_name" to screenName))
```

### 4. Check Initialization Status

If you're unsure about the SDK's initialization status:

```kotlin
if (AdMoreSDK.isInitialized()) {
    // Safe to send events
    AdMoreSDK.sendEvent("some_event", data)
} else {
    // Handle not initialized case
    Log.w("MyApp", "AdMore SDK not initialized")
}
```

### 5. Don't Over-Track

Focus on meaningful events rather than tracking everything. This helps with:
- Reducing network usage
- Improving battery life
- Keeping analytics data relevant

## Troubleshooting

### Common Issues

**1. SDK Not Initialized**

If you see `"SDK not initialized"` errors, ensure you've called `AdMoreSDK.initialize()` before sending any events, preferably in your Application class.

**2. Events Not Being Sent**

Check the following:
- Internet connectivity
- Correct unique key
- SDK initialization

**3. ProGuard/R8 Issues**

If you're using ProGuard/R8, ensure you have the correct rules in your `proguard-rules.pro` file:

```proguard
# AdMore SDK
-keep class com.seamlabs.admore.** { *; }
```

## Size Impact

The AdMore SDK adds approximately 150KB to your APK size after optimization.

## Privacy Compliance

### Advertising ID

The SDK collects the device's Advertising ID, which requires disclosure in your privacy policy:

> This app collects device identifiers, including advertising identifiers, to analyze app usage and improve our services. You can reset your advertising identifier through your device settings.

### GDPR and CCPA Compliance

For users in the EU or California, we recommend implementing a consent management solution and only initializing the SDK after obtaining proper consent.

## License

The AdMore SDK is licensed under the [MIT License](LICENSE).

## Support

For support, contact us at:

- Email: support@seamlabs.com
- Documentation: https://docs.seamlabs.com/admore
