# WearOS
-keepclassmembers class com.thewizrd.shared_resources.R$* {
    public static <fields>;
}
-dontwarn com.thewizrd.shared_resources.R$*

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

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