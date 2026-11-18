import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

fun readSimpleKeyValueFile(targetFile: File): Map<String, String> {
    if (!targetFile.exists()) return emptyMap()
    val result = LinkedHashMap<String, String>()
    targetFile.forEachLine(Charsets.UTF_8) { rawLine ->
        val line = rawLine.trim()
        if (line.isNotEmpty() && !line.startsWith("#") && line.contains("=")) {
            val idx = line.indexOf('=')
            val key = line.substring(0, idx).trim()
            var value = line.substring(idx + 1).trim()
            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))
            ) {
                value = value.substring(1, value.length - 1)
            }
            result[key] = value
        }
    }
    return result
}

fun escapeBuildConfigString(raw: String): String =
    raw.replace("\\", "\\\\").replace("\"", "\\\"")

val localProps = Properties().apply {
    val lpFile = rootProject.file("local.properties")
    if (lpFile.exists()) {
        lpFile.inputStream().use { load(it) }
    }
}
val serverEnvMap = readSimpleKeyValueFile(rootProject.file("server/.env"))

// Chỉ đọc các khóa cho phép phía Client theo GEMINI.md §1 (ORS_API_KEY, STRIPE_PUBLISHABLE_KEY pk_test_..., BACKEND_BASE_URL).
// Tuyệt đối KHÔNG nhúng GEMINI_API_KEY, BREVO_API_KEY, STRIPE_SECRET_KEY hay FIREBASE_PRIVATE_KEY vào APK.
val orsApiKey: String = (localProps.getProperty("ORS_API_KEY")
    ?: localProps.getProperty("OPENROUTE_API_KEY")
    ?: "").trim()

val rawStripePubKey: String = (localProps.getProperty("STRIPE_PUBLISHABLE_KEY")
    ?: serverEnvMap["STRIPE_PUBLISHABLE_KEY"]
    ?: "").trim()
val stripePublishableKey: String = if (rawStripePubKey.startsWith("pk_")) rawStripePubKey else ""

val backendBaseUrl: String = (localProps.getProperty("BACKEND_BASE_URL")
    ?: "http://10.0.2.2:3000/").trim().let { if (it.endsWith("/")) it else "$it/" }

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

        buildConfigField("String", "ORS_API_KEY", "\"${escapeBuildConfigString(orsApiKey)}\"")
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"${escapeBuildConfigString(stripePublishableKey)}\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"${escapeBuildConfigString(backendBaseUrl)}\"")
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
        buildConfig = true
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
