plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Releases are side-loaded from GitHub rather than shipped through Play, and 1.0 went out signed
// with this machine's Android debug key. Reusing it keeps new builds installable on top of copies
// people already have; a fresh key would make every update a uninstall-and-lose-your-data affair.
val sideloadKeystore = File(System.getProperty("user.home"), ".android/debug.keystore")

android {
    namespace = "com.rishi.githubstreak"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rishi.githubstreak"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }

    signingConfigs {
        if (sideloadKeystore.exists()) {
            create("sideload") {
                storeFile = sideloadKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // Null on a machine with no debug keystore; assembleRelease then leaves the APK
            // unsigned instead of failing the build.
            signingConfig = signingConfigs.findByName("sideload")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // org.json ships with Android but is a stub in JVM unit tests, so pull in the real one.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
