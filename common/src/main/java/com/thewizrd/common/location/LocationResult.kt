package com.thewizrd.common.location

import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.shared_resources.locationdata.LocationData

sealed interface LocationResult {
    val data: LocationData?
    val locationChanged: Boolean

    data class Changed(
        override val data: LocationData,
        override val locationChanged: Boolean = true
    ) : LocationResult

    data class ChangedInvalid(
        override val data: LocationData?,
        override val locationChanged: Boolean = true
    ) : LocationResult

    data class NotChanged(
        override val data: LocationData?,
        override val locationChanged: Boolean = false
    ) : LocationResult

    data class PermissionDenied(
        override val data: LocationData? = null,
        override val locationChanged: Boolean = false
    ) : LocationResult

    data class Error(
        override val data: LocationData? = null,
        override val locationChanged: Boolean = false,
        val errorMessage: ErrorMessage
    ) : LocationResult
}