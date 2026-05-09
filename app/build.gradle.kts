import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

/**
 * Load API Key from local.properties
 */
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val itadApiKey: String = localProperties.getProperty("itad_api_key") ?: ""

android {
    namespace = "com.example.treasure"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.treasure"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //  Inject into BuildConfig
        buildConfigField("String", "ITAD_API_KEY", "\"$itadApiKey\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "ITAD_API_KEY", "\"$itadApiKey\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "ITAD_API_KEY", "\"$itadApiKey\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
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
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation.layout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    val nav_version = "2.9.3"
    implementation("androidx.navigation:navigation-compose:$nav_version")
    androidTestImplementation("androidx.navigation:navigation-testing:$nav_version")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Material3
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha03")

    // Icons
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Coil
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-android-compiler:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // WorkManager
    val work_version = "2.11.1"
    implementation("androidx.work:work-runtime-ktx:$work_version")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$nav_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$nav_version")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$nav_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0-alpha03")

    // Arrow
    implementation("io.arrow-kt:arrow-core:2.1.2")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")

    implementation("androidx.paging:paging-runtime:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1")

    // Palette & Collections
    implementation(libs.androidx.palette)
    implementation(libs.androidx.collection.ktx)

    // Media3
    val media3_version = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-ui:$media3_version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3_version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3_version")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Firebase Bill of Materials (BoM) - manages versions for you
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    // Remote Config
    implementation("com.google.firebase:firebase-config")

    // Analytics (Optional, but usually recommended with Firebase)
    implementation("com.google.firebase:firebase-analytics")

    // Google Play Services
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

}
