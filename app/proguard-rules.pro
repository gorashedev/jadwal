# ===== Jadwal ProGuard Rules =====

# ----- Hilt -----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @javax.inject.Singleton class * { *; }

# ----- Room -----
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# ----- Retrofit + Gson -----
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ----- OkHttp -----
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ----- Coroutines -----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-dontwarn kotlinx.coroutines.**

# ----- Gemini AI -----
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.**

# ----- Firebase -----
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ----- Lottie -----
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ----- Compose -----
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ----- WorkManager -----
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ----- App Models (لا تشفّرها) -----
-keep class com.jadwal.domain.model.** { *; }
-keep class com.jadwal.data.local.entity.** { *; }
-keep class com.jadwal.data.remote.dto.** { *; }

# ----- General -----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
