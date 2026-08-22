plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization")
    // alias(libs.plugins.hilt)
    kotlin("kapt")
    id("com.google.gms.google-services")
}

import java.util.Properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.proyectoing.glocosasmarai"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.proyectoing.glocosasmarai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_API_KEY", localProperties.getProperty("GEMINI_API_KEY") ?: "\"\"")
        buildConfigField("String", "BACKEND_URL", localProperties.getProperty("BACKEND_URL") ?: "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kapt {
        correctErrorTypes = true  // ← agregar esto
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST"
            )
        }
    }
}

dependencies {
    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Necesario para 'rememberLauncherForActivityResult'
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    // OpenCV - Commented out for now, will be added later
    // implementation(libs.opencv.android)
    
    // ML Kit - Commented out for now, will be added later
    // implementation(libs.mlkit.text.recognition)
    
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // WorkManager (Para programar las notificaciones de comida)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    
    // Gemini API - Google AI
    implementation("com.google.ai.client.generativeai:generativeai:0.2.2")
    
    // Dependency Injection - Commented out for now
    // implementation(libs.hilt.android)
    // kapt(libs.hilt.compiler)
    
    // Permissions - Commented out for now, will be added later
    // implementation(libs.accompanist.permissions)
    
    // Charts
    implementation(libs.vico.compose)
    
    // PDF Generation - Using iText7
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.itextpdf:kernel:7.2.5")
    implementation("com.itextpdf:io:7.2.5")
    implementation("com.itextpdf:layout:7.2.5")
    implementation("com.itextpdf:font-asian:7.2.5")
    implementation("com.itextpdf:sign:7.2.5")
    
    // Word Document Generation - Using Apache POI (requires minSdk 26)
    // implementation("org.apache.poi:poi:5.2.3")
    // implementation("org.apache.poi:poi-ooxml:5.2.3")
    
    // Word Document Generation - Using HTML (compatible with all Android versions)
    // No additional dependencies needed
    
    // Testing
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    //Logica Gson para leer Json
    implementation("com.google.code.gson:gson:2.10.1")

    // Firebase Auth
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")

    // Google Sign-In + Drive (las que tienes en la imagen)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20240521-2.0.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.coil-kt:coil-compose:2.6.0")
}