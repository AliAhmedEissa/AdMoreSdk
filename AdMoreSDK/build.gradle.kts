import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.seamlabs.admore.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // Build config fields - FIXED: Separate host and base URL
        buildConfigField("String", "publicKeyBase64", "\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhylwhwTzPfMgHwNsnzbK/brtQ5sow8rSrYvCDdMUTcyz/6yEE/LTJUVM2BVRcoeg+YgZgW4ZkcpPLyccF4O9oieTcrJNLc/adArQr9fcUxpJ2pKCebpaRWOJRcxqXx4tNC3LcpgbmJE7Reu6Phc0WWDFDhXQKuQIvzdApQpU4norHBJaG4exi2BCnafqn8ncBrPX8IfgvdEThbtXl8brK9A/UAxlNcqB+ffBiApl9agjDkgOzaV+DCQJ0ZUIZ/HEpz4abZPX0wWOCFh4fCGy6DLcAxx0SwU5jCnRfKYGNog2VkcR/iXoJ2Ax5IfjX5OnTFkBSGoRLWXxxNJqpvw9CwIDAQAB\"")
        buildConfigField("String", "certificatePin", "\"\"") // Empty for IP address
        buildConfigField("String", "host", "\"209.38.231.139:8080\"") // Just hostname:port
        buildConfigField("String", "baseUrl", "\"http://209.38.231.139:8080/\"") // Full URL for Retrofit
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            tasks.whenTaskAdded {
                if (name.contains("lintVitalAnalyzeRelease")) {
                    enabled = false
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
    }

    // Resource prefix to avoid conflicts
    resourcePrefix = "admore_"

    // Packaging options to handle conflicts
    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module"
            )
        }
    }

    lint {
        disable += setOf("NullSafeMutableLiveData", "InvalidPackage")
        abortOnError = false
        checkReleaseBuilds = false
        ignoreWarnings = true
    }
}

dependencies {
    // Core Android dependencies - use 'implementation' to avoid conflicts
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Networking - with explicit exclusions to prevent conflicts
    implementation(libs.retrofit) {
        exclude(group = "com.squareup.okhttp3")
    }
    implementation(libs.converter.gson) {
        exclude(group = "com.squareup.okhttp3")
    }
    implementation(libs.gson)
    implementation(libs.logging.interceptor)

    // Add OkHttp explicitly to avoid version conflicts
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Google Play Services
    implementation(libs.play.services.ads.identifier)

    // Koin - only expose koin-android as API
    api(libs.koin.android)
    implementation("io.insert-koin:koin-core:3.5.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.seamlabs"
                artifactId = "admore-sdk"
                version = "1.0.9" // Increment version for fixes

                pom {
                    name.set("AdMore SDK")
                    description.set("AdMore SDK for Android - Data collection and analytics")
                    url.set("https://github.com/AliAhmedEissa/AdMoreSdk")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("seamlabs")
                            name.set("Seam Labs")
                            email.set("a.eissa@blueride.co")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/AliAhmedEissa/AdMoreSdk.git")
                        developerConnection.set("scm:git:ssh://github.com:AliAhmedEissa/AdMoreSdk.git")
                        url.set("https://github.com/AliAhmedEissa/AdMoreSdk/tree/main")
                    }
                }
            }
        }
    }
}