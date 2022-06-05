package com.thewizrd.shared_resources.weatherdata.auth

abstract class ProviderKey {
    abstract fun fromString(input: String)
    abstract override fun toString(): String
}