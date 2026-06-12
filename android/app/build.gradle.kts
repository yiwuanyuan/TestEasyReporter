// App module build.gradle.kts — 氦检漏计算器
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")        // Kotlin 2.0+ Compose 编译器插件
    id("org.jetbrains.kotlin.plugin.serialization")   // Kotlin Serialization (类型安全导航)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")                    // KSP (Room + Hilt 注解处理)
}

android {
    namespace = "com.aerosun.heliumleakdetector"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aerosun.heliumleakdetector"
        minSdk = 26          // Android 8.0
        targetSdk = 35       // Android 15
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }


    // Room schema 导出目录（用于 AutoMigration 验证）
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    // Compose BOM (统一管理所有 Compose 库版本)
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose UI
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material3.adaptive)  // 平板自适应
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Activity & Lifecycle
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Data & Serialization
    implementation(libs.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager (设备校准提醒)
    implementation(libs.work.runtime.ktx)

    // DOCX 模板填充 — 纯 Kotlin/ZIP 实现，无需外部依赖

    // Testing
    testImplementation(libs.junit5.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
