package com.thewizrd.simpleweather.wearable

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.*
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.shared_resources.wearable.WearableSettings
import com.thewizrd.shared_resources.weatherdata.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.time.Clock
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutionException

class WearableWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private val mContext = context.applicationContext

    companion object {
        private const val TAG = "WearableWorker"

        // Actions
        private const val KEY_ACTION = "action"
        private const val KEY_URGENTREQUEST = "urgent"
        const val ACTION_SENDUPDATE = "SimpleWeather.Droid.action.SEND_UPDATE"
        const val ACTION_SENDSETTINGSUPDATE = "SimpleWeather.Droid.action.SEND_SETTINGS_UPDATE"
        const val ACTION_SENDLOCATIONUPDATE = "SimpleWeather.Droid.action.SEND_LOCATION_UPDATE"
        const val ACTION_SENDWEATHERUPDATE = "SimpleWeather.Droid.action.SEND_WEATHER_UPDATE"

        @JvmStatic
        @JvmOverloads
        fun enqueueAction(context: Context, intentAction: String, urgent: Boolean = true) {
            val context = context.applicationContext

            when (intentAction) {
                ACTION_SENDUPDATE,
                ACTION_SENDSETTINGSUPDATE,
                ACTION_SENDLOCATIONUPDATE,
                ACTION_SENDWEATHERUPDATE -> {
                    startWork(context, intentAction, urgent)
                }
            }
        }

        private fun startWork(context: Context, intentAction: String, urgent: Boolean) {
            val context = context.applicationContext

            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val updateRequest = OneTimeWorkRequest.Builder(WearableWorker::class.java)
                    .setInputData(
                            Data.Builder()
                                    .putString(KEY_ACTION, intentAction)
                                    .putBoolean(KEY_URGENTREQUEST, urgent)
                                    .build()
                    )
                    .build()

            WorkManager.getInstance(context)
                    .enqueueUniqueWork(String.format(Locale.ROOT, "%s:%s_oneTime", TAG, intentAction), ExistingWorkPolicy.APPEND_OR_REPLACE, updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)
        }
    }

    override suspend fun doWork(): Result {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG)

        val intentAction = inputData.getString(KEY_ACTION)
        val urgent = inputData.getBoolean(KEY_URGENTREQUEST, true)

        Logger.writeLine(Log.INFO, "%s: Action: %s", TAG, intentAction)

        // Don't send anything unless we're setup
        if (!Settings.isWeatherLoaded())
            return Result.success()

        val mWearNodesWithApp = findWearDevicesWithApp()
        if (!mWearNodesWithApp.isEmpty()) {
            if (ACTION_SENDUPDATE == intentAction) {
                createSettingsDataRequest(urgent)
                createLocationDataRequest(urgent)
                createWeatherDataRequest(urgent)
            } else if (ACTION_SENDSETTINGSUPDATE == intentAction) {
                createSettingsDataRequest(urgent)
            } else if (ACTION_SENDLOCATIONUPDATE == intentAction) {
                createLocationDataRequest(urgent)
            } else if (ACTION_SENDWEATHERUPDATE == intentAction) {
                createWeatherDataRequest(urgent)
            }
        }

        return Result.success()
    }

    /* Wearable Functions */
    private suspend fun findWearDevicesWithApp(): Collection<Node> = withContext(Dispatchers.IO) {
        val capabilityInfo = try {
            Wearable.getCapabilityClient(mContext)
                    .getCapability(WearableHelper.CAPABILITY_WEAR_APP, CapabilityClient.FILTER_ALL)
                    .await()
        } catch (e: ExecutionException) {
            if (e.cause is ApiException) {
                val apiException = e.cause as ApiException
                // Ignore this error
                if (apiException.statusCode != WearableStatusCodes.API_NOT_CONNECTED) {
                    Logger.writeLine(Log.ERROR, e)
                }
            } else {
                Logger.writeLine(Log.ERROR, e)
            }
            null
        } catch (e: InterruptedException) {
            if (e.cause is ApiException) {
                val apiException = e.cause as ApiException
                if (apiException.statusCode != WearableStatusCodes.API_NOT_CONNECTED) {
                    Logger.writeLine(Log.ERROR, e)
                }
            } else {
                Logger.writeLine(Log.ERROR, e)
            }
            null
        }

        return@withContext if (capabilityInfo != null) {
            capabilityInfo.nodes
        } else {
            emptySet()
        }
    }

    private suspend fun createSettingsDataRequest(urgent: Boolean) {
        withContext(Dispatchers.IO) {
            val mapRequest = PutDataMapRequest.create(WearableHelper.SettingsPath)
            mapRequest.dataMap.putString(WearableSettings.KEY_API, Settings.getAPI())
            mapRequest.dataMap.putString(WearableSettings.KEY_APIKEY, Settings.getAPIKEY())
            mapRequest.dataMap.putBoolean(WearableSettings.KEY_APIKEY_VERIFIED, Settings.isKeyVerified())
            mapRequest.dataMap.putBoolean(WearableSettings.KEY_FOLLOWGPS, Settings.useFollowGPS())
            mapRequest.dataMap.putString(WearableSettings.KEY_TEMPUNIT, Settings.getTemperatureUnit())

            val unitMap = DataMap()
            unitMap.putString(WearableSettings.KEY_TEMPUNIT, Settings.getTemperatureUnit())
            unitMap.putString(WearableSettings.KEY_SPEEDUNIT, Settings.getSpeedUnit())
            unitMap.putString(WearableSettings.KEY_DISTANCEUNIT, Settings.getDistanceUnit())
            unitMap.putString(WearableSettings.KEY_PRESSUREUNIT, Settings.getPressureUnit())
            unitMap.putString(WearableSettings.KEY_PRECIPITATIONUNIT, Settings.getPrecipitationUnit())
            mapRequest.dataMap.putDataMap(WearableSettings.KEY_UNITS, unitMap)

            mapRequest.dataMap.putString(WearableSettings.KEY_LANGUAGE, LocaleUtils.getLocaleCode())
            mapRequest.dataMap.putString(WearableSettings.KEY_ICONPROVIDER, Settings.getIconsProvider())
            mapRequest.dataMap.putLong(WearableSettings.KEY_UPDATETIME, Instant.now(Clock.systemUTC()).toEpochMilli())
            val request = mapRequest.asPutDataRequest()
            if (urgent) request.setUrgent()
            try {
                val client = Wearable.getDataClient(mContext)
                client.deleteDataItems(mapRequest.uri).await()
                client.putDataItem(request).await()
            } catch (e: ExecutionException) {
                Logger.writeLine(Log.ERROR, e)
            } catch (e: InterruptedException) {
                Logger.writeLine(Log.ERROR, e)
            }

            Logger.writeLine(Log.INFO, "%s: createSettingsDataRequest(): urgent: %s", TAG, java.lang.Boolean.toString(urgent))
        }
    }

    private suspend fun createLocationDataRequest(urgent: Boolean) {
        withContext(Dispatchers.IO) {
            val mapRequest = PutDataMapRequest.create(WearableHelper.LocationPath)
            val homeData = Settings.getHomeData()
            mapRequest.dataMap.putString(WearableSettings.KEY_LOCATIONDATA, JSONParser.serializer(homeData, LocationData::class.java))
            mapRequest.dataMap.putLong(WearableSettings.KEY_UPDATETIME, Instant.now(Clock.systemUTC()).toEpochMilli())
            val request = mapRequest.asPutDataRequest()
            if (urgent) request.setUrgent()
            try {
                val client = Wearable.getDataClient(mContext)
                client.deleteDataItems(mapRequest.uri).await()
                client.putDataItem(request).await()
            } catch (e: ExecutionException) {
                Logger.writeLine(Log.ERROR, e)
            } catch (e: InterruptedException) {
                Logger.writeLine(Log.ERROR, e)
            }

            Logger.writeLine(Log.INFO, "%s: createLocationDataRequest(): urgent: %s", TAG, java.lang.Boolean.toString(urgent))
        }
    }

    private suspend fun createWeatherDataRequest(urgent: Boolean) {
        withContext(Dispatchers.IO) {
            val mapRequest = PutDataMapRequest.create(WearableHelper.WeatherPath)
            val homeData = Settings.getHomeData()
            val weatherData = Settings.getWeatherData(homeData.query)
            val alertData = Settings.getWeatherAlertData(homeData.query)
            val forecasts = Settings.getWeatherForecastData(homeData.query)
            val hrForecasts = Settings.getHourlyWeatherForecastData(homeData.query)

            if (weatherData != null) {
                weatherData.forecast = forecasts!!.forecast
                weatherData.hrForecast = hrForecasts
                weatherData.txtForecast = forecasts.txtForecast
                weatherData.weatherAlerts = alertData
                mapRequest.dataMap.putAsset(WearableSettings.KEY_WEATHERDATA, Asset.createFromBytes(JSONParser.serializer(weatherData, Weather::class.java).toByteArray(Charset.forName("UTF-8"))))
            }

            mapRequest.dataMap.putLong(WearableSettings.KEY_UPDATETIME, Instant.now(Clock.systemUTC()).toEpochMilli())

            val request = mapRequest.asPutDataRequest()
            if (urgent) request.setUrgent()
            try {
                val client = Wearable.getDataClient(mContext)
                client.deleteDataItems(mapRequest.uri).await()
                client.putDataItem(request).await()
            } catch (e: ExecutionException) {
                Logger.writeLine(Log.ERROR, e)
            } catch (e: InterruptedException) {
                Logger.writeLine(Log.ERROR, e)
            }

            Logger.writeLine(Log.INFO, "%s: createWeatherDataRequest(): urgent: %s", TAG, java.lang.Boolean.toString(urgent))
        }
    }
}