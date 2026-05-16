plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.jadwal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jadwal.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // قراءة المفاتيح من local.properties بأمان
        val localProperties = com.android.build.gradle.internal.cxx.configure.gradleLocalProperties(rootDir, providers)
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${localProperties.getProperty("SUPABASE_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            //applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
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

    // دعم RTL
    defaultConfig {
        resourceConfigurations.addAll(listOf("ar", "en"))
    }
}

dependencies {

    // ===== Core Desugaring (لدعم Java 8+ على Android 8) =====
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    implementation("androidx.appcompat:appcompat:1.7.0")

    // ===== Core =====
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // ===== Compose BOM =====
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")

    // ===== Navigation =====
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ===== Hilt DI =====
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")

    // ===== Room Database =====
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // ===== DataStore =====
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ===== Network =====
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ===== Coroutines =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // ===== WorkManager =====
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // ===== Firebase =====
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging")

    // ===== Charts (Vico) =====
    implementation("com.patrykandpatrick.vico:compose:2.0.0-alpha.27")
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.27")

    // ===== Lottie =====
    implementation("com.airbnb.android:lottie-compose:6.5.2")

    // ===== Splash Screen =====
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ===== Accompanist =====
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")

    // ===== DateTime =====
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // ===== Gemini AI =====
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // ===== Glass Morphism (Haze) =====
    implementation("dev.chrisbanes.haze:haze:1.0.1")
    implementation("dev.chrisbanes.haze:haze-materials:1.0.1")

    // ===== Testing =====
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ===== Supabase =====
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.0")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.5.0")
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("io.coil-kt:coil-compose:2.6.0")  // للصورة الشخصية
}

// Kapt لـ Hilt
kapt {
    correctErrorTypes = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}
