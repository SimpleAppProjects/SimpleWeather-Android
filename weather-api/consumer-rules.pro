# Moshi
# Keep name of @JsonClass type to lookup generated adapter
-keepnames @com.squareup.moshi.JsonClass class *

-keepclassmembers,allowobfuscation class * {
  @com.squareup.moshi.Json <fields>;
}

# Keep generated JsonAdapter for @JsonClass type
-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter {
    <init>();
    <init>(...);
}

# JWT
-keepattributes InnerClasses

-keep class io.jsonwebtoken.** { *; }
-keepnames class io.jsonwebtoken.* { *; }
-keepnames interface io.jsonwebtoken.* { *; }

-keep class org.bouncycastle.** { *; }
-keepnames class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**