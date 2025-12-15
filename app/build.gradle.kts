plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("dagger.hilt.android.plugin")

}

android {
    namespace = "com.shakilpatel.boloindia.distributor"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.shakilpatel.boloindia.distributor"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://boloindia.in/\"")
            buildConfigField("String", "BASE_URL", "\"https://boloindia.in/\"")

        }
        debug {
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.16:8000/\"")
            buildConfigField("String", "WEB_URL", "\"http://192.168.1.16:5174/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)


    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.9.6")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    kapt("com.google.dagger:hilt-android-compiler:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    // Ktor Client
    implementation("io.ktor:ktor-client-android:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    implementation("io.ktor:ktor-client-logging:3.3.3")
    implementation("io.ktor:ktor-client-auth:3.3.3")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Coil (Image loading)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // DataStore (for tokens)
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.5.2")
    implementation("androidx.camera:camera-lifecycle:1.5.2")
    implementation("androidx.camera:camera-view:1.5.2")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // QR Code Generation (Your custom library)
    implementation("com.github.alexzhirkevich:custom-qr-generator:2.0.0-alpha01")

    // Image Compression
    implementation("id.zelory:compressor:3.0.1")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.2.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.github.skydoves:colorpicker-compose:1.1.3")


}

kapt {
    correctErrorTypes = true
}
