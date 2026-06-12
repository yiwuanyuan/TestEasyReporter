# 氦检漏计算器 — ProGuard / R8 Rules
# ==========================================

# === Kotlin Serialization (Navigation type-safe routes) ===
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aerosun.heliumleakdetector.**$$serializer { *; }
-keepclassmembers class com.aerosun.heliumleakdetector.** {
    *** Companion;
}
-keepclasseswithmembers class com.aerosun.heliumleakdetector.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# === Room ===
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# === Hilt ===
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class com.aerosun.heliumleakdetector.core.di.** { *; }
-keep class * extends androidx.hilt.lifecycle.ViewModelAssistedFactory { *; }

# === Gson ===
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.aerosun.heliumleakdetector.data.local.entity.** { *; }
-keep class com.aerosun.heliumleakdetector.domain.model.** { *; }

# === Coroutines ===
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# === Compose ===
-keep class androidx.compose.** { *; }

# === Navigation ===
-keep class androidx.navigation.** { *; }

# === DataStore ===
-keep class androidx.datastore.** { *; }

# === DOCX 模板填充 (纯 Kotlin/ZIP) — 无需额外 keep 规则 ===

# === Keep app entry points ===
-keep class com.aerosun.heliumleakdetector.HeliumApp { *; }
-keep class com.aerosun.heliumleakdetector.MainActivity { *; }
