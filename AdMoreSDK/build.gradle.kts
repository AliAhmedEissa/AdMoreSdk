import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dagger.hilt)        // Dagger-Hilt for dependency injection
    alias(libs.plugins.kotlin.kapt)        // Kotlin annotation processor plugin (KAPT)
    `maven-publish`                        // Add Maven Publish plugin
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
        buildConfigField("String","publicKeyBase64",localProperties.getProperty("publicKeyBase64"))
        buildConfigField("String","certificatePin",localProperties.getProperty("certificatePin"))
        buildConfigField("String","host",localProperties.getProperty("host"))

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures{
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.logging.interceptor)

    implementation(libs.hilt.android)                    // Hilt Android dependency
    kapt(libs.hilt.compiler)                             // Hilt annotation processor

    implementation(libs.play.services.ads.identifier)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                
                groupId = "com.seamlabs"
                artifactId = "admore-sdk"
                version = "1.0.0"  // You can change this version as needed
                
                pom {
                    name.set("AdMore SDK")
                    description.set("AdMore SDK for Android")
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
                }
            }
        }
        
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/AliAhmedEissa/AdMoreSdk")
            }
        }
    }
}