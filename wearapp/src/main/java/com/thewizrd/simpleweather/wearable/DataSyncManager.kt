package com.thewizrd.simpleweather.wearable

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.util.ObjectsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import com.thewizrd.shared_resources.icons.WeatherIconsProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.shared_resources.wearable.WearableSettings
import com.thewizrd.shared_resources.weatherdata.Forecasts
import com.thewizrd.shared_resources.weatherdata.HourlyForecasts
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker.Companion.requestWidgetUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ExecutionException

object DataSyncManager {
    private const val TAG = "DataSyncManager"

    suspend fun updateSettings(context: Context, dataMap: DataMap?) = withContext(Dispatchers.Default) {
        val appContext = context.applicationContext
        val settingsMgr = SettingsManager(appContext)

        if (dataMap != null && !dataMap.isEmpty) {
            val localBroadcastMgr = LocalBroadcastManager.getInstance(appContext)

            val updateTimeMillis = dataMap.getLong(WearableSettings.KEY_UPDATETIME)

            if (updateTimeMillis != getSettingsUpdateTime(appContext)) {
                val API = dataMap.getString(WearableSettings.KEY_API, "")
                val API_KEY = dataMap.getString(WearableSettings.KEY_APIKEY, "")
                val keyVerified = dataMap.getBoolean(WearableSettings.KEY_APIKEY_VERIFIED, false)
                if (!API.isNullOrBlank()) {
                    settingsMgr.setAPI(API)
                    if (WeatherManager.isKeyRequired(API)) {
                        settingsMgr.setAPIKEY(API_KEY)
                        settingsMgr.setKeyVerified(false)
                    } else {
                        settingsMgr.setAPIKEY("")
                        settingsMgr.setKeyVerified(false)
                    }
                }

                settingsMgr.setFollowGPS(dataMap.getBoolean(WearableSettings.KEY_FOLLOWGPS, false))

                val unitMap = dataMap.getDataMap(WearableSettings.KEY_UNITS)
                val oldUnits = settingsMgr.getUnitString()
                if (unitMap != null) {
                    settingsMgr.setTemperatureUnit(unitMap.getString(WearableSettings.KEY_TEMPUNIT, Units.FAHRENHEIT))
                    settingsMgr.setSpeedUnit(unitMap.getString(WearableSettings.KEY_SPEEDUNIT, Units.MILES_PER_HOUR))
                    settingsMgr.setDistanceUnit(unitMap.getString(WearableSettings.KEY_DISTANCEUNIT, Units.MILES))
                    settingsMgr.setPressureUnit(unitMap.getString(WearableSettings.KEY_PRESSUREUNIT, Units.INHG))
                    settingsMgr.setPrecipitationUnit(unitMap.getString(WearableSettings.KEY_PRECIPITATIONUNIT, Units.INCHES))
                } else {
                    settingsMgr.setDefaultUnits(dataMap.getString(WearableSettings.KEY_TEMPUNIT, Units.FAHRENHEIT))
                }
                val newUnits = settingsMgr.getUnitString()

                if (!ObjectsCompat.equals(oldUnits, newUnits)) {
                    localBroadcastMgr.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT))
                }

                LocaleUtils.setLocaleCode(dataMap.getString(WearableSettings.KEY_LANGUAGE, ""))

                val oldIcons = settingsMgr.getIconsProvider()
                settingsMgr.setIconsProvider(dataMap.getString(WearableSettings.KEY_ICONPROVIDER, WeatherIconsProvider.KEY))
                val newIcons = settingsMgr.getIconsProvider()
                if (!ObjectsCompat.equals(oldIcons, newIcons)) {
                    // Update tiles and complications
                    requestWidgetUpdate(appContext)
                }

                setSettingsUpdateTime(appContext, updateTimeMillis)

                Timber.tag(TAG).d("Updated settings")
            }

            // Send callback to receiver
            localBroadcastMgr.sendBroadcast(Intent(WearableHelper.SettingsPath))
        }
    }

    suspend fun updateLocation(context: Context, dataMap: DataMap?) = withContext(Dispatchers.Default) {
        val appContext = context.applicationContext
        val settingsMgr = SettingsManager(appContext)

        if (dataMap != null && !dataMap.isEmpty) {
            val locationJSON = dataMap.getString(WearableSettings.KEY_LOCATIONDATA, "")
            if (!locationJSON.isNullOrBlank()) {
                val locationData = JSONParser.deserializer(locationJSON, LocationData::class.java)

                if (locationData != null) {
                    val updateTimeMillis = dataMap.getLong(WearableSettings.KEY_UPDATETIME)

                    if (updateTimeMillis != getLocationDataUpdateTime(appContext) ||
                            locationData != settingsMgr.getHomeData()) {
                        settingsMgr.saveHomeData(locationData)
                    }

                    setLocationDataUpdateTime(appContext, updateTimeMillis)

                    Timber.tag(TAG).d("updateLocation: Updated location data")

                    // Send callback to receiver
                    LocalBroadcastManager.getInstance(appContext).sendBroadcast(
                            Intent(WearableHelper.LocationPath))
                }
            }
        }
    }

    suspend fun updateWeather(context: Context, dataMap: DataMap?) = withContext(Dispatchers.Default) {
        val appContext = context.applicationContext
        val settingsMgr = SettingsManager(appContext)

        if (dataMap != null && !dataMap.isEmpty) {
            val updateTimeMillis = dataMap.getLong(WearableSettings.KEY_UPDATETIME)
            // Check if data actually exists to force an update
            var dataExists = false
            val homeData = settingsMgr.getHomeData()
            if (homeData != null) {
                dataExists = withContext(Dispatchers.IO) {
                    settingsMgr.getWeatherDAO().getWeatherDataCountByKey(homeData.query) > 0
                }
            }
            if (updateTimeMillis != getWeatherUpdateTime(appContext) || !dataExists) {
                val weatherAsset = dataMap.getAsset(WearableSettings.KEY_WEATHERDATA)
                if (weatherAsset != null) {
                    try {
                        withContext(Dispatchers.IO) {
                            val fd = Wearable.getDataClient(appContext).getFdForAsset(weatherAsset).await()
                            fd.inputStream.use { inputStream ->
                                val weatherData = JSONParser.deserializer(inputStream, Weather::class.java)
                                if (weatherData?.isValid == true) {
                                    settingsMgr.saveWeatherAlerts(homeData, weatherData.weatherAlerts)
                                    settingsMgr.saveWeatherData(weatherData)
                                    settingsMgr.saveWeatherForecasts(Forecasts(weatherData.query, weatherData.forecast, weatherData.txtForecast))
                                    settingsMgr.saveWeatherForecasts(weatherData.query, weatherData.hrForecast?.map { input -> HourlyForecasts(weatherData.query, input) })
                                    settingsMgr.setUpdateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(updateTimeMillis), ZoneOffset.UTC))
                                    setWeatherUpdateTime(appContext, updateTimeMillis)

                                    Timber.tag(TAG).d("Updated weather data")

                                    requestWidgetUpdate(appContext)
                                } else {
                                    Timber.tag(TAG).d("Weather data invalid")
                                }
                            }
                        }
                    } catch (e: ExecutionException) {
                        Logger.writeLine(Log.ERROR, e)
                    } catch (e: InterruptedException) {
                        Logger.writeLine(Log.ERROR, e)
                    } catch (e: IOException) {
                        Logger.writeLine(Log.ERROR, e)
                    }
                } else {
                    Timber.tag(TAG).d("updateWeather: weather data missing")
                }
            }

            // Send callback to receiver
            LocalBroadcastManager.getInstance(appContext).sendBroadcast(
                    Intent(WearableHelper.WeatherPath))
        }
    }

    private fun getSettingsUpdateTime(context: Context): Long {
        val prefs = context.applicationContext.getSharedPreferences("datasync", Context.MODE_PRIVATE)
        return prefs.getLong("settings_updatetime", 0)
    }

    private fun getLocationDataUpdateTime(context: Context): Long {
        val prefs = context.applicationContext.getSharedPreferences("datasync", Context.MODE_PRIVATE)
        return prefs.getLong("location_updatetime", 0)
    }

    private fun getWeatherUpdateTime(context: Context): Long {
        val prefs = context.applicationContext.getSharedPreferences("datasync", Context.MODE_PRIVATE)
        return prefs.getLong("weather_updatetime", 0)
    }

    private fun setSettingsUpdateTime(context: Context, value: Long) {
        val prefs = context.applicationContext.getSharedPreferences("datasync", Context.MODE_PRIVATE)
        prefs.edit().putLong("settings_updatetime", value).apply()
    }

    private fun setLocationDataUpdateTime(context: Context, value: Long) {
        val prefs = context.applicationContext.getSharedPreferences("datasync", Context.MODE_PRIVATE)
        prefs.edit().putLong("location_updatetime", value).apply()
    }

    private fun setWeatherUpdateTime(context: Context, value: Long) {
        val prefs = context.applicationContext.getSharedPreferences("datasync", Context.MODE_PRIVATE)
        prefs.edit().putLong("weather_updatetime", value).apply()
    }
}