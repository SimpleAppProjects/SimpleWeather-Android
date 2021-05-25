package com.thewizrd.shared_resources.weatherdata.model

fun Weather?.isNullOrInvalid(): Boolean {
    return this == null || !this.isValid
}