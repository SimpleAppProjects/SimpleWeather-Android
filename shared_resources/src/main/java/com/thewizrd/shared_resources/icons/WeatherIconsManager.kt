package com.thewizrd.shared_resources.icons

import com.thewizrd.shared_resources.appLib
import java.util.*

class WeatherIconsManager internal constructor() : WeatherIconsProvider {
    private val _iconProviders = mutableMapOf<String, WeatherIconProvider>()
    private var _iconProvider: WeatherIconsProvider? = null
    val defaultIconProviders: Map<String, WeatherIconProvider>

    init {
        val defaultIconMap = mutableMapOf<String, WeatherIconProvider>()
        defaultIconMap.addIconProvider(WeatherIconsEFProvider())
        defaultIconMap.addIconProvider(WUndergroundIconsProvider())
        defaultIconMap.addIconProvider(WeatherIconicProvider())
        defaultIconProviders = Collections.unmodifiableMap(defaultIconMap)

        // Register default icon providers
        resetIconProviders()

        updateIconProvider()
    }

    fun updateIconProvider() {
        val settingsMgr = appLib.settingsManager
        val iconsSource = settingsMgr.getIconsProvider()
        _iconProvider = getIconProvider(iconsSource)
    }

    fun registerIconProvider(provider: WeatherIconProvider) {
        if (!_iconProviders.containsKey(provider.key)) {
            _iconProviders[provider.key] = provider
        }
    }

    fun getIconProvider(key: String): WeatherIconProvider {
        var provider = _iconProviders[key]
        if (provider == null) {
            // Can't find the provider for this key; fallback to default/first available
            if (_iconProviders.isNotEmpty()) {
                provider = _iconProviders.values.first()
            } else {
                registerIconProvider(WeatherIconsEFProvider().also { provider = it })
            }
        }
        return provider!!
    }

    val iconProviders: Map<String, WeatherIconProvider>
        get() = Collections.unmodifiableMap(_iconProviders)

    fun resetIconProviders() {
        _iconProviders.clear()
        _iconProviders.putAll(defaultIconProviders)
    }

    private fun MutableMap<String, WeatherIconProvider>.addIconProvider(
        provider: WeatherIconProvider
    ) {
        this[provider.key] = provider
    }

    /* WeatherIconsProvider proxy methods */
    val iconProvider: WeatherIconsProvider
        get() {
            if (_iconProvider == null) {
                updateIconProvider()
            }
            return _iconProvider!!
        }

    override fun isFontIcon(): Boolean {
        return _iconProvider!!.isFontIcon
    }

    override fun getWeatherIconResource(icon: String): Int {
        return _iconProvider!!.getWeatherIconResource(icon)
    }

    fun shouldUseMonochrome(): Boolean {
        val settingsMgr = appLib.settingsManager
        val iconsSource = settingsMgr.getIconsProvider()
        return shouldUseMonochrome(iconsSource)
    }

    fun shouldUseMonochrome(wip: String?): Boolean {
        return if (wip == null) {
            true
        } else {
            when (wip) {
                "wi-erik-flowers",
                "wui-ashley-jager",
                "w-iconic-jackd248",
                "pixeden-icons_set-weather" -> true

                "meteocons-basmilius",
                "wci_sliu_iconfinder" -> false

                else -> true
            }
        }
    }
}