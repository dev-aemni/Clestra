# Hardened R8 / ProGuard Configuration for Clestra
-optimizationpasses 5
-allowaccessmodification
-overloadaggressively
-repackageclasses 'o'
-renamesourcefileattribute SourceFile
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses

# Strip all android.util.Log calls in release APKs
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep Retrofit service interfaces & parameter generics
-keepattributes MethodParameters
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class * extends retrofit2.Call
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# Keep Moshi DTO adapters
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keepclassmembers class * extends com.squareup.moshi.JsonAdapter {
    <init>(...);
}
-keep @com.squareup.moshi.JsonClass class * { *; }

# Keep Room database & entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }

# Suppress harmless R8 warnings from OkHttp/Kotlin coroutines internals
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
