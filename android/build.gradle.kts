// Top-level build.gradle.kts — 氦检漏计算器
// 根构建文件使用直接 plugin ID（不需要 version catalog alias）
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false
    id("com.google.dagger.hilt.android") version "2.53" apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" apply false
}
