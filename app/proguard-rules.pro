# UniDownloader ProGuard / R8 Optimization Rules

# 1. YoutubeDL-Android & FFmpeg JNI bindings
-keep class com.yausername.youtubedl_android.** { *; }
-keepclassmembers class com.yausername.youtubedl_android.** { *; }
-keep class io.github.junkfood02.youtubedl_android.** { *; }
-keepclassmembers class io.github.junkfood02.youtubedl_android.** { *; }

# 2. Data Models & JSON Serialization
-keep class com.unidownloader.app.data.model.** { *; }
-keepclassmembers class com.unidownloader.app.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# 3. OkHttp & Okio Network Optimizations
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# 4. Kotlin Coroutines & Jetpack Compose
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView { *; }

# 5. Coil Image Loading
-dontwarn coil.**
-keep class coil.** { *; }
