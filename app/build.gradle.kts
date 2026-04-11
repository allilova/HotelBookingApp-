plugins {
    // Android application plugin - declared ONCE here, not in root build.gradle.kts
    alias(libs.plugins.android.application)

    // KSP - needed for Room annotation processing (FavoriteHotel stays local)
    id("com.google.devtools.ksp") version "2.3.5"

    // Google Services - reads google-services.json and configures Firebase
    // Must be LAST in the plugins block
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.hotelbookingapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hotelbookingapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // ── AndroidX core ─────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ── Image loading ─────────────────────────────────────────────────────────
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0")

    // ── Map ───────────────────────────────────────────────────────────────────
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // ── CameraX ───────────────────────────────────────────────────────────────
    val camerax_version = "1.3.0"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    // ── Room (kept ONLY for FavoriteHotel - favorites stay device-local) ──────
    // Bookings, Users and CustomHotels move to Firestore in later phases
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // ── Lifecycle / ViewModel / Coroutines ────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // ── Material + Location ───────────────────────────────────────────────────
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ── Firebase ──────────────────────────────────────────────────────────────
    // BOM controls all Firebase library versions - do NOT add versions to
    // individual Firebase libraries below, the BOM manages them
    implementation(platform(libs.firebase.bom))

    // Firebase Authentication
    // Replaces our local SHA-256 + UserDao + SharedPreferences session
    implementation(libs.firebase.auth)

    // Firebase Firestore
    // Replaces Room for Booking and CustomHotel - syncs across all devices
    implementation(libs.firebase.firestore)

    // Firebase Cloud Messaging
    // Push notifications: host notified on new booking, guest notified on status change
    implementation(libs.firebase.messaging)
}