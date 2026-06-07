plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.mapmate"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mapmate"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // demo mode on -> app runs everyone's merged logic on-device with seeded data
        // so it works on a bare emulator without live firebase / cloud functions
        // flip to false once real google-services.json + deployed functions are in place
        buildConfigField("boolean", "DEMO_MODE", "true")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        // mike's privacy lib uses java.time, desugaring backports it to minSdk 24
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.play.services.location)
    // play-services-maps + maps-compose dropped -> the NavX design draws its own stylized
    // canvas map (com.mapmate.ui.map.StylizedMapCanvas), so no google maps key is needed to run
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform("com.google.firebase:firebase-bom:34.14.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
