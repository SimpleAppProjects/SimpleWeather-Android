package com.thewizrd.shared_resources.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object ConnectivityUtils {
    @JvmStatic
    fun isNetworkConnected(context: Context): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connMgr?.getNetworkCapabilities(connMgr.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            connMgr?.activeNetworkInfo?.isAvailable == true && connMgr.activeNetworkInfo?.isConnected == true
        }
    }
}