# WearOS
-keepclassmembers class com.thewizrd.shared_resources.R$* {
    public static <fields>;
}
-dontwarn com.thewizrd.shared_resources.R$*

# R8 Compatibility Rules
-keepclassmembers,allowobfuscation class * {
  @com.squareup.moshi.Json <fields>;
}

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses