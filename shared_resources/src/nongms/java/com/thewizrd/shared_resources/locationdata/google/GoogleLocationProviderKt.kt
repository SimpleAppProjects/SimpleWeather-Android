package com.thewizrd.shared_resources.locationdata.google

import android.location.Geocoder
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl

fun getGoogleLocationProvider(): LocationProviderImpl {
    return AndroidLocationProvider()
}