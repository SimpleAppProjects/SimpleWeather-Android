package com.thewizrd.shared_resources

import android.content.Context
import android.content.Intent
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller

object GMSSecurityProvider {
    @JvmStatic
    fun installAsync(context: Context) {
        ProviderInstaller.installIfNeededAsync(context.applicationContext, object :
                ProviderInstaller.ProviderInstallListener {
            override fun onProviderInstalled() {
                // no-op
            }

            override fun onProviderInstallFailed(errorCode: Int, intent: Intent?) {
                val googleApiAvailability = GoogleApiAvailability.getInstance()
                if (googleApiAvailability.isUserResolvableError(errorCode)) {
                    // Prompt the user to install/update/enable Google Play services.
                    googleApiAvailability.showErrorNotification(context.applicationContext, errorCode)
                }
            }
        })
    }
}