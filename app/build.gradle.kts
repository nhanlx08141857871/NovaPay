plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.novabank.novapay"
    compileSdk {
        version = release(35)
    }

    defaultConfig {
        applicationId = "com.novabank.novapay"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core AndroidX & Material 3
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Dagger Hilt
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    // Jetpack Navigation Component
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Retrofit 2 + Gson + OkHttp Logging
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.adapter.rxjava3)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // RxJava 3 + RxAndroid
    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    // Firebase (BOM: Auth, Cloud Firestore)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // CameraX + Guava ListenableFuture
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.guava)

    // Google ML Kit (Face Detection & Barcode Scanning)
    implementation(libs.mlkit.face.detection)
    implementation(libs.mlkit.barcode.scanning)

    // Biometric Authentication
    implementation(libs.biometric)

    // Stripe Android SDK
    implementation(libs.stripe.android)

    // Map (osmdroid) & Charts (MPAndroidChart)
    implementation(libs.osmdroid.android)
    implementation(libs.mpandroidchart)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
