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

-keep class android.support.v7.widget.** { *; }
-dontwarn android.support.v7.widget.**

-keep class android.support.v7.internal.widget.** { *; }
-dontwarn android.support.v7.internal.widget.**

-keep class android.support.v4.widget.** { *; }
-dontwarn android.support.v4.widget.**

-keep class android.support.v4.view.** { *; }
-dontwarn android.support.v4.view.**

-keep class android.support.v7.view.** { *; }
-dontwarn android.support.v7.view.**

-keep class android.support.design.widget.** { *; }
-dontwarn android.support.design.widget.**

-keep class android.support.design.internal.** { *; }
-dontwarn android.support.design.internal.**

-keep class android.support.wear.widget.BoxInsetLayout$LayoutParams
-keepclassmembers class android.support.wear.widget.BoxInsetLayout$LayoutParams {
   <init>(...);
   <init>(...);
   <init>(...);
   <init>(...);
   <init>(...);
   <init>(...);
   <init>(...);
   <init>(...);
}

-keep class android.support.wear.widget.BoxInsetLayout
-keepclassmembers class android.support.wear.widget.BoxInsetLayout {
   <init>(...);
   <init>(...);
   <init>(...);
   *** getInsets(...);
   *** isRound(...);
}
