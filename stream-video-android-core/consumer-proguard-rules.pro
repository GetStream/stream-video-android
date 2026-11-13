## Stream Video Android Core Proguard Rules

# SFU protobufs are decoded through Wire's generated ADAPTER fields, which
# retrofit2-wire-converter already keeps. Do not keep the whole package.

-keep class com.squareup.moshi.JsonReader
-keep class com.squareup.moshi.JsonAdapter
# Video parses JSON through Moshi codegen adapters. It does not ship kotlin-reflect.

## Moshi model classes
# Generated models use @JsonClass(generateAdapter = true). Moshi loads
# ClassNameJsonAdapter by name, and its own rules keep those adapters.
# Do not keep the model package; that retain was only for reflection.

## Kotlin serialized classes
# kotlinx-serialization already keeps its generated serializers. This rule only
# preserves Stream's own models, so a host app that reflects on them still works.
# It must not match every @Serializable class in the host app.
-keep @kotlinx.serialization.Serializable class io.getstream.video.** { *; }

## Retrofit (https://github.com/square/retrofit/blob/master/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro)

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# With R8 full mode generic signatures are stripped for classes that are not kept.
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Prevent R8 from stripping notification-handling classes.
-keep class io.getstream.video.android.core.notifications.internal.VideoPushDelegate { *; }
-keep class io.getstream.android.push.delegate.PushDelegate { *; }
-keep class io.getstream.android.push.delegate.AndroidPushDelegateProvider { *; }
-keep class io.getstream.android.push.permissions.PushPermissionsInitializer { *; }

# defines consumer proguard rules for org.webrt
-keep class org.webrtc.** { *; }
# Keep the jni_zero library (required by modern WebRTC)
-keep class org.jni_zero.** { *; }