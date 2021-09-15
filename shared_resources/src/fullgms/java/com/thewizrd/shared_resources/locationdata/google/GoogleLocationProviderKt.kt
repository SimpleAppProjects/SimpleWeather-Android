package com.thewizrd.shared_resources.locationdata.google

import android.location.Geocoder
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.locationdata.locationiq.LocationIQProvider

fun getGoogleLocationProvider(): LocationProviderImpl {
    return if (Geocoder.isPresent()) {
        GoogleLocationProvider()
    } else {
        LocationIQProvider()
    }
}