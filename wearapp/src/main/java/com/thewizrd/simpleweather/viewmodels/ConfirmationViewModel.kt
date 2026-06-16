package com.thewizrd.simpleweather.viewmodels

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import androidx.wear.R as wearRes

class ConfirmationViewModel : ViewModel() {
    private val _confirmationEventsFlow = MutableStateFlow<ConfirmationData?>(null)

    val confirmationEventsFlow = _confirmationEventsFlow
        .distinctUntilChanged(areEquivalent = { old, new -> old == new })
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    fun showConfirmation(data: ConfirmationData) {
        _confirmationEventsFlow.update { data }
    }

    fun showSuccess(message: String? = null) {
        _confirmationEventsFlow.update {
            ConfirmationData(
                animatedVectorResId = wearRes.drawable.confirmation_animation,
                confirmationType = ConfirmationType.Success,
                message = message
            )
        }
    }

    fun showFailure(message: String? = null) {
        _confirmationEventsFlow.update {
            ConfirmationData(
                animatedVectorResId = wearRes.drawable.failure_animation,
                confirmationType = ConfirmationType.Failure,
                message = message
            )
        }
    }

    fun showOpenOnPhone(message: String? = null) {
        _confirmationEventsFlow.update {
            ConfirmationData(
                animatedVectorResId = wearRes.drawable.open_on_phone_animation,
                confirmationType = ConfirmationType.OpenOnPhone,
                message = message
            )
        }
    }

    fun clearFlow() {
        _confirmationEventsFlow.update { null }
    }
}

@JsonClass(generateAdapter = true)
data class ConfirmationData(
    val message: String? = null,
    @DrawableRes val iconResId: Int? = null,
    @DrawableRes val animatedVectorResId: Int? = null,
    val confirmationType: ConfirmationType = ConfirmationType.Custom,
    val durationMs: Long = ConfirmationDialogDefaults.DurationMillis
)

enum class ConfirmationType {
    Success, Failure, OpenOnPhone, Custom
}