plugins {
    id("org.jetbrains.kotlin.android")
    id("com.android.application")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.progettoprogrammazionemobile"
    compileSdk = 34

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    defaultConfig {
        applicationId = "com.example.progettoprogrammazionemobile"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xinline-classes"
        freeCompilerArgs += "-Xenable-break-continue-in-lambdas"

    }

}

dependencies {
    //for authentication
    implementation("com.firebaseui:firebase-ui-auth:8.0.1")
    implementation("com.google.android.gms:play-services-auth:20.3.0")
    implementation ("com.google.dagger:dagger:2.40.5")
    implementation("com.google.firebase:firebase-messaging-ktx:24.0.0")
    val lifecycle_version = "2.7.0"
    val activity_version = "1.8.2"
    implementation ("com.google.firebase:firebase-auth:21.0.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.activity:activity-ktx:$activity_version")
    implementation("com.google.firebase:firebase-firestore")
    //implementation ("com.google.firebase:firebase-firestore-ktx:23.0.0")
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    val nav_version = "2.7.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")
    implementation ("com.github.Pygmalion69:Gauge:1.5.2")
    implementation ("com.github.Gruzer:simple-gauge-android:0.3.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation ("com.google.firebase:firebase-firestore-ktx:24.1.2")
    implementation ("com.google.firebase:firebase-auth-ktx:21.1.0")


    implementation ("androidx.viewpager2:viewpager2:1.0.0")

    implementation ("com.google.android.material:material:1.7.0")
    // OkHttp e JSON parsing
    implementation ("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
    implementation ("org.json:json:20210307")

    implementation ("androidx.recyclerview:recyclerview:1.3.0")

    // add the dependency for the Google AI client SDK for Android
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    implementation ("com.google.android.gms:play-services-tasks:18.0.2")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation ("com.squareup.retrofit2:converter-scalars:2.9.0")

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation ("com.google.code.gson:gson:2.8.9")

    implementation ("androidx.fragment:fragment-ktx:1.3.6")


    implementation(kotlin("stdlib"))
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.moshi:moshi:1.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.12.0")





    /* add the dependency for the Vertex AI SDK for Android
    implementation("com.google.firebase:firebase-vertexai:16.0.0-alpha02")

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")



    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
    implementation ("com.google.firebase:firebase-auth:21.0.3")
    implementation ("com.google.firebase:firebase-messaging:23.0.0")
    implementation ("com.google.firebase:firebase-firestore:24.0.0")
    implementation ("com.google.firebase:firebase-vertex-ai:1.0.0")

    */

}