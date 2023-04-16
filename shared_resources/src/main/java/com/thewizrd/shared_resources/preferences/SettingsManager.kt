package com.thewizrd.shared_resources.preferences

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.squareup.moshi.JsonReader
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.database.LocationsDAO
import com.thewizrd.shared_resources.database.LocationsDatabase
import com.thewizrd.shared_resources.database.WeatherDAO
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.buildEmptyGPSLocation
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.DateTimeUtils.LOCAL_DATE_TIME_FORMATTER
import com.thewizrd.shared_resources.utils.DateTimeUtils.LOCAL_DATE_TIME_MIN
import com.thewizrd.shared_resources.utils.Units.*
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Buffer
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class SettingsManager(context: Context) {
    private val appContext = context.applicationContext
    val isPhone = context.resources.getBoolean(R.bool.isPhone)

    // Shared Settings
    private val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val wuSharedPrefs = appContext.getSharedPreferences(
        WeatherAPI.WEATHERUNDERGROUND,
        Context.MODE_PRIVATE
    )
    private val versionPrefs = appContext.getSharedPreferences("version", Context.MODE_PRIVATE)
    private val devSettings = appContext.getSharedPreferences("devsettings", Context.MODE_PRIVATE)

    companion object {
        const val TAG = "SettingsManager"

        // Data
        private const val CACHE_LIMIT = 25
        private const val MAX_LOCATIONS = 10

        const val DEFAULT_INTERVAL = 180

        const val CONNECTION_TIMEOUT = 10000 // 10s
        const val READ_TIMEOUT = 10000 // 10s

        // Settings Keys
        const val KEY_API = "API"
        const val KEY_APIKEY = "API_KEY"
        private const val KEY_APIKEY_VERIFIED = "API_KEY_VERIFIED"
        const val KEY_APIKEY_PREFIX = "api_key"
        private const val KEY_USECELSIUS = "key_usecelsius"
        const val KEY_WEATHERLOADED = "weatherLoaded"
        const val KEY_FOLLOWGPS = "key_followgps"
        private const val KEY_LASTGPSLOCATION = "key_lastgpslocation"
        const val KEY_REFRESHINTERVAL = "key_refreshinterval"
        private const val KEY_UPDATETIME = "key_updatetime"
        const val KEY_USEALERTS = "key_usealerts"
        const val KEY_USEPERSONALKEY = "key_usepersonalkey"
        private const val KEY_CURRENTVERSION = "key_currentversion"
        private const val KEY_CURRENT_SDK_VERSION = "key_current_sdk_version"
        const val KEY_TEMPUNIT = "key_tempunit"
        const val KEY_SPEEDUNIT = "key_speedunit"
        const val KEY_DISTANCEUNIT = "key_distanceunit"
        const val KEY_PRECIPITATIONUNIT = "key_precipitationunit"
        const val KEY_PRESSUREUNIT = "key_pressureunit"
        const val KEY_ICONSSOURCE = "key_iconssource"

        // !ANDROID_WEAR
        const val KEY_ONGOINGNOTIFICATION = "key_ongoingnotification"
        const val KEY_NOTIFICATIONICON = "key_notificationicon"
        const val KEY_NOTIFICATIONFCAST = "key_notificationfcast"
        private const val KEY_ONBOARDINGCOMPLETE = "key_onboardcomplete"
        const val KEY_USERTHEME = "key_usertheme"
        private const val KEY_DEVSETTINGSENABLED = "key_devsettingsenabled"

        const val TEMPERATURE_ICON = "0"
        const val CONDITION_ICON = "1"

        @StringDef(
            TEMPERATURE_ICON,
            CONDITION_ICON
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class NotificationIconOption

        const val NOTIF_FORECAST_NONE = "0"
        const val NOTIF_FORECAST_DAILY = "1"
        const val NOTIF_FORECAST_HOURLY = "2"

        @StringDef(
            NOTIF_FORECAST_NONE,
            NOTIF_FORECAST_DAILY,
            NOTIF_FORECAST_HOURLY
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class NotificationForecastType

        const val KEY_DAILYNOTIFICATION = "key_dailynotification"
        const val KEY_DAILYNOTIFICATIONTIME = "key_dailynotificationtime"
        const val KEY_POPCHANCENOTIFICATION = "key_popchancenotification"
        const val KEY_POPCHANCEPCT = "key_popchancepct"
        const val KEY_LASTCHANCENOTIFICATIONTIME = "key_lastchancenotificationtime"

        // Format: HH:mm (24-hr)
        const val DEFAULT_DAILYNOTIFICATION_TIME = "08:00"

        // END - !ANDROID_WEAR
        // ANDROID_WEAR - only
        const val KEY_DATASYNC = "key_datasync"
        // END

        // Weather Data
        private var lastGPSLocData: LocationData? = null
        private var loaded: Boolean = false
        private val mutex = Mutex()

        fun isLoaded() = loaded
    }

    private fun getLocationDB(): LocationsDatabase {
        return LocationsDatabase.getInstance(appContext)
    }

    private fun getWeatherDB(): WeatherDatabase {
        return WeatherDatabase.getInstance(appContext)
    }

    suspend fun loadIfNeeded() = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!loaded) {
                load()
                loaded = true
            }
        }
    }

    private suspend fun load() {
        val lastGPSLoc = getLastGPSLocation()
        if (!lastGPSLoc.isNullOrBlank()) {
            try {
                withContext(Dispatchers.IO) {
                    val reader = JsonReader.of(Buffer().writeUtf8(lastGPSLoc))
                    lastGPSLocData = LocationData().apply {
                        fromJson(reader)
                    }
                }
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex, "Error on load(): lastGPSLocData")
            } finally {
                if (lastGPSLocData?.tzLong.isNullOrEmpty()) {
                    lastGPSLocData = buildEmptyGPSLocation()
                }
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private fun getWeatherDAO(): WeatherDAO = getWeatherDB().weatherDAO()

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private fun getLocationsDAO(): LocationsDAO = getLocationDB().locationsDAO()

    suspend fun getFavorites(): Collection<LocationData> {
        loadIfNeeded()
        return getLocationsDAO().getFavorites()
    }

    suspend fun getLocationData(): List<LocationData> {
        loadIfNeeded()
        return getLocationsDAO().loadAllLocationData()
    }

    suspend fun getLocation(key: String?): LocationData? {
        loadIfNeeded()
        return getLocationsDAO().getLocation(key)
    }

    suspend fun getWeatherData(key: String?): Weather? {
        loadIfNeeded()
        return getWeatherDAO().getWeatherData(key)
    }

    suspend fun getWeatherDataByCoordinate(location: LocationData): Weather? {
        loadIfNeeded()
        val query = String.format(
            Locale.ROOT, "\"latitude\":\"%s\",\"longitude\":\"%s\"",
            location.latitude.toString(), location.longitude.toString()
        )
        return getWeatherDAO().getWeatherDataByCoord("%$query%")
    }

    suspend fun getWeatherAlertData(key: String?): Collection<WeatherAlert> {
        loadIfNeeded()

        var alerts: Collection<WeatherAlert>? = null

        try {
            val weatherAlertData = getWeatherDAO().getWeatherAlertData(key)
            alerts = weatherAlertData?.alerts
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex, "SimpleWeather: Settings.GetWeatherAlertData()")
        }

        return alerts ?: emptyList()
    }

    suspend fun getWeatherForecastData(key: String?): Forecasts? {
        loadIfNeeded()
        return getWeatherDAO().getForecastData(key)
    }

    suspend fun getHourlyWeatherForecastDataByLimit(
        key: String?,
        loadSize: Int
    ): List<HourlyForecast> {
        loadIfNeeded()
        return getWeatherDAO().getHourlyForecastsByQueryOrderByDateByLimit(key, loadSize)
    }

    suspend fun getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
        key: String?,
        loadSize: Int,
        date: ZonedDateTime
    ): List<HourlyForecast> {
        loadIfNeeded()
        return getWeatherDAO().getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
            key,
            loadSize,
            date
        )
    }

    suspend fun getHourlyWeatherForecastData(key: String?): List<HourlyForecast> {
        loadIfNeeded()
        return getWeatherDAO().getHourlyForecastsByQueryOrderByDate(key)
    }

    suspend fun getFirstHourlyForecastDataByDate(
        key: String?,
        date: ZonedDateTime
    ): HourlyForecast? {
        loadIfNeeded()
        return getWeatherDAO().getFirstHourlyForecastDataByDate(key, date)
    }

    suspend fun getLastGPSLocData(): LocationData? {
        loadIfNeeded()
        if (lastGPSLocData?.locationType != LocationType.GPS) {
            lastGPSLocData?.locationType = LocationType.GPS
        }
        return lastGPSLocData
    }

    suspend fun saveWeatherData(weather: Weather) {
        if (!weather.isNullOrInvalid()) {
            getWeatherDAO().insertWeatherData(weather)
        }

        appLib.appScope.launch(Dispatchers.IO) {
            if (getWeatherDAO().getWeatherDataCount() > CACHE_LIMIT) cleanupWeatherData()
        }
    }

    suspend fun saveWeatherAlerts(location: LocationData?, alerts: Collection<WeatherAlert>?) {
        if (location?.isValid == true) {
            val alertData = WeatherAlerts(location.query, alerts)
            getWeatherDAO().insertWeatherAlertData(alertData)
        }

        appLib.appScope.launch(Dispatchers.IO) {
            if (getWeatherDAO().getWeatherAlertDataCount() > CACHE_LIMIT) cleanupWeatherAlertData()
        }
    }

    suspend fun saveWeatherForecasts(forecasts: Forecasts?) {
        if (forecasts != null) {
            getWeatherDAO().insertForecast(forecasts)
        }

        appLib.appScope.launch(Dispatchers.IO) {
            if (getWeatherDAO().getForecastDataCountGroupedByQuery() > CACHE_LIMIT / 2) cleanupWeatherForecastData()
        }
    }

    suspend fun saveWeatherForecasts(key: String, forecasts: Collection<HourlyForecasts>?) {
        getWeatherDAO().deleteHourlyForecastByKey(key)
        if (forecasts != null) {
            getWeatherDAO().insertAllHourlyForecasts(forecasts)
        }

        appLib.appScope.launch(Dispatchers.IO) {
            if (getWeatherDAO().getHourlyForecastCountGroupedByQuery() > CACHE_LIMIT / 2) cleanupWeatherForecastData()
        }
    }

    private suspend fun cleanupWeatherData() {
        val locs: List<LocationData?>
        if (isPhone) {
            locs = getLocationsDAO().loadAllLocationData().toMutableList<LocationData?>()
            if (useFollowGPS()) locs.add(lastGPSLocData)
        } else {
            locs = listOf(getHomeData())
        }

        val locQueries = locs.map { it?.query }
        getWeatherDAO().deleteWeatherDataByKeyNotIn(locQueries)
    }

    private suspend fun cleanupWeatherForecastData() {
        val locs: List<LocationData?>
        if (isPhone) {
            locs = getLocationsDAO().loadAllLocationData().toMutableList<LocationData?>()
            if (useFollowGPS()) locs.add(lastGPSLocData)
        } else {
            locs = listOf(getHomeData())
        }

        val locQueries = locs.map { it?.query }
        getWeatherDAO().deleteForecastByKeyNotIn(locQueries)
        getWeatherDAO().deleteHourlyForecastByKeyNotIn(locQueries)
    }

    private suspend fun cleanupWeatherAlertData() {
        val locs: List<LocationData?>
        if (isPhone) {
            locs = getLocationsDAO().loadAllLocationData().toMutableList<LocationData?>()
            if (useFollowGPS()) locs.add(lastGPSLocData)
        } else {
            locs = listOf(getHomeData())
        }

        val locQueries = locs.map { it?.query }
        getWeatherDAO().deleteWeatherAlertDataByKeyNotIn(locQueries)
    }

    suspend fun addLocation(location: LocationData?) {
        if (location?.isValid == true) {
            getLocationsDAO().insertLocationData(location)
            val pos = getLocationsDAO().getLocationDataCount()
            val fav = Favorites().apply {
                query = location.query
                position = pos
            }
            getLocationsDAO().insertFavorite(fav)
        }
    }

    suspend fun updateLocation(location: LocationData?) {
        if (appLib.isPhone) {
            if (location?.locationType == LocationType.GPS && location.isValid) {
                saveLastGPSLocData(location)
            } else if (location?.locationType == LocationType.SEARCH && location.isValid) {
                getLocationsDAO().updateLocationData(location)
            }
        } else {
            saveLastGPSLocData(location)
        }
    }

    suspend fun updateLocationWithKey(location: LocationData?, oldKey: String?) {
        if (location?.isValid == true && !oldKey.isNullOrBlank()) {
            // Get position from favorites table
            var fav = getLocationsDAO().getFavorite(oldKey) ?: return
            val pos = fav.position

            // Remove location from table
            getLocationsDAO().deleteLocationDataByKey(oldKey)
            getLocationsDAO().deleteFavoritesByKey(oldKey)

            // Add updated location with new query (pkey)
            getLocationsDAO().insertLocationData(location)
            fav = Favorites().apply {
                query = location.query
                position = pos
            }
            getLocationsDAO().insertFavorite(fav)
        }
    }

    suspend fun deleteLocations() {
        getLocationsDAO().deleteAllLocationData()
        getLocationsDAO().deleteAllFavoriteData()
    }

    suspend fun deleteLocation(key: String?) {
        if (!key.isNullOrBlank()) {
            getLocationsDAO().deleteLocationDataByKey(key)
            getLocationsDAO().deleteFavoritesByKey(key)
            resetPostition()
        }
    }

    suspend fun moveLocation(key: String?, toPos: Int) {
        if (!key.isNullOrBlank()) {
            getLocationsDAO().updateFavPosition(key, toPos)
        }
    }

    private suspend fun resetPostition() {
        val favs = getLocationsDAO().loadAllFavoritesByPosition()
        for (fav in favs) {
            fav.position = favs.indexOf(fav)
            getLocationsDAO().updateFavorite(fav)
        }
    }

    suspend fun saveLastGPSLocData(data: LocationData?) {
        lastGPSLocData = data
        withContext(Dispatchers.Default) {
            setLastGPSLocation(JSONParser.serializer(lastGPSLocData, LocationData::class.java))
        }
    }

    suspend fun getHomeData(): LocationData? {
        val homeData: LocationData?
        if (isPhone) {
            homeData = if (useFollowGPS()) {
                getLastGPSLocData()
            } else {
                loadIfNeeded()
                getLocationsDAO().getFirstFavorite()
            }
        } else {
            homeData = getLastGPSLocData()
            if (homeData != null && !useFollowGPS()) homeData.locationType = LocationType.SEARCH
        }
        return homeData
    }

    // Android Wear specific members
    @RequiresApi(Build.VERSION_CODES.M)
    fun getDataSync(): WearableDataSync {
        return WearableDataSync.valueOf(preferences.getString(KEY_DATASYNC, "0")!!.toInt())
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setDataSync(value: WearableDataSync) {
        preferences.edit {
            putString(KEY_DATASYNC, value.value.toString())
        }
    }

    // Settings Members
    @TemperatureUnits
    fun getTemperatureUnit(): String {
        return preferences.getString(KEY_TEMPUNIT, if (preferences.getBoolean(KEY_USECELSIUS, false)) CELSIUS else FAHRENHEIT)!!
    }

    fun setTemperatureUnit(@TemperatureUnits unit: String?) {
        preferences.edit {
            putString(KEY_TEMPUNIT, unit)
        }
    }

    @SpeedUnits
    fun getSpeedUnit(): String {
        return preferences.getString(KEY_SPEEDUNIT, MILES_PER_HOUR)!!
    }

    fun setSpeedUnit(@SpeedUnits unit: String?) {
        preferences.edit {
            putString(KEY_SPEEDUNIT, unit)
        }
    }

    @PressureUnits
    fun getPressureUnit(): String {
        return preferences.getString(KEY_PRESSUREUNIT, INHG)!!
    }

    fun setPressureUnit(@PressureUnits unit: String?) {
        preferences.edit {
            putString(KEY_PRESSUREUNIT, unit)
        }
    }

    @DistanceUnits
    fun getDistanceUnit(): String {
        return preferences.getString(KEY_DISTANCEUNIT, MILES)!!
    }

    fun setDistanceUnit(@DistanceUnits unit: String?) {
        preferences.edit {
            putString(KEY_DISTANCEUNIT, unit)
        }
    }

    @PrecipitationUnits
    fun getPrecipitationUnit(): String {
        return preferences.getString(KEY_PRECIPITATIONUNIT, INCHES)!!
    }

    fun setPrecipitationUnit(@PrecipitationUnits unit: String?) {
        preferences.edit {
            putString(KEY_PRECIPITATIONUNIT, unit)
        }
    }

    fun getUnitString(): String {
        return String.format(Locale.ROOT, "%s;%s;%s;%s;%s",
                getTemperatureUnit(),
                getSpeedUnit(),
                getPressureUnit(),
                getDistanceUnit(),
                getPrecipitationUnit()
        )
    }

    fun setDefaultUnits(@TemperatureUnits unit: String) {
        val isFahrenheit = FAHRENHEIT == unit
        preferences.edit {
            putString(KEY_TEMPUNIT, unit)
            putString(KEY_SPEEDUNIT, if (isFahrenheit) MILES_PER_HOUR else KILOMETERS_PER_HOUR)
            putString(KEY_PRESSUREUNIT, if (isFahrenheit) INHG else MILLIBAR)
            putString(KEY_DISTANCEUNIT, if (isFahrenheit) MILES else KILOMETERS)
            putString(KEY_PRECIPITATIONUNIT, if (isFahrenheit) INCHES else MILLIMETERS)
        }
    }

    fun isWeatherLoaded(): Boolean {
        return if (!preferences.contains(KEY_WEATHERLOADED)) {
            setWeatherLoaded(false)
            false
        } else {
            preferences.getBoolean(KEY_WEATHERLOADED, false)
        }
    }

    fun setWeatherLoaded(isLoaded: Boolean) {
        preferences.edit {
            putBoolean(KEY_WEATHERLOADED, isLoaded)
        }
    }

    @WeatherAPI.WeatherProviders
    fun getAPI(): String? {
        return if (!preferences.contains(KEY_API)) {
            val API = remoteConfigService.getDefaultWeatherProvider()
            setAPI(API)
            API
        } else {
            preferences.getString(KEY_API, null)
        }
    }

    fun setAPI(@WeatherAPI.WeatherProviders api: String?) {
        preferences.edit {
            putString(KEY_API, api)
        }
    }

    @Deprecated("Use getAPIKey()", ReplaceWith("getAPIKey()"))
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getAPIKEY(): String? {
        return if (!preferences.contains(KEY_APIKEY)) {
            ""
        } else {
            preferences.getString(KEY_APIKEY, null)
        }
    }

    fun getAPIKey(): String? {
        return getAPI()?.let { getAPIKey(it) }
    }

    fun setAPIKey(key: String?) {
        getAPI()?.let {
            setAPIKey(it, key)
        }
    }

    fun getAPIKey(@WeatherAPI.WeatherProviders provider: String): String? {
        return preferences.getString("${KEY_APIKEY_PREFIX}_${provider}", null)
    }

    fun setAPIKey(@WeatherAPI.WeatherProviders provider: String, key: String?) {
        preferences.edit {
            putString("${KEY_APIKEY_PREFIX}_${provider}", key)
        }
    }

    fun getAPIKeyMap(): Map<String, Any?> {
        return preferences.all.filter { (key, _) ->
            key.startsWith(KEY_APIKEY_PREFIX, false)
        }
    }

    fun useFollowGPS(): Boolean {
        return if (!preferences.contains(KEY_FOLLOWGPS)) {
            setFollowGPS(false)
            false
        } else {
            preferences.getBoolean(KEY_FOLLOWGPS, false)
        }
    }

    fun setFollowGPS(value: Boolean) {
        preferences.edit {
            putBoolean(KEY_FOLLOWGPS, value)
        }
    }

    private fun getLastGPSLocation(): String? {
        return preferences.getString(KEY_LASTGPSLOCATION, null)
    }

    private fun setLastGPSLocation(value: String?) {
        preferences.edit {
            putString(KEY_LASTGPSLOCATION, value)
        }
    }

    fun getUpdateTime(): LocalDateTime {
        return if (!preferences.contains(KEY_UPDATETIME)) {
            DateTimeUtils.LOCALDATETIME_MIN
        } else {
            LocalDateTime.parse(
                preferences.getString(KEY_UPDATETIME, LOCAL_DATE_TIME_MIN),
                LOCAL_DATE_TIME_FORMATTER
            )
        }
    }

    fun setUpdateTime(value: LocalDateTime) {
        preferences.edit {
            putString(KEY_UPDATETIME, value.format(LOCAL_DATE_TIME_FORMATTER))
        }
    }

    fun getRefreshInterval(): Int {
        return preferences.getString(KEY_REFRESHINTERVAL, DEFAULT_INTERVAL.toString())!!.toInt()
    }

    fun setRefreshInterval(value: Int) {
        preferences.edit {
            putString(KEY_REFRESHINTERVAL, value.toString())
        }
    }

    fun showOngoingNotification(): Boolean {
        return if (!preferences.contains(KEY_ONGOINGNOTIFICATION)) false else preferences.getBoolean(KEY_ONGOINGNOTIFICATION, false)
    }

    fun setOngoingNotification(value: Boolean) {
        preferences.edit {
            putBoolean(KEY_ONGOINGNOTIFICATION, value)
        }
    }

    @NotificationIconOption
    fun getNotificationIcon(): String {
        return if (!preferences.contains(KEY_NOTIFICATIONICON)) {
            TEMPERATURE_ICON
        } else {
            preferences.getString(KEY_NOTIFICATIONICON, TEMPERATURE_ICON)!!
        }
    }

    @NotificationForecastType
    fun getNotificationForecastType(): String {
        return preferences.getString(KEY_NOTIFICATIONFCAST, NOTIF_FORECAST_NONE)
            ?: NOTIF_FORECAST_NONE
    }

    fun setNotificationForecastType(@NotificationForecastType forecastType: String) {
        preferences.edit {
            putString(
                KEY_NOTIFICATIONFCAST,
                when (forecastType) {
                    NOTIF_FORECAST_NONE,
                    NOTIF_FORECAST_DAILY,
                    NOTIF_FORECAST_HOURLY ->
                        forecastType
                    else -> NOTIF_FORECAST_NONE
                }
            )
        }
    }

    fun useAlerts(): Boolean {
        return if (!preferences.contains(KEY_USEALERTS)) {
            setAlerts(false)
            false
        } else {
            preferences.getBoolean(KEY_USEALERTS, false)
        }
    }

    fun setAlerts(value: Boolean) {
        preferences.edit {
            putBoolean(KEY_USEALERTS, value)
        }
    }

    fun getUserThemeMode(): UserThemeMode {
        return if (!preferences.contains(KEY_USERTHEME)) {
            setUserThemeMode(UserThemeMode.FOLLOW_SYSTEM)
            UserThemeMode.FOLLOW_SYSTEM
        } else {
            UserThemeMode.valueOf(preferences.getString(KEY_USERTHEME, "0")!!.toInt())
        }
    }

    fun setUserThemeMode(value: UserThemeMode) {
        preferences.edit {
            putString(KEY_USERTHEME, value.value.toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestedBGAccess(): Boolean {
        return preferences.getBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun setRequestBGAccess(value: Boolean) {
        preferences.edit {
            putBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION, value)
        }
    }
    // END - !ANDROID_WEAR

    @Deprecated(
        "Use isKeyVerified(String)",
        ReplaceWith("isKeyVerified(null)")
    )
    fun isKeyVerified(): Boolean {
        return if (!wuSharedPrefs.contains(KEY_APIKEY_VERIFIED)) {
            false
        } else {
            wuSharedPrefs.getBoolean(KEY_APIKEY_VERIFIED, false)
        }
    }

    @Deprecated(
        "Use setKeyVerified(String, Boolean)",
        ReplaceWith("setKeyVerified(null, true)")
    )
    fun setKeyVerified(value: Boolean) {
        val wuEditor = wuSharedPrefs.edit()
        wuEditor.putBoolean(KEY_APIKEY_VERIFIED, value)
        wuEditor.apply()
        if (!value) wuEditor.remove(KEY_APIKEY_VERIFIED).apply()
    }

    fun isKeyVerified(provider: String): Boolean {
        val prefKey = "${KEY_APIKEY_VERIFIED}_${provider}"

        return if (!wuSharedPrefs.contains(prefKey)) {
            false
        } else {
            wuSharedPrefs.getBoolean(prefKey, false)
        }
    }

    fun setKeyVerified(provider: String, value: Boolean) {
        val prefKey = "${KEY_APIKEY_VERIFIED}_${provider}"

        wuSharedPrefs.edit {
            putBoolean(prefKey, value)
            if (!value) {
                remove(prefKey)
            }
        }
    }

    fun usePersonalKey(): Boolean {
        return if (!preferences.contains(KEY_USEPERSONALKEY)) {
            false
        } else {
            preferences.getBoolean(KEY_USEPERSONALKEY, false)
        }
    }

    fun setPersonalKey(value: Boolean) {
        preferences.edit {
            putBoolean(KEY_USEPERSONALKEY, value)
        }
    }

    fun getVersionCode(): Long {
        return versionPrefs.getString(KEY_CURRENTVERSION, "0")!!.toLong()
    }

    fun setVersionCode(value: Long) {
        val versionEditor = versionPrefs.edit()
        versionEditor.putString(KEY_CURRENTVERSION, value.toString())
        versionEditor.apply()
    }

    fun getSDKVersionCode(): Int {
        return versionPrefs.getInt(KEY_CURRENT_SDK_VERSION, 0)
    }

    fun setSDKVersionCode(value: Int) {
        versionPrefs.edit {
            putInt(KEY_CURRENT_SDK_VERSION, value)
        }
    }

    fun getMaxLocations(): Int {
        return MAX_LOCATIONS
    }

    fun isOnBoardingComplete(): Boolean {
        return if (!preferences.contains(KEY_ONBOARDINGCOMPLETE)) {
            false
        } else {
            preferences.getBoolean(KEY_ONBOARDINGCOMPLETE, false)
        }
    }

    fun setOnBoardingComplete(value: Boolean) {
        preferences.edit {
            putBoolean(KEY_ONBOARDINGCOMPLETE, value)
        }
    }

    fun isDevSettingsEnabled(): Boolean {
        return devSettings.getBoolean(KEY_DEVSETTINGSENABLED, false)
    }

    fun setDevSettingsEnabled(enable: Boolean) {
        devSettings.edit(true) {
            putBoolean(KEY_DEVSETTINGSENABLED, enable)
        }
        localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_SENDUPDATE))
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getDevSettingsPreferenceMap(): Map<String, Any?> {
        return devSettings.all.minus(KEY_DEVSETTINGSENABLED)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun clearDevSettingsPreferences(enable: Boolean? = null) {
        val enabled = enable ?: isDevSettingsEnabled()
        devSettings.edit(true) {
            clear()
            putBoolean(KEY_DEVSETTINGSENABLED, enabled)
        }
    }

    fun getAnimatorScale(): Float {
        return Settings.Global.getFloat(
            appContext.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
    }

    fun getIconsProvider(): String {
        return preferences.getString(KEY_ICONSSOURCE, WeatherIconsEFProvider.KEY)!!
    }

    fun setIconsProvider(iconsSource: String?) {
        preferences.edit {
            putString(KEY_ICONSSOURCE, iconsSource)
        }
    }

    fun isDailyNotificationEnabled(): Boolean {
        return preferences.getBoolean(KEY_DAILYNOTIFICATION, false)
    }

    fun setDailyNotificationEnabled(value: Boolean) {
        preferences.edit {
            putBoolean(KEY_DAILYNOTIFICATION, value)
        }
    }

    fun getDailyNotificationTime(): String {
        return preferences.getString(KEY_DAILYNOTIFICATIONTIME, DEFAULT_DAILYNOTIFICATION_TIME)!!
    }

    fun setDailyNotificationTime(value: String) {
        preferences.edit {
            putString(KEY_DAILYNOTIFICATIONTIME, value)
        }
    }

    fun isPoPChanceNotificationEnabled(): Boolean {
        return preferences.getBoolean(KEY_POPCHANCENOTIFICATION, false)
    }

    fun setPoPChanceNotificationEnabled(value: Boolean) {
        preferences.edit {
            putBoolean(KEY_POPCHANCENOTIFICATION, value)
        }
    }

    fun getLastPoPChanceNotificationTime(): ZonedDateTime {
        return if (!preferences.contains(KEY_LASTCHANCENOTIFICATIONTIME)) {
            Instant.EPOCH.atZone(ZoneOffset.UTC)
        } else {
            ZonedDateTime.parse(
                    preferences.getString(KEY_LASTCHANCENOTIFICATIONTIME, "1970-01-01T00:00:00Z"),
                    DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        }
    }

    fun setLastPoPChanceNotificationTime(value: ZonedDateTime) {
        preferences.edit {
            putString(
                KEY_LASTCHANCENOTIFICATIONTIME,
                value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
            )
        }
    }

    fun getPoPChanceMinimumPercentage(): Int {
        return preferences.getString(KEY_POPCHANCEPCT, "60")?.toIntOrNull() ?: 60
    }

    fun setPoPChanceMinimumPercentage(
        @androidx.annotation.IntRange(
            from = 40L,
            to = 90L
        ) pct: Int
    ) {
        preferences.edit {
            putString(
                KEY_POPCHANCEPCT,
                if (pct in 40..90) {
                    pct
                } else {
                    60
                }.toString()
            )
        }
    }
}