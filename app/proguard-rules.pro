# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\bryan\AppData\Local\Android\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

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