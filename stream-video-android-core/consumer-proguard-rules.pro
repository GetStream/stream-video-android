## Stream Video Android Core Proguard Rules

# Wire protocol buffer model classes
-keep class stream.video.sfu.** { *; }

-keep class com.squareup.moshi.JsonReader
-keep class com.squareup.moshi.JsonAdapter
-keep class kotlin.reflect.jvm.internal.* { *; }

## Moshi model classes
-keep class org.openapitools.client.** { *; }