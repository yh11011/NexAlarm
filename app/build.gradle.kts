plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services") version "4.4.1"
    id("com.google.firebase.crashlytics") version "3.0.3"
}

android {
    namespace = "com.nexalarm.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexalarm.app"
        minSdk = 26
        targetSdk = 35

        // Version management: Update both versionCode and versionName together
        // versionCode must be incremented for each release (integer, monotonic increase)
        // versionName should follow Semantic Versioning: MAJOR.MINOR.PATCH[-SUFFIX]
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig properties for runtime environment detection
        buildConfigField("boolean", "IS_PRODUCTION", "false")
        buildConfigField("String", "BUILD_TIMESTAMP", "\"${System.currentTimeMillis()}\"")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Build types configuration
    buildTypes {
        debug {
            isDebuggable = true
            // Debug build: no obfuscation, include symbols for crash logs
            buildConfigField("boolean", "IS_PRODUCTION", "false")
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                // Debug build 不需要上傳 mapping file（沒有混淆）
                mappingFileUploadEnabled = false
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            buildConfigField("boolean", "IS_PRODUCTION", "true")
        }
    }

    // Room schema 匯出路徑（用於追蹤資料庫遷移歷史）
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}


dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons)

    implementation(libs.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Google Play Billing
    implementation(libs.billing.ktx)

    // Firebase Crashlytics for remote crash reporting (free tier)
    // Initialize via google-services.json (obtained from Firebase Console)
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // LeakCanary for memory leak detection (debug only)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    // 加密儲存（保護 JWT token）
    implementation(libs.security.crypto)

    // 背景同步（WorkManager）
    implementation(libs.work.runtime)

    // Unit tests (JVM — no device needed)
    testImplementation(libs.junit)

    // Instrumented tests (require a connected device or emulator)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.uiautomator)
    androidTestImplementation(libs.coroutines.test)
}
