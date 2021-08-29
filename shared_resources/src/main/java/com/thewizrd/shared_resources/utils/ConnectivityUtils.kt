package com.thewizrd.shared_resources.utils

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.getSystemService
import com.thewizrd.shared_resources.SimpleLibrary

object ConnectivityUtils {
    @JvmStatic
    fun isNetworkConnected(): Boolean {
        val connMgr = SimpleLibrary.instance.appContext.getSystemService<ConnectivityManager>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connMgr?.getNetworkCapabilities(connMgr.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            connMgr?.activeNetworkInfo?.isAvailable == true && connMgr.activeNetworkInfo?.isConnected == true
        }
    }
}