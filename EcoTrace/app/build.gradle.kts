import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

private val localProps = Properties().also {
    it.load(rootProject.file("local.properties").inputStream())
}

android {
    namespace = "com.gy.ecotrace"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gy.ecotrace"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SERVER_API_URI", localProps.getProperty("SERVER_API_URI"))
        buildConfigField("String", "MAPKIT_API_KEY", localProps.getProperty("MAPKIT_API_KEY"))
        buildConfigField("String", "TRANSLATE_WEB_PAGE", localProps.getProperty("TRANSLATE_WEB_PAGE"))
        buildConfigField("String", "TRANSLATE_EXPRESSION", localProps.getProperty("TRANSLATE_EXPRESSION"))
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
    }
}

dependencies {

    // ── Core ──────────────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.core:core-animation:1.0.0")

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // ── Navigation ────────────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation(libs.androidx.navigation.compose)

    // ── Compose ───────────────────────────────────────────────────────────────
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.36.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ── Material ──────────────────────────────────────────────────────────────
    implementation("com.google.android.material:material:1.12.0")

    // ── Hilt ──────────────────────────────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:2.56.2")
    kapt("com.google.dagger:hilt-compiler:2.56.2")

    // ── Network ───────────────────────────────────────────────────────────────
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ── Room ──────────────────────────────────────────────────────────────────
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    kapt("androidx.room:room-compiler:2.7.0")

    // ── Firebase ──────────────────────────────────────────────────────────────
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")

    // ── Camera ────────────────────────────────────────────────────────────────
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-extensions:1.3.4")

    // ── ML Kit / QR ───────────────────────────────────────────────────────────
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.zxing:core:3.5.3")

    // ── UI утилиты ────────────────────────────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.valentinilk.shimmer:compose-shimmer:1.0.5")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ── Realtime (observer) ───────────────────────────────────────────────────
    implementation("io.socket:socket.io-client:2.1.0") {
        exclude(group = "org.json", module = "json") // org.json уже есть в Android
    }
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.28")

    // ── Прочее ────────────────────────────────────────────────────────────────
    implementation("com.yandex.android:maps.mobile:4.1.0-full")
    implementation(kotlin("reflect"))

    // ── Тесты ─────────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
