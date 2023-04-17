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

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Crashlytics
-keepattributes SourceFile,LineNumberTable        # Keep file names/line numbers
-keep public class * extends java.lang.Exception  # Keep custom exceptions (opt)

# To let Crashlytics automatically upload the ProGuard or DexGuard mapping file, remove this line from the config file
# -printmapping mapping.txt

# For faster builds with ProGuard, exclude Crashlytics. Add the following lines to your ProGuard config file:
# -keep class com.crashlytics.** { *; }
# -dontwarn com.crashlytics.**

# Navigation
-keep public enum com.thewizrd.simpleweather.main.WeatherListType {
  public *;
}

# RevenueCat
-keep class com.revenuecat.purchases.** { *; }

# Widgets
-keepclassmembers class com.thewizrd.simpleweather.R$id {
    public static int forecast?;
    public static int forecast?_date;
    public static int forecast?_icon;
    public static int forecast?_hi;
    public static int forecast?_divider;
    public static int forecast?_lo;

    public static int hrforecast?;
    public static int hrforecast?_date;
    public static int hrforecast?_icon;
    public static int hrforecast?_hi;
    public static int hrforecast?_divider;
    public static int hrforecast?_lo;

    public static int location?;
    public static int location?_name;
}

# Keep custom model classes (ImageData)
# Needed for (de)serialization in Firestore
-keep class com.thewizrd.simpleweather.images.model.** { *; }

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

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE