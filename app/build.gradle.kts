plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin.get()
}

repositories {
    google()
    mavenCentral()
    maven("https://api.xposed.info/")
    maven("https://jitpack.io")
}

android {
    namespace = "com.shiinasign"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shiinasign"
        minSdk = 24
        targetSdk = 36
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

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // Xposed API 本地 jar 依赖
    implementation(files("libs/XposedBridgeAPI-82.jar"))
    // Gson 依赖
    implementation("com.google.code.gson:gson:2.13.2")

    // Protobuf 依赖
    implementation("com.google.protobuf:protobuf-java:4.32.1")

    // kotlinx.serialization 依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.9.0")

    // OkHttp 依赖
    implementation("com.squareup.okhttp3:okhttp:5.1.0")

    // kotlinx-io 依赖
    implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:0.1.16")

    // MaterialEditText 依赖
    implementation("com.rengwuxian.materialedittext:library:2.1.4")

    // EzXHelper 依赖 (3.x 拆分模块)
    implementation("io.github.kyuubiran.ezxhelper:core:3.0.1")
    implementation("io.github.kyuubiran.ezxhelper:xposed-api-82:3.0.1")
    implementation("io.github.kyuubiran.ezxhelper:android-utils:3.0.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.6")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
