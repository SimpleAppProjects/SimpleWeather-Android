package com.thewizrd.simpleweather.wearable

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.*
import com.thewizrd.common.wearable.WearableHelper
import com.thewizrd.common.wearable.WearableSettings
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.time.Clock
import java.time.Instant

class WearableWorker(context: Context, workerParams: WorkerParameters) :
        CoroutineWorker(context, workerParams) {
    private val settingsManager = SettingsManager(context.applicationContext)

    companion object {
        private const val TAG = "WearableWorker"

        // Actions
        private const val KEY_ACTION = "action"
        private const val KEY_URGENTREQUEST = "urgent"
        private const val KEY_NODEID = "nodeID"

        @JvmStatic
        @JvmOverloads
        fun enqueueAction(context: Context, intentAction: String, urgent: Boolean = true) {
            when (intentAction) {
                WearableWorkerActions.ACTION_SENDUPDATE,
                WearableWorkerActions.ACTION_SENDSETTINGSUPDATE,
                WearableWorkerActions.ACTION_SENDLOCATIONUPDATE,
                WearableWorkerActions.ACTION_SENDWEATHERUPDATE,
                WearableWorkerActions.ACTION_SENDPARTIALWEATHERUPDATE,
                WearableWorkerActions.ACTION_SENDSETUPSTATUS -> {
                    startWork(context.applicationContext, intentAction, urgent)
                }
            }
        }

        fun sendSetupStatus(context: Context, nodeID: String) {
            startWork(
                context.applicationContext,
                WearableWorkerActions.ACTION_SENDSETUPSTATUS,
                true,
                nodeID
            )
        }

        private fun startWork(
            context: Context,
            intentAction: String,
            urgent: Boolean,
            nodeID: String? = null
        ) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val data = Data.Builder()
                .putString(KEY_ACTION, intentAction)
                .putBoolean(KEY_URGENTREQUEST, urgent)

            nodeID?.let { data.putString(KEY_NODEID, nodeID) }

            val updateRequest = OneTimeWorkRequestBuilder<WearableWorker>()
                .setInputData(data.build())
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueue(updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)
        }
    }

    override suspend fun doWork(): Result {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG)

        val intentAction = inputData.getString(KEY_ACTION)
        val urgent = inputData.getBoolean(KEY_URGENTREQUEST, true)

        Logger.writeLine(Log.INFO, "%s: Action: %s", TAG, intentAction)

        // Don't send anything unless we're setup
        if (!settingsManager.isWeatherLoaded())
            return Result.success()

        val mWearNodesWithApp = findWearDevicesWithApp()

        if (!mWearNodesWithApp.isEmpty()) {
            when (intentAction) {
                WearableWorkerActions.ACTION_SENDUPDATE -> {
                    createSettingsDataRequest(urgent)
                    createLocationDataRequest(urgent)
                    createWeatherDataRequest(urgent)
                }
                WearableWorkerActions.ACTION_SENDSETTINGSUPDATE -> {
                    createSettingsDataRequest(urgent)
                }
                WearableWorkerActions.ACTION_SENDLOCATIONUPDATE -> {
                    createLocationDataRequest(urgent)
                }
                WearableWorkerActions.ACTION_SENDWEATHERUPDATE -> {
                    createWeatherDataRequest(urgent)
                }
                WearableWorkerActions.ACTION_SENDPARTIALWEATHERUPDATE -> {
                    createWeatherDataRequest(urgent, partialUpdate = true)
                }
                WearableWorkerActions.ACTION_SENDSETUPSTATUS -> {
                    inputData.getString(KEY_NODEID)?.let { sendSetupStatus(it) }
                }
            }
        }

        return Result.success()
    }

    /* Wearable Functions */
    private suspend fun findWearDevicesWithApp(): Collection<Node> = withContext(Dispatchers.IO) {
        val capabilityInfo = try {
            Wearable.getCapabilityClient(applicationContext)
                .getCapability(WearableHelper.CAPABILITY_WEAR_APP, CapabilityClient.FILTER_ALL)
                .await()
        } catch (e: Exception) {
            logError(e)
            null
        }

        return@withContext capabilityInfo?.nodes ?: emptySet()
    }

    private suspend fun createSettingsDataRequest(urgent: Boolean) {
        withContext(Dispatchers.IO) {
            val mapRequest = PutDataMapRequest.create(WearableHelper.SettingsPath)
            mapRequest.dataMap.putString(WearableSettings.KEY_API, settingsManager.getAPI() ?: "")
            mapRequest.dataMap.putString(
                WearableSettings.KEY_APIKEY,
                settingsManager.getAPIKey() ?: ""
            )
            mapRequest.dataMap.putBoolean(
                WearableSettings.KEY_APIKEY_VERIFIED,
                settingsManager.getAPI()?.let { settingsManager.isKeyVerified(it) } ?: false
            )
            mapRequest.dataMap.putBoolean(
                WearableSettings.KEY_FOLLOWGPS,
                settingsManager.useFollowGPS()
            )
            mapRequest.dataMap.putString(
                WearableSettings.KEY_TEMPUNIT,
                settingsManager.getTemperatureUnit()
            )

            val unitMap = DataMap()
            unitMap.putString(WearableSettings.KEY_TEMPUNIT, settingsManager.getTemperatureUnit())
            unitMap.putString(WearableSettings.KEY_SPEEDUNIT, settingsManager.getSpeedUnit())
            unitMap.putString(WearableSettings.KEY_DISTANCEUNIT, settingsManager.getDistanceUnit())
            unitMap.putString(WearableSettings.KEY_PRESSUREUNIT, settingsManager.getPressureUnit())
            unitMap.putString(
                WearableSettings.KEY_PRECIPITATIONUNIT,
                settingsManager.getPrecipitationUnit()
            )
            mapRequest.dataMap.putDataMap(WearableSettings.KEY_UNITS, unitMap)

            if (settingsManager.isDevSettingsEnabled()) {
                val devSettingsMap = DataMap().apply {
                    putBoolean(WearableSettings.KEY_DEVSETTINGS, true)
                }
                mapRequest.dataMap.putDataMap(WearableSettings.KEY_DEVSETTINGS, devSettingsMap)
            }

            val apiKeyMap = DataMap()
            val apiKeyVerifyMap = DataMap()
            for (entry in settingsManager.getAPIKeyMap()) {
                if (entry.value is String) {
                    apiKeyMap.putString(entry.key, entry.value as String)
                    apiKeyVerifyMap.putBoolean(entry.key, settingsManager.isKeyVerified(entry.key))
                }
            }
            mapRequest.dataMap.putDataMap(WearableSettings.KEY_APIKEYS, apiKeyMap)
            mapRequest.dataMap.putDataMap(WearableSettings.KEY_APIKEYS_VERIFIED, apiKeyVerifyMap)

            mapRequest.dataMap.putString(
                WearableSettings.KEY_LANGUAGE,
                LocaleUtils.getLocaleCode() ?: ""
            )
            mapRequest.dataMap.putString(
                WearableSettings.KEY_ICONPROVIDER,
                settingsManager.getIconsProvider()
            )
            mapRequest.dataMap.putLong(
                WearableSettings.KEY_UPDATETIME,
                Instant.now(Clock.systemUTC()).toEpochMilli()
            )
            val request = mapRequest.asPutDataRequest()
            if (urgent) request.setUrgent()
            try {
                val client = Wearable.getDataClient(applicationContext)
                client.deleteDataItems(mapRequest.uri).await()
                client.putDataItem(request).await()
            } catch (e: Exception) {
                logError(e)
            }

            Logger.writeLine(
                Log.INFO,
                "%s: createSettingsDataRequest(): urgent: %s",
                TAG,
                urgent.toString()
            )
        }
    }

    private suspend fun createLocationDataRequest(urgent: Boolean) {
        withContext(Dispatchers.IO) {
            val mapRequest = PutDataMapRequest.create(WearableHelper.LocationPath)
            val homeData = settingsManager.getHomeData()
            mapRequest.dataMap.putString(
                WearableSettings.KEY_LOCATIONDATA,
                JSONParser.serializer(homeData) ?: ""
            )
            mapRequest.dataMap.putLong(
                WearableSettings.KEY_UPDATETIME,
                Instant.now().toEpochMilli()
            )
            val request = mapRequest.asPutDataRequest()
            if (urgent) request.setUrgent()
            try {
                val client = Wearable.getDataClient(applicationContext)
                client.deleteDataItems(mapRequest.uri).await()
                client.putDataItem(request).await()
            } catch (e: Exception) {
                logError(e)
            }

            Logger.writeLine(
                Log.INFO,
                "%s: createLocationDataRequest(): urgent: %s",
                TAG,
                urgent.toString()
            )
        }
    }

    private suspend fun createWeatherDataRequest(urgent: Boolean, partialUpdate: Boolean = false) {
        withContext(Dispatchers.IO) {
            val mapRequest = PutDataMapRequest.create(WearableHelper.WeatherPath)
            val homeData = settingsManager.getHomeData()!!
            val weatherData = settingsManager.getWeatherData(homeData.query)
            val alertData = settingsManager.getWeatherAlertData(homeData.query)
            val forecasts = settingsManager.getWeatherForecastData(homeData.query)
            val hrForecasts = settingsManager.getHourlyWeatherForecastData(homeData.query)

            if (weatherData != null) {
                weatherData.forecast = forecasts?.forecast
                weatherData.hrForecast = hrForecasts
                weatherData.txtForecast = forecasts?.txtForecast
                weatherData.minForecast = forecasts?.minForecast
                weatherData.weatherAlerts = alertData
                mapRequest.dataMap.putAsset(
                    WearableSettings.KEY_WEATHERDATA,
                    Asset.createFromBytes(
                        JSONParser.serializer(weatherData)?.toByteArray(Charset.forName("UTF-8"))
                            ?: byteArrayOf()
                    )
                )
            }

            mapRequest.dataMap.putLong(
                WearableSettings.KEY_UPDATETIME,
                Instant.now(Clock.systemUTC()).toEpochMilli()
            )
            mapRequest.dataMap.putBoolean(
                WearableSettings.KEY_PARTIAL_WEATHER_UPDATE,
                partialUpdate
            )

            val request = mapRequest.asPutDataRequest()
            if (urgent) request.setUrgent()
            try {
                val client = Wearable.getDataClient(applicationContext)
                client.deleteDataItems(mapRequest.uri).await()
                client.putDataItem(request).await()
            } catch (e: Exception) {
                logError(e)
            }

            Logger.writeLine(
                Log.INFO,
                "%s: createWeatherDataRequest(): urgent: %s",
                TAG,
                urgent.toString()
            )
        }
    }

    private suspend fun sendSetupStatus(nodeID: String) =
        withContext(Dispatchers.IO) {
            try {
                val client = Wearable.getMessageClient(applicationContext)
                client.sendMessage(
                    nodeID, WearableHelper.IsSetupPath,
                    byteArrayOf((if (settingsManager.isWeatherLoaded()) 1 else 0).toByte())
                ).await()
            } catch (e: Exception) {
                logError(e)
            }
        }

    private fun logError(e: Exception) {
        if (e is ApiException || e.cause is ApiException) {
            val apiException = e.cause as? ApiException ?: e as? ApiException
            if (apiException?.statusCode == WearableStatusCodes.API_NOT_CONNECTED ||
                apiException?.statusCode == WearableStatusCodes.TARGET_NODE_NOT_CONNECTED
            ) {
                // Ignore this error
                return
            }
        }

        Logger.writeLine(Log.ERROR, e)
    }
}