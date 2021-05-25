package com.thewizrd.shared_resources.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.content.edit
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.google.gson.stream.JsonReader
import com.thewizrd.shared_resources.ApplicationLib
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.database.LocationsDAO
import com.thewizrd.shared_resources.database.LocationsDatabase
import com.thewizrd.shared_resources.database.WeatherDAO
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.icons.WeatherIconsProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.utils.Units.*
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.WeatherProviders
import com.thewizrd.shared_resources.weatherdata.model.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.StringReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@SuppressLint("CommitPrefEdits")
class SettingsManager(context: Context) {
    private val appContext = context.applicationContext
    val isPhone = context.resources.getBoolean(R.bool.isPhone)

    // Shared Settings
    private val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val editor = preferences.edit()
    private val wuSharedPrefs = appContext.getSharedPreferences(WeatherAPI.WEATHERUNDERGROUND, Context.MODE_PRIVATE)
    private val versionPrefs = appContext.getSharedPreferences("version", Context.MODE_PRIVATE)

    companion object {
        const val TAG = "SettingsManager"

        // Database
        private var locationDB: LocationsDatabase? = null
        private var weatherDB: WeatherDatabase? = null

        // NOTE: Remember to add migrations for ALL databases when updating version
        const val CURRENT_DBVERSION = 8

        // Data
        private const val CACHE_LIMIT = 25
        private const val MAX_LOCATIONS = 10

        const val DEFAULTINTERVAL = 180

        const val CONNECTION_TIMEOUT = 10000 // 10s
        const val READ_TIMEOUT = 10000 // 10s

        // Settings Keys
        const val KEY_API = "API"
        const val KEY_APIKEY = "API_KEY"
        const val KEY_APIKEY_VERIFIED = "API_KEY_VERIFIED"
        private const val KEY_USECELSIUS = "key_usecelsius"
        private const val KEY_WEATHERLOADED = "weatherLoaded"
        const val KEY_FOLLOWGPS = "key_followgps"
        private const val KEY_LASTGPSLOCATION = "key_lastgpslocation"
        const val KEY_REFRESHINTERVAL = "key_refreshinterval"
        private const val KEY_UPDATETIME = "key_updatetime"
        private const val KEY_DBVERSION = "key_dbversion"
        const val KEY_USEALERTS = "key_usealerts"
        const val KEY_USEPERSONALKEY = "key_usepersonalkey"
        private const val KEY_CURRENTVERSION = "key_currentversion"
        const val KEY_TEMPUNIT = "key_tempunit"
        const val KEY_SPEEDUNIT = "key_speedunit"
        const val KEY_DISTANCEUNIT = "key_distanceunit"
        const val KEY_PRECIPITATIONUNIT = "key_precipitationunit"
        const val KEY_PRESSUREUNIT = "key_pressureunit"
        const val KEY_ICONSSOURCE = "key_iconssource"

        // !ANDROID_WEAR
        const val KEY_ONGOINGNOTIFICATION = "key_ongoingnotification"
        const val KEY_NOTIFICATIONICON = "key_notificationicon"
        private const val KEY_ONBOARDINGCOMPLETE = "key_onboardcomplete"
        const val KEY_USERTHEME = "key_usertheme"
        const val TEMPERATURE_ICON = "0"
        const val CONDITION_ICON = "1"

        const val KEY_DAILYNOTIFICATION = "key_dailynotification"
        const val KEY_DAILYNOTIFICATIONTIME = "key_dailynotificationtime"
        const val KEY_POPCHANCENOTIFICATION = "key_popchancenotification"
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

        fun isLoaded() = loaded
    }

    private fun getLocationDB(): LocationsDatabase {
        if (locationDB == null) {
            locationDB = Room.databaseBuilder(appContext,
                    LocationsDatabase::class.java, "locations.db")
                    .addMigrations(*DBMigrations.LOC_MIGRATION_SET)
                    .fallbackToDestructiveMigration()
                    .build()
        }

        return locationDB!!
    }

    private fun getWeatherDB(): WeatherDatabase {
        if (weatherDB == null) {
            weatherDB = Room.databaseBuilder(appContext,
                    WeatherDatabase::class.java, "weatherdata.db")
                    .addMigrations(*DBMigrations.W_MIGRATION_SET)
                    .fallbackToDestructiveMigration()
                    .build()
        }

        return weatherDB!!
    }

    @Synchronized
    suspend fun loadIfNeeded() = withContext(Dispatchers.IO) {
        if (!loaded) {
            load()
            loaded = true
        }
    }

    @Synchronized
    fun loadIfNeededSync() {
        runBlocking {
            loadIfNeeded()
        }
    }

    private suspend fun load() {
        /* DB Migration */
        DBMigrations.performMigrations(appContext, getWeatherDB(), getLocationDB())

        val lastGPSLoc = getLastGPSLocation()
        if (!lastGPSLoc.isNullOrBlank()) {
            try {
                withContext(Dispatchers.IO) {
                    val reader = JsonReader(StringReader(lastGPSLoc))
                    lastGPSLocData = LocationData().apply {
                        fromJson(reader)
                    }
                }
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex, "Error on load(): lastGPSLocData")
            } finally {
                if (lastGPSLocData?.tzLong.isNullOrEmpty()) lastGPSLocData = LocationData()
            }
        }

        /* Version-specific Migration */
        VersionMigrations.performMigrations(appContext, getWeatherDB(), getLocationDB())
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getWeatherDAO(): WeatherDAO = getWeatherDB().weatherDAO()

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getLocationsDAO(): LocationsDAO = getLocationDB().locationsDAO()

    // Shared Preferences listener
    class SettingsListener(@NonNull private val app: ApplicationLib) : OnSharedPreferenceChangeListener {
        private val mLocalBroadcastManager = LocalBroadcastManager.getInstance(app.appContext)
        private val settingsMgr = SettingsManager(app.appContext)

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            if (key.isNullOrBlank()) return

            val isWeatherLoaded = sharedPreferences.getBoolean(KEY_WEATHERLOADED, false)

            when (key) {
                KEY_API -> {
                    // Weather Provider changed
                    WeatherManager.instance.updateAPI()
                    if (isWeatherLoaded) {
                        mLocalBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI))
                    }
                }
                KEY_USEPERSONALKEY -> {
                    // Weather Provider changed
                    WeatherManager.instance.updateAPI()
                }
                KEY_FOLLOWGPS -> {
                    if (isWeatherLoaded) {
                        val value = sharedPreferences.getBoolean(key, false)
                        mLocalBroadcastManager.sendBroadcast(
                                Intent(CommonActions.ACTION_SETTINGS_UPDATEGPS))
                        if (app.isPhone) mLocalBroadcastManager.sendBroadcast(
                                Intent(if (value) CommonActions.ACTION_WIDGET_REFRESHWIDGETS else CommonActions.ACTION_WIDGET_RESETWIDGETS))
                    }
                }
                KEY_REFRESHINTERVAL -> {
                    if (isWeatherLoaded) {
                        mLocalBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_UPDATEREFRESH))
                    }
                }
                KEY_DATASYNC -> {
                    if (isWeatherLoaded) {
                        // Reset UpdateTime value to force a refresh
                        val dataSync = WearableDataSync.valueOf(sharedPreferences.getString(KEY_DATASYNC, "0")!!.toInt())
                        settingsMgr.setUpdateTime(DateTimeUtils.getLocalDateTimeMIN())
                        // Reset interval if setting is off
                        if (dataSync == WearableDataSync.OFF) settingsMgr.setRefreshInterval(DEFAULTINTERVAL)
                    }
                }
                KEY_ICONSSOURCE -> {
                    WeatherIconsManager.getInstance().updateIconProvider()
                }
                KEY_DAILYNOTIFICATION -> {
                    if (isWeatherLoaded) {
                        mLocalBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_UPDATEDAILYNOTIFICATION))
                    }
                }
            }
        }
    }

    fun getFavorites(): Collection<LocationData>? {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            getLocationsDAO().favorites
        }
    }

    fun getLocationData(): List<LocationData>? {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            getLocationsDAO().loadAllLocationData()
        }
    }

    fun getLocation(key: String?): LocationData? {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            getLocationsDAO().getLocation(key)
        }
    }

    fun getWeatherData(key: String?): Weather? {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            getWeatherDAO().getWeatherData(key)
        }
    }

    fun getWeatherDataByCoordinate(location: LocationData): Weather? {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            val query = String.format(Locale.ROOT, "\"latitude\":\"%s\",\"longitude\":\"%s\"",
                    location.latitude.toString(), location.longitude.toString())
            getWeatherDAO().getWeatherDataByCoord("%$query%")
        }
    }

    fun getWeatherAlertData(key: String?): Collection<WeatherAlert> {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            var alerts: Collection<WeatherAlert>? = null
            try {
                val weatherAlertData = getWeatherDAO().getWeatherAlertData(key)
                if (weatherAlertData?.alerts != null) alerts = weatherAlertData.alerts
            } catch (ex: Exception) {
                Logger.writeLine(Log.ERROR, ex, "SimpleWeather: Settings.GetWeatherAlertData()")
            } finally {
                if (alerts == null) alerts = ArrayList()
            }
            alerts
        }
    }

    fun getWeatherForecastData(key: String?): Forecasts? {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            getWeatherDAO().getForecastData(key)
        }
    }

    fun getHourlyWeatherForecastDataByLimit(key: String?, loadSize: Int): List<HourlyForecast>? {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            getWeatherDAO().getHourlyForecastsByQueryOrderByDateByLimit(key, loadSize)
        }
    }

    fun getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(key: String?, loadSize: Int, date: ZonedDateTime?): List<HourlyForecast>? {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            getWeatherDAO().getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(key, loadSize, date)
        }
    }

    fun getHourlyWeatherForecastData(key: String?): List<HourlyForecast>? {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            getWeatherDAO().getHourlyForecastsByQueryOrderByDate(key)
        }
    }

    fun getFirstHourlyForecastDataByDate(key: String?, date: ZonedDateTime?): HourlyForecast? {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            getWeatherDAO().getFirstHourlyForecastDataByDate(key, date)
        }
    }

    fun getLastGPSLocData(): LocationData? {
        return runBlocking(Dispatchers.IO) {
            loadIfNeeded()
            if (lastGPSLocData?.locationType != LocationType.GPS) {
                lastGPSLocData?.locationType = LocationType.GPS
            }
            lastGPSLocData
        }
    }

    fun saveWeatherData(weather: Weather?) {
        if (weather != null && weather.isValid) {
            runBlocking(Dispatchers.IO) {
                getWeatherDAO().insertWeatherData(weather)
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            if (getWeatherDAO().weatherDataCount > CACHE_LIMIT) cleanupWeatherData()
        }
    }

    fun saveWeatherAlerts(location: LocationData?, alerts: Collection<WeatherAlert>?) {
        if (location != null && location.isValid) {
            runBlocking(Dispatchers.IO) {
                val alertData = WeatherAlerts(location.query, alerts)
                getWeatherDAO().insertWeatherAlertData(alertData)
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            if (getWeatherDAO().weatherAlertDataCount > CACHE_LIMIT) cleanupWeatherAlertData()
        }
    }

    fun saveWeatherForecasts(forecasts: Forecasts?) {
        if (forecasts != null) {
            runBlocking(Dispatchers.IO) {
                getWeatherDAO().insertForecast(forecasts)
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            if (getWeatherDAO().forecastDataCountGroupedByQuery > CACHE_LIMIT / 2) cleanupWeatherForecastData()
        }
    }

    fun saveWeatherForecasts(key: String, forecasts: Collection<HourlyForecasts>?) {
        runBlocking(Dispatchers.IO) {
            getWeatherDAO().deleteHourlyForecastByKey(key)
            if (forecasts != null) {
                getWeatherDAO().insertAllHourlyForecasts(forecasts)
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            if (getWeatherDAO().hourlyForecastCountGroupedByQuery > CACHE_LIMIT / 2) cleanupWeatherForecastData()
        }
    }

    private fun cleanupWeatherData() {
        GlobalScope.launch(Dispatchers.IO) {
            val locs: List<LocationData?>
            if (isPhone) {
                locs = getLocationsDAO().loadAllLocationData()
                if (useFollowGPS()) locs.add(lastGPSLocData)
            } else {
                locs = listOf(getHomeData())
            }

            val locQueries = locs.map { it?.query }
            getWeatherDAO().deleteWeatherDataByKeyNotIn(locQueries)
        }
    }

    private fun cleanupWeatherForecastData() {
        GlobalScope.launch(Dispatchers.IO) {
            val locs: List<LocationData?>
            if (isPhone) {
                locs = getLocationsDAO().loadAllLocationData()
                if (useFollowGPS()) locs.add(lastGPSLocData)
            } else {
                locs = listOf(getHomeData())
            }

            val locQueries = locs.map { it?.query }
            getWeatherDAO().deleteForecastByKeyNotIn(locQueries)
            getWeatherDAO().deleteHourlyForecastByKeyNotIn(locQueries)
        }
    }

    private fun cleanupWeatherAlertData() {
        GlobalScope.launch(Dispatchers.IO) {
            val locs: List<LocationData?>
            if (isPhone) {
                locs = getLocationsDAO().loadAllLocationData()
                if (useFollowGPS()) locs.add(lastGPSLocData)
            } else {
                locs = listOf(getHomeData())
            }

            val locQueries = locs.map { it?.query }
            getWeatherDAO().deleteWeatherAlertDataByKeyNotIn(locQueries)
        }
    }

    fun addLocation(location: LocationData?) {
        if (location != null && location.isValid) {
            runBlocking(Dispatchers.IO) {
                getLocationsDAO().insertLocationData(location)
                val pos = getLocationsDAO().locationDataCount
                val fav = Favorites().apply {
                    query = location.query
                    position = pos
                }
                getLocationsDAO().insertFavorite(fav)
            }
        }
    }

    fun updateLocation(location: LocationData?) {
        if (location?.locationType == LocationType.GPS && location.isValid) {
            saveLastGPSLocData(location)
        } else if (location?.locationType == LocationType.SEARCH && location.isValid) {
            runBlocking(Dispatchers.IO) {
                getLocationsDAO().updateLocationData(location)
            }
        }
    }

    fun updateLocationWithKey(location: LocationData?, oldKey: String?) {
        if (location?.isValid == true && !oldKey.isNullOrBlank()) {
            runBlocking(Dispatchers.IO) {
                // Get position from favorites table
                var fav = getLocationsDAO().getFavorite(oldKey) ?: return@runBlocking
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
    }

    fun deleteLocations() {
        runBlocking(Dispatchers.IO) {
            getLocationsDAO().deleteAllLocationData()
            getLocationsDAO().deleteAllFavoriteData()
        }
    }

    fun deleteLocation(key: String?) {
        if (!key.isNullOrBlank()) {
            runBlocking(Dispatchers.IO) {
                getLocationsDAO().deleteLocationDataByKey(key)
                getLocationsDAO().deleteFavoritesByKey(key)
                resetPostition()
            }
        }
    }

    fun moveLocation(key: String?, toPos: Int) {
        if (!key.isNullOrBlank()) {
            runBlocking(Dispatchers.IO) {
                getLocationsDAO().updateFavPosition(key, toPos)
            }
        }
    }

    private fun resetPostition() {
        runBlocking(Dispatchers.IO) {
            val favs = getLocationsDAO().loadAllFavoritesByPosition()
            for (fav in favs) {
                fav.position = favs.indexOf(fav)
                getLocationsDAO().updateFavorite(fav)
            }
        }
    }

    fun saveLastGPSLocData(data: LocationData?) {
        lastGPSLocData = data
        runBlocking(Dispatchers.Default) {
            setLastGPSLocation(JSONParser.serializer(lastGPSLocData, LocationData::class.java))
        }
    }

    fun getHomeData(): LocationData? {
        val homeData: LocationData?
        if (isPhone) {
            homeData = if (useFollowGPS()) {
                getLastGPSLocData()
            } else {
                runBlocking(Dispatchers.IO) {
                    loadIfNeeded()
                    getLocationsDAO().firstFavorite
                }
            }
        } else {
            homeData = getLastGPSLocData()
            if (homeData != null && !useFollowGPS()) homeData.locationType = LocationType.SEARCH
        }
        return homeData
    }

    // Android Wear specific members
    @RequiresApi(Build.VERSION_CODES.M)
    fun saveHomeData(data: LocationData?) {
        saveLastGPSLocData(data)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getDataSync(): WearableDataSync {
        return WearableDataSync.valueOf(preferences.getString(KEY_DATASYNC, "0")!!.toInt())
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setDataSync(value: WearableDataSync) {
        editor.putString(KEY_DATASYNC, value.value.toString())
        editor.commit()
    }

    // Settings Members
    @TemperatureUnits
    fun getTemperatureUnit(): String {
        return preferences.getString(KEY_TEMPUNIT, if (preferences.getBoolean(KEY_USECELSIUS, false)) CELSIUS else FAHRENHEIT)!!
    }

    fun setTemperatureUnit(@TemperatureUnits unit: String?) {
        editor.putString(KEY_TEMPUNIT, unit)
        editor.commit()
    }

    @SpeedUnits
    fun getSpeedUnit(): String {
        return preferences.getString(KEY_SPEEDUNIT, MILES_PER_HOUR)!!
    }

    fun setSpeedUnit(@SpeedUnits unit: String?) {
        editor.putString(KEY_SPEEDUNIT, unit)
        editor.commit()
    }

    @PressureUnits
    fun getPressureUnit(): String {
        return preferences.getString(KEY_PRESSUREUNIT, INHG)!!
    }

    fun setPressureUnit(@PressureUnits unit: String?) {
        editor.putString(KEY_PRESSUREUNIT, unit)
        editor.commit()
    }

    @DistanceUnits
    fun getDistanceUnit(): String {
        return preferences.getString(KEY_DISTANCEUNIT, MILES)!!
    }

    fun setDistanceUnit(@DistanceUnits unit: String?) {
        editor.putString(KEY_DISTANCEUNIT, unit)
        editor.commit()
    }

    @PrecipitationUnits
    fun getPrecipitationUnit(): String {
        return preferences.getString(KEY_PRECIPITATIONUNIT, INCHES)!!
    }

    fun setPrecipitationUnit(@PrecipitationUnits unit: String?) {
        editor.putString(KEY_PRECIPITATIONUNIT, unit)
        editor.commit()
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
        editor.putString(KEY_TEMPUNIT, unit)
        editor.putString(KEY_SPEEDUNIT, if (isFahrenheit) MILES_PER_HOUR else KILOMETERS_PER_HOUR)
        editor.putString(KEY_PRESSUREUNIT, if (isFahrenheit) INHG else MILLIBAR)
        editor.putString(KEY_DISTANCEUNIT, if (isFahrenheit) MILES else KILOMETERS)
        editor.putString(KEY_PRECIPITATIONUNIT, if (isFahrenheit) INCHES else MILLIMETERS)
        editor.apply()
    }

    fun isWeatherLoaded(): Boolean {
        return runBlocking(Dispatchers.IO) {
            if (isPhone) {
                if (!DBUtils.locationDataExists(getLocationDB())) {
                    setWeatherLoaded(false)
                    return@runBlocking false
                }
            } else {
                if (!DBUtils.weatherDataExists(getWeatherDB())) {
                    setWeatherLoaded(false)
                    return@runBlocking false
                }
            }

            if (preferences.contains(KEY_WEATHERLOADED) && preferences.getBoolean(KEY_WEATHERLOADED, false)) {
                setWeatherLoaded(true)
                return@runBlocking true
            } else {
                return@runBlocking false
            }
        }
    }

    fun setWeatherLoaded(isLoaded: Boolean) {
        editor.putBoolean(KEY_WEATHERLOADED, isLoaded)
        editor.commit()
    }

    @WeatherProviders
    fun getAPI(): String? {
        return if (!preferences.contains(KEY_API)) {
            val API = RemoteConfig.getDefaultWeatherProvider()
            setAPI(API)
            API
        } else {
            preferences.getString(KEY_API, null)
        }
    }

    fun setAPI(@WeatherProviders api: String?) {
        editor.putString(KEY_API, api)
        editor.commit()
    }

    fun getAPIKEY(): String? {
        return if (!preferences.contains(KEY_APIKEY)) {
            ""
        } else {
            preferences.getString(KEY_APIKEY, null)
        }
    }

    fun setAPIKEY(key: String?) {
        editor.putString(KEY_APIKEY, key)
        editor.commit()
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
        editor.putBoolean(KEY_FOLLOWGPS, value)
        editor.commit()
    }

    fun getLastGPSLocation(): String? {
        return preferences.getString(KEY_LASTGPSLOCATION, null)
    }

    fun setLastGPSLocation(value: String?) {
        editor.putString(KEY_LASTGPSLOCATION, value)
        editor.commit()
    }

    fun getUpdateTime(): LocalDateTime {
        return if (!preferences.contains(KEY_UPDATETIME)) {
            DateTimeUtils.getLocalDateTimeMIN()
        } else {
            LocalDateTime.parse(preferences.getString(KEY_UPDATETIME, "1/1/1900 12:00:00 AM"),
                    DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.ROOT))
        }
    }

    fun setUpdateTime(value: LocalDateTime) {
        editor.putString(KEY_UPDATETIME, value.format(DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.ROOT)))
        editor.commit()
    }

    fun getRefreshInterval(): Int {
        return preferences.getString(KEY_REFRESHINTERVAL, DEFAULTINTERVAL.toString())!!.toInt()
    }

    fun setRefreshInterval(value: Int) {
        editor.putString(KEY_REFRESHINTERVAL, value.toString())
        editor.commit()
    }

    fun showOngoingNotification(): Boolean {
        return if (!preferences.contains(KEY_ONGOINGNOTIFICATION)) false else preferences.getBoolean(KEY_ONGOINGNOTIFICATION, false)
    }

    fun setOngoingNotification(value: Boolean) {
        editor.putBoolean(KEY_ONGOINGNOTIFICATION, value)
        editor.commit()
    }

    fun getNotificationIcon(): String {
        return if (!preferences.contains(KEY_NOTIFICATIONICON)) {
            TEMPERATURE_ICON
        } else {
            preferences.getString(KEY_NOTIFICATIONICON, TEMPERATURE_ICON)!!
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
        editor.putBoolean(KEY_USEALERTS, value)
        editor.commit()
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
        editor.putString(KEY_USERTHEME, value.value.toString())
        editor.commit()
    }
    // END - !ANDROID_WEAR

    fun getDBVersion(): Int {
        return preferences.getString(KEY_DBVERSION, "0")!!.toInt()
    }

    fun setDBVersion(value: Int) {
        editor.putString(KEY_DBVERSION, value.toString())
        editor.commit()
    }

    fun isKeyVerified(): Boolean {
        return if (!wuSharedPrefs.contains(KEY_APIKEY_VERIFIED)) {
            false
        } else {
            wuSharedPrefs.getBoolean(KEY_APIKEY_VERIFIED, false)
        }
    }

    fun setKeyVerified(value: Boolean) {
        val wuEditor = wuSharedPrefs.edit()
        wuEditor.putBoolean(KEY_APIKEY_VERIFIED, value)
        wuEditor.apply()
        if (!value) wuEditor.remove(KEY_APIKEY_VERIFIED).apply()
    }

    fun usePersonalKey(): Boolean {
        return if (!preferences.contains(KEY_USEPERSONALKEY)) {
            false
        } else {
            preferences.getBoolean(KEY_USEPERSONALKEY, false)
        }
    }

    fun setPersonalKey(value: Boolean) {
        editor.putBoolean(KEY_USEPERSONALKEY, value)
        editor.commit()
    }

    fun getVersionCode(): Long {
        return versionPrefs.getString(KEY_CURRENTVERSION, "0")!!.toLong()
    }

    fun setVersionCode(value: Long) {
        val versionEditor = versionPrefs.edit()
        versionEditor.putString(KEY_CURRENTVERSION, value.toString())
        versionEditor.apply()
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
        editor.putBoolean(KEY_ONBOARDINGCOMPLETE, value)
        editor.commit()
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun getAnimatorScale(): Float {
        return Settings.Global.getFloat(appContext.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
    }

    fun getIconsProvider(): String {
        return preferences.getString(KEY_ICONSSOURCE, WeatherIconsProvider.KEY)!!
    }

    fun setIconsProvider(iconsSource: String?) {
        editor.putString(KEY_ICONSSOURCE, iconsSource)
        editor.commit()
    }

    fun isDailyNotificationEnabled(): Boolean {
        return preferences.getBoolean(KEY_DAILYNOTIFICATION, false)
    }

    fun setDailyNotificationEnabled(value: Boolean) {
        preferences.edit(true) {
            putBoolean(KEY_DAILYNOTIFICATION, value)
        }
    }

    fun getDailyNotificationTime(): String {
        return preferences.getString(KEY_DAILYNOTIFICATIONTIME, DEFAULT_DAILYNOTIFICATION_TIME)!!
    }

    fun setDailyNotificationTime(value: String) {
        preferences.edit(true) {
            putString(KEY_DAILYNOTIFICATIONTIME, value)
        }
    }

    fun isPoPChanceNotificationEnabled(): Boolean {
        return preferences.getBoolean(KEY_POPCHANCENOTIFICATION, false)
    }

    fun setPoPChanceNotificationEnabled(value: Boolean) {
        preferences.edit(true) {
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
        preferences.edit(true) {
            putString(KEY_LASTCHANCENOTIFICATIONTIME, value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
        }
    }
}