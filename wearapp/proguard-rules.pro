# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-keepclassmembers class com.thewizrd.shared_resources.R$* {
    public static <fields>;
}
-dontwarn com.thewizrd.shared_resources.R$*

# SimpleXML
-keepclassmembers class com.thewizrd.shared_resources.locationdata.here.AutoCompleteQuery$** {
   <init>(...);
}
-keepclassmembers class com.thewizrd.shared_resources.weatherdata.metno.Weatherdata$** {
   <init>(...);
}
-keepclassmembers class com.thewizrd.shared_resources.weatherdata.metno.Astrodata$** {
   <init>(...);
}
-keepclassmembers class com.thewizrd.shared_resources.weatherdata.openweather.Location$** {
   <init>(...);
}
-keepclassmembers class com.thewizrd.shared_resources.weatherdata.weatheryahoo.AutoCompleteQuery$** {
   <init>(...);
}
-keep class * implements org.simpleframework.xml.convert.Converter {
   public *;
}

-keepattributes Root, Attribute, ElementList, Text, Element, Convert, *Annotation*

-keep class javax.xml.stream.** { *; }
-dontwarn javax.xml.stream.**

-keep class com.bea.xml.stream.** { *; }
-dontwarn com.bea.xml.stream.**

-keep public class org.simpleframework.** { *; }
-keep class org.simpleframework.xml.** { *; }
-keep class org.simpleframework.xml.core.** { *; }
-keep class org.simpleframework.xml.util.** { *; }

# Crashlytics
-keepattributes SourceFile,LineNumberTable        # Keep file names/line numbers
-keep public class * extends java.lang.Exception  # Keep custom exceptions (opt)

# To let Crashlytics automatically upload the ProGuard or DexGuard mapping file, remove this line from the config file
# -printmapping mapping.txt

# For faster builds with ProGuard, exclude Crashlytics. Add the following lines to your ProGuard config file:
# -keep class com.crashlytics.** { *; }
# -dontwarn com.crashlytics.**

# R8 Compatibility Rules
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
# Keep custom model classes
-keep class com.thewizrd.shared_resources.weatherdata.images.model.** { *; }
# Navigation
-keep public enum com.thewizrd.simpleweather.main.WeatherListType {
  public *;
}