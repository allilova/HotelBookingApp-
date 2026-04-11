plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") version "4.4.4" apply false
    // KSP is still needed for Room (we keep Room for FavoriteHotel)
    id("com.google.devtools.ksp") version "2.3.5"
    id("com.android.application")
    id("com.google.gms.google-services")
    // Google Services plugin must be applied here in the app module
    // This plugin reads google-services.json and generates Firebase config
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

    // ── Room (kept ONLY for FavoriteHotel - favorites are device-local) ───────
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
    // The BOM (Bill of Materials) must be declared as a platform dependency.
    // It controls versions for ALL firebase-* libraries listed below,
    // so we intentionally do NOT specify versions on the individual libraries.
    implementation(platform(libs.firebase.bom))

    // Firebase Authentication
    // - Replaces our SHA-256 password hashing + UserDao + local user session in SharedPreferences
    // - FirebaseAuth.currentUser persists the session automatically across app restarts
    // - Email/password provider used (same as our current login flow, just handled by Firebase)
    implementation(libs.firebase.auth)

    // Firebase Firestore
    // - Replaces Room for Booking and CustomHotel entities
    // - Real-time sync: a host sees new bookings without refreshing, guests see status changes live
    // - Offline persistence is enabled by default on Android (data cached locally)
    implementation(libs.firebase.firestore)

    // Firebase Cloud Messaging
    // - Sends push notifications to hosts when a guest makes a booking
    // - Sends push notifications to guests when a host confirms or cancels their booking
    // - FCM token for each user stored in Firestore users/{uid}/fcmToken
    implementation(libs.firebase.messaging)
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
}