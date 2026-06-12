// Helium Leak Detector - Android Project Settings
// 氦检漏计算器 - Android 项目设置

pluginManagement {
    repositories {
        google()                // AndroidX, KSP, Hilt, AGP
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "HeliumLeakDetector"

include(":app")
