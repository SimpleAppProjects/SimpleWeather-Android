package com.thewizrd.shared_resources.locationdata.google

import com.thewizrd.shared_resources.locationdata.LocationProviderImpl

fun getGoogleLocationProvider(): LocationProviderImpl {
    return GoogleLocationProvider()
}