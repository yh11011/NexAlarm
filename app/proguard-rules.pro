# ProGuard rules for NexAlarm
# This file specifies which classes and methods to keep and which to obfuscate
# for the Release build of the application.

# ============ General Rules ============

# Preserve line numbers for crash logs to be readable
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Keep all public API of the application
-keep public class com.nexalarm.app.** {
    public protected <fields>;
    public protected <methods>;
}

# ============ Android & Jetpack ============

# Keep Android classes
-keep class androidx.** { *; }
-keep class android.** { *; }

# Keep Jetpack Compose
-keep class androidx.compose.** { *; }

# Keep Material Design 3
-keep class com.google.android.material.** { *; }

# ============ Room Database ============

# Keep all Room entities and DAOs
-keep class com.nexalarm.app.data.model.** { *; }
-keep class com.nexalarm.app.data.database.** { *; }

# Keep Room annotations
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep TypeConverters
-keep class * implements androidx.room.TypeConverter

# ============ Serialization ============

# Keep Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    !static !transient <fields>;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============ ViewModels & Lifecycle ============

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# Keep lifecycle components
-keep class * extends androidx.lifecycle.LiveData { *; }
-keep class * extends androidx.lifecycle.MutableLiveData { *; }

# ============ Coroutines ============

# Keep coroutine classes
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }

# ============ Google Play Billing ============

# Keep billing client classes
-keep class com.android.billingclient.** { *; }
-keep class com.google.android.gms.** { *; }

# ============ Enums ============

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============ Native Methods ============

# Keep native method declarations
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============ Custom Classes ============

# Keep AlarmScheduler
-keep class com.nexalarm.app.util.AlarmScheduler { *; }

# Keep receivers and services
-keep class com.nexalarm.app.receiver.AlarmReceiver { *; }
-keep class com.nexalarm.app.receiver.BootReceiver { *; }
-keep class com.nexalarm.app.service.AlarmService { *; }
-keep class com.nexalarm.app.service.MeetingModeTileService { *; }

# Keep crash handler
-keep class com.nexalarm.app.util.CrashHandler { *; }

# Keep BroadcastReceiver subclasses
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep Service subclasses
-keep class * extends android.app.Service { *; }

# Keep Activity subclasses
-keep class * extends android.app.Activity { *; }

# ============ Optimization ============

# Optimization flags
-optimizationpasses 5
-dontusemixedcaseclassnames

# Keep classes for reflection (if needed)
-keepclasseswithmembers class * {
    *** *(...);
}

# Remove unused code and resources during optimization
-dontshrink
-dontoptimize
-verbose

# ============ Warnings ============

# Suppress warnings about unknown types that might come from external libraries
-dontwarn java.lang.invoke.*
-dontwarn androidx.**
-dontwarn com.google.**
-dontwarn android.**

# ============ Debug ============

# Log configuration (useful for debugging ProGuard issues)
# Uncomment to see detailed ProGuard logs
# -printconfiguration configuration.txt
# -printseeds seeds.txt
# -printusage usage.txt
# -printmapping mapping.txt
