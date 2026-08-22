package com.thewizrd.weather_api.google.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import okhttp3.Request
import java.security.MessageDigest

fun Request.Builder.addGoogleAuth(context: Context): Request.Builder {
    return this
        .addHeader("X-Android-Package", context.packageName)
        .addHeader("X-Android-Cert", context.getPackageSignature() ?: "")
}

fun Context.getGoogleAuthHeaders(): Map<String, String> {
    return mapOf(
        "X-Android-Package" to packageName,
        "X-Android-Cert" to (getPackageSignature() ?: "")
    )
}

private fun Context.getPackageSignature(): String? {
    val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info =
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        info.signingInfo?.apkContentsSigners?.first()
    } else {
        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        info.signatures?.first()
    } ?: return null

    val md = MessageDigest.getInstance("SHA1")
    return md.digest(signature.toByteArray()).let {
        Base16.encode(it).lowercase()
    }
}