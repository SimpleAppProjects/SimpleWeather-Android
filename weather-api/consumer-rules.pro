# R8 Compatibility Rules
-keepclassmembers,allowobfuscation class * {
  @com.squareup.moshi.Json <fields>;
}