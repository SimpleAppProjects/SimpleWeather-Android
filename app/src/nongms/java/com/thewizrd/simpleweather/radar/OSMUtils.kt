package com.thewizrd.simpleweather.radar

import android.content.Context
import android.content.pm.PackageInfo
import com.thewizrd.shared_resources.Constants
import com.thewizrd.simpleweather.BuildConfig
import org.osmdroid.config.Configuration
import java.io.File

private var mapInitialized: Boolean = false

fun Context.initializeMap() {
    if (!mapInitialized) {
        val version = runCatching {
            val packageInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            String.format("v%s", packageInfo.versionName)
        }.getOrDefault("")

        Configuration.getInstance().run {
            userAgentValue =
                String.format("SimpleWeather (${Constants.SUPPORT_EMAIL_ADDRESS}) %s", version)
            osmdroidTileCache = File(cacheDir, "tiles")
            osmdroidBasePath = File(noBackupFilesDir, "osmdroid")
            isDebugMapTileDownloader = BuildConfig.DEBUG
        }

        mapInitialized = true
    }
}