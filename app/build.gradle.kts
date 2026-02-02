plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose) // KSPを適用
    id("com.google.dagger.hilt.android") // 追加
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.komorebi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.Komorebi"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            // もし追加の引数がある場合はここにも書けます
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kapt {
    correctErrorTypes = true
}

// app/build.gradle.kts
configurations.all {
    resolutionStrategy {
        // Kotlin 2.x のメタデータを正しく読み取れるバージョンに強制
        force("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
        force("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
        force("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
    }
}

// dependencies ブロックを以下のように書き換えてください
dependencies {
    // 1. Compose BOM を最新に近いバージョンに更新 (ここが最重要)
    // 2023.10.01 だと Tv-Foundation 1.0.0-alpha11 と互換性がありません
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))

    // 2. 各ライブラリの指定 (バージョンは BOM が管理するので書かない)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation") // 追加
    implementation("androidx.compose.ui:ui-graphics")       // 追加

    // --- TV用ライブラリ ---
    // これらは BOM に含まれないため、バージョンを固定します
    implementation("androidx.tv:tv-material:1.0.0")
    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")

    // --- Hilt ---
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.dagger:hilt-android:2.54")
    kapt("com.google.dagger:hilt-compiler:2.54")

    // --- Retrofit ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // --- Room (KSPへの移行を推奨) ---
    val room_version = "2.7.0-alpha11" // 2.7.0より安定している2.6.1を一旦推奨
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.room:room-paging:$room_version")
    ksp("androidx.room:room-compiler:$room_version") // kaptからkspへ

    // --- Media3 ---
    val media3_version = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-ui:$media3_version")
    implementation("androidx.media3:media3-common:$media3_version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3_version")

    // --- その他 ---
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.paging:paging-runtime:3.3.0")
    implementation("androidx.paging:paging-compose:3.3.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.android.material:material:1.12.0")
}