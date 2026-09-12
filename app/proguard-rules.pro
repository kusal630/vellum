# Vellum release ProGuard / R8 rules.
# Referenced by the release build type in app/build.gradle.kts, where
# isMinifyEnabled = true (with isShrinkResources = true), so these are active
# on release builds; F-Droid applies them on its builders.

# --- Generic: keep annotations/signatures R8 needs for reflection-based libs ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Room (androidx.room:room-runtime 2.8.4) ---
# Generated AppDatabase_Impl plus entity/DAO metadata are accessed via reflection.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.DatabaseView class *
-keep class androidx.room.migration.** { *; }
-keep class com.vellum.notes.data.db.** { *; }
-dontwarn androidx.room.paging.**

# --- Input/Palm Rejection (critical for engine logic) ---
# PalmRejectionEngine, RestingHandTracker, PointerMotionState use reflection-free
# but R8 can still strip internal fields/methods without explicit keeps.
-keep class com.vellum.notes.input.** { *; }
-dontwarn com.vellum.notes.input.**

# --- Vosk (com.alphacephei:vosk-android) ---
# JNI bridge: native methods and the org.vosk classes must survive shrinking.
-keep class org.vosk.** { *; }
-keepclassmembers class org.vosk.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- Jetpack Compose / Material3 ---
# Compose compiler + AGP already emit the keeps Composables need; no blanket
# androidx.compose keep is required. Keep only true entry points referenced via
# reflection: @Keep markers, Parcelable/Serializable types used with
# rememberSaveable or navigation arguments, and custom Saver implementations.
-keep @androidx.annotation.Keep class *
-keepclassmembers @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclasseswithmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep class * implements androidx.compose.runtime.saveable.Saver { *; }

# --- kotlinx.serialization (plugin-managed serializers + JSON runtime) ---
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclasseswithmembers class com.vellum.notes.** {
    @kotlinx.serialization.Serializable <fields>;
}
-dontnote kotlinx.serialization.AnnotationsKt

# --- Coroutines / DataStore / WorkManager (service-loader + reflection) ---
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.datastore.**
-dontwarn androidx.work.**
# Preferences DataStore (used via preferencesDataStore delegate) needs no blanket
# keep. Keep only a custom Proto DataStore Serializer implementation, should one
# be added later (standard DataStore guidance).
-keep class * extends androidx.datastore.core.Serializer { *; }
