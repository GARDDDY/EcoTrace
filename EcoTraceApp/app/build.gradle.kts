import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.gy.ecotrace"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gy.ecotrace"
        minSdk = 29 // 29 Q -- android 10
        targetSdk = 34 // android 14
        versionCode = 1
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("String", "MAPKIT_API_KEY",
                gradleLocalProperties(rootDir).getProperty("MAPKIT_API_KEY") ?: "")
            buildConfigField("String", "SERVER_API_URI",
                gradleLocalProperties(rootDir).getProperty("SERVER_API_URI") ?: "")
        }
        debug {
            buildConfigField("String", "MAPKIT_API_KEY",
                gradleLocalProperties(rootDir).getProperty("MAPKIT_API_KEY") ?: "")
            buildConfigField("String", "SERVER_API_URI",
                gradleLocalProperties(rootDir).getProperty("SERVER_API_URI") ?: "")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = false
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0-alpha03")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.fragment:fragment-ktx:1.5.6")
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.0")
//    implementation("com.google.android.ads:mediation-test-suite:3.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.17")

    implementation("com.google.code.gson:gson:2.8.8")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
	implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging:24.0.0")
    implementation("com.github.bumptech.glide:glide:4.11.0")
    implementation("com.yandex.android:maps.mobile:4.1.0-full")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation(kotlin("reflect"))
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    implementation("com.google.mlkit:barcode-scanning:17.0.2")
    implementation("androidx.camera:camera-core:1.2.2")
    implementation("androidx.camera:camera-camera2:1.2.2")
    implementation("androidx.camera:camera-view:1.2.2")
    implementation("androidx.camera:camera-lifecycle:1.2.2")
    implementation("androidx.camera:camera-extensions:1.2.2")
    implementation("com.google.zxing:core:3.4.1")


    implementation("com.squareup.okhttp3:okhttp:4.12.0")




    androidTestDebugImplementation("androidx.test.espresso:espresso-core:3.0.2")
    androidTestDebugImplementation("androidx.test.espresso:espresso-contrib:3.0.2")
    androidTestDebugImplementation("androidx.test:runner:1.4.0")
    androidTestDebugImplementation("androidx.test:rules:1.4.0")
    androidTestDebugImplementation("junit:junit:4.13.2")

}