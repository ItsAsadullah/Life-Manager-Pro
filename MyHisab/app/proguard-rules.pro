# ========================================================
# MyHisab / Life Manager Pro - ProGuard / R8 Rules
# ========================================================

# 1. Line numbers for crash reporting / stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# 2. Kotlin Coroutines & Flow
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# 3. Room Database & Entities
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.hisabnikash.app.data.local.** { *; }

# 4. Firebase Authentication, Firestore & Google Identity
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**
-keep class com.google.android.libraries.identity.googleid.** { *; }

# 5. AndroidX Biometric
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# 6. Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# 7. WorkManager
-keep class androidx.work.** { *; }
-keep class com.hisabnikash.app.worker.** { *; }

# 8. App Models & Utilities
-keep class com.hisabnikash.app.utils.** { *; }
-keep class com.hisabnikash.app.sync.** { *; }
-keep class com.hisabnikash.app.auth.** { *; }
