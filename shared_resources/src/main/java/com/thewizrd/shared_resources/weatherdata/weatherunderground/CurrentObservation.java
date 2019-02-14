package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class CurrentObservation {

    @SerializedName("nowcast")
    private String nowcast;

    @SerializedName("temp_c")
    private float tempC;

    @SerializedName("observation_epoch")
    private String observationEpoch;

    @SerializedName("temp_f")
    private float tempF;

    @SerializedName("wind_kph")
    private float windKph;

    @SerializedName("wind_mph")
    private float windMph;

    @SerializedName("wind_degrees")
    private int windDegrees;

    @SerializedName("temperature_string")
    private String temperatureString;

    @SerializedName("weather")
    private String weather;

    @SerializedName("feelslike_string")
    private String feelslikeString;

    @SerializedName("precip_today_metric")
    private String precipTodayMetric;

    @SerializedName("precip_1hr_string")
    private String precip1hrString;

    @SerializedName("icon_url")
    private String iconUrl;

    @SerializedName("image")
    private Image image;

    @SerializedName("UV")
    private String uV;

    @SerializedName("station_id")
    private String stationId;

    @SerializedName("local_epoch")
    private String localEpoch;

    @SerializedName("local_tz_short")
    private String localTzShort;

    @SerializedName("wind_dir")
    private String windDir;

    @SerializedName("precip_1hr_metric")
    private String precip1hrMetric;

    @SerializedName("pressure_in")
    private String pressureIn;

    @SerializedName("local_tz_long")
    private String localTzLong;

    @SerializedName("wind_gust_mph")
    private String windGustMph;

    @SerializedName("windchill_string")
    private String windchillString;

    @SerializedName("wind_gust_kph")
    private String windGustKph;

    @SerializedName("wind_string")
    private String windString;

    @SerializedName("local_time_rfc822")
    private String localTimeRfc822;

    @SerializedName("visibility_km")
    private String visibilityKm;

    @SerializedName("relative_humidity")
    private String relativeHumidity;

    @SerializedName("pressure_mb")
    private String pressureMb;

    @SerializedName("observation_time_rfc822")
    private String observationTimeRfc822;

    @SerializedName("precip_1hr_in")
    private String precip1hrIn;

    @SerializedName("feelslike_c")
    private float feelslikeC;

    @SerializedName("observation_time")
    private String observationTime;

    @SerializedName("feelslike_f")
    private float feelslikeF;

    @SerializedName("history_url")
    private String historyUrl;

    @SerializedName("windchill_f")
    private String windchillF;

    @SerializedName("windchill_c")
    private String windchillC;

    @SerializedName("precip_today_string")
    private String precipTodayString;

    @SerializedName("icon")
    private String icon;

    @SerializedName("precip_today_in")
    private String precipTodayIn;

    @SerializedName("solarradiation")
    private String solarradiation;

    @SerializedName("observation_location")
    private ObservationLocation observationLocation;

    @SerializedName("dewpoint_f")
    private String dewpointF;

    @SerializedName("display_location")
    private DisplayLocation displayLocation;

    @SerializedName("dewpoint_string")
    private String dewpointString;

    @SerializedName("pressure_trend")
    private String pressureTrend;

    @SerializedName("dewpoint_c")
    private String dewpointC;

    @SerializedName("estimated")
    private Estimated estimated;

    @SerializedName("forecast_url")
    private String forecastUrl;

    @SerializedName("local_tz_offset")
    private String localTzOffset;

    @SerializedName("heat_index_f")
    private String heatIndexF;

    @SerializedName("heat_index_c")
    private String heatIndexC;

    @SerializedName("ob_url")
    private String obUrl;

    @SerializedName("heat_index_string")
    private String heatIndexString;

    @SerializedName("visibility_mi")
    private String visibilityMi;

    public void setNowcast(String nowcast) {
        this.nowcast = nowcast;
    }

    public String getNowcast() {
        return nowcast;
    }

    public void setTempC(float tempC) {
        this.tempC = tempC;
    }

    public float getTempC() {
        return tempC;
    }

    public void setObservationEpoch(String observationEpoch) {
        this.observationEpoch = observationEpoch;
    }

    public String getObservationEpoch() {
        return observationEpoch;
    }

    public void setTempF(float tempF) {
        this.tempF = tempF;
    }

    public float getTempF() {
        return tempF;
    }

    public void setWindKph(float windKph) {
        this.windKph = windKph;
    }

    public float getWindKph() {
        return windKph;
    }

    public void setWindMph(float windMph) {
        this.windMph = windMph;
    }

    public float getWindMph() {
        return windMph;
    }

    public void setWindDegrees(int windDegrees) {
        this.windDegrees = windDegrees;
    }

    public int getWindDegrees() {
        return windDegrees;
    }

    public void setTemperatureString(String temperatureString) {
        this.temperatureString = temperatureString;
    }

    public String getTemperatureString() {
        return temperatureString;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getWeather() {
        return weather;
    }

    public void setFeelslikeString(String feelslikeString) {
        this.feelslikeString = feelslikeString;
    }

    public String getFeelslikeString() {
        return feelslikeString;
    }

    public void setPrecipTodayMetric(String precipTodayMetric) {
        this.precipTodayMetric = precipTodayMetric;
    }

    public String getPrecipTodayMetric() {
        return precipTodayMetric;
    }

    public void setPrecip1hrString(String precip1hrString) {
        this.precip1hrString = precip1hrString;
    }

    public String getPrecip1hrString() {
        return precip1hrString;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Image getImage() {
        return image;
    }

    public void setUV(String uV) {
        this.uV = uV;
    }

    public String getUV() {
        return uV;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getStationId() {
        return stationId;
    }

    public void setLocalEpoch(String localEpoch) {
        this.localEpoch = localEpoch;
    }

    public String getLocalEpoch() {
        return localEpoch;
    }

    public void setLocalTzShort(String localTzShort) {
        this.localTzShort = localTzShort;
    }

    public String getLocalTzShort() {
        return localTzShort;
    }

    public void setWindDir(String windDir) {
        this.windDir = windDir;
    }

    public String getWindDir() {
        return windDir;
    }

    public void setPrecip1hrMetric(String precip1hrMetric) {
        this.precip1hrMetric = precip1hrMetric;
    }

    public String getPrecip1hrMetric() {
        return precip1hrMetric;
    }

    public void setPressureIn(String pressureIn) {
        this.pressureIn = pressureIn;
    }

    public String getPressureIn() {
        return pressureIn;
    }

    public void setLocalTzLong(String localTzLong) {
        this.localTzLong = localTzLong;
    }

    public String getLocalTzLong() {
        return localTzLong;
    }

    public void setWindGustMph(String windGustMph) {
        this.windGustMph = windGustMph;
    }

    public String getWindGustMph() {
        return windGustMph;
    }

    public void setWindchillString(String windchillString) {
        this.windchillString = windchillString;
    }

    public String getWindchillString() {
        return windchillString;
    }

    public void setWindGustKph(String windGustKph) {
        this.windGustKph = windGustKph;
    }

    public String getWindGustKph() {
        return windGustKph;
    }

    public void setWindString(String windString) {
        this.windString = windString;
    }

    public String getWindString() {
        return windString;
    }

    public void setLocalTimeRfc822(String localTimeRfc822) {
        this.localTimeRfc822 = localTimeRfc822;
    }

    public String getLocalTimeRfc822() {
        return localTimeRfc822;
    }

    public void setVisibilityKm(String visibilityKm) {
        this.visibilityKm = visibilityKm;
    }

    public String getVisibilityKm() {
        return visibilityKm;
    }

    public void setRelativeHumidity(String relativeHumidity) {
        this.relativeHumidity = relativeHumidity;
    }

    public String getRelativeHumidity() {
        return relativeHumidity;
    }

    public void setPressureMb(String pressureMb) {
        this.pressureMb = pressureMb;
    }

    public String getPressureMb() {
        return pressureMb;
    }

    public void setObservationTimeRfc822(String observationTimeRfc822) {
        this.observationTimeRfc822 = observationTimeRfc822;
    }

    public String getObservationTimeRfc822() {
        return observationTimeRfc822;
    }

    public void setPrecip1hrIn(String precip1hrIn) {
        this.precip1hrIn = precip1hrIn;
    }

    public String getPrecip1hrIn() {
        return precip1hrIn;
    }

    public void setFeelslikeC(float feelslikeC) {
        this.feelslikeC = feelslikeC;
    }

    public float getFeelslikeC() {
        return feelslikeC;
    }

    public void setObservationTime(String observationTime) {
        this.observationTime = observationTime;
    }

    public String getObservationTime() {
        return observationTime;
    }

    public void setFeelslikeF(float feelslikeF) {
        this.feelslikeF = feelslikeF;
    }

    public float getFeelslikeF() {
        return feelslikeF;
    }

    public void setHistoryUrl(String historyUrl) {
        this.historyUrl = historyUrl;
    }

    public String getHistoryUrl() {
        return historyUrl;
    }

    public void setWindchillF(String windchillF) {
        this.windchillF = windchillF;
    }

    public String getWindchillF() {
        return windchillF;
    }

    public void setWindchillC(String windchillC) {
        this.windchillC = windchillC;
    }

    public String getWindchillC() {
        return windchillC;
    }

    public void setPrecipTodayString(String precipTodayString) {
        this.precipTodayString = precipTodayString;
    }

    public String getPrecipTodayString() {
        return precipTodayString;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public void setPrecipTodayIn(String precipTodayIn) {
        this.precipTodayIn = precipTodayIn;
    }

    public String getPrecipTodayIn() {
        return precipTodayIn;
    }

    public void setSolarradiation(String solarradiation) {
        this.solarradiation = solarradiation;
    }

    public String getSolarradiation() {
        return solarradiation;
    }

    public void setObservationLocation(ObservationLocation observationLocation) {
        this.observationLocation = observationLocation;
    }

    public ObservationLocation getObservationLocation() {
        return observationLocation;
    }

    public void setDewpointF(String dewpointF) {
        this.dewpointF = dewpointF;
    }

    public String getDewpointF() {
        return dewpointF;
    }

    public void setDisplayLocation(DisplayLocation displayLocation) {
        this.displayLocation = displayLocation;
    }

    public DisplayLocation getDisplayLocation() {
        return displayLocation;
    }

    public void setDewpointString(String dewpointString) {
        this.dewpointString = dewpointString;
    }

    public String getDewpointString() {
        return dewpointString;
    }

    public void setPressureTrend(String pressureTrend) {
        this.pressureTrend = pressureTrend;
    }

    public String getPressureTrend() {
        return pressureTrend;
    }

    public void setDewpointC(String dewpointC) {
        this.dewpointC = dewpointC;
    }

    public String getDewpointC() {
        return dewpointC;
    }

    public void setEstimated(Estimated estimated) {
        this.estimated = estimated;
    }

    public Estimated getEstimated() {
        return estimated;
    }

    public void setForecastUrl(String forecastUrl) {
        this.forecastUrl = forecastUrl;
    }

    public String getForecastUrl() {
        return forecastUrl;
    }

    public void setLocalTzOffset(String localTzOffset) {
        this.localTzOffset = localTzOffset;
    }

    public String getLocalTzOffset() {
        return localTzOffset;
    }

    public void setHeatIndexF(String heatIndexF) {
        this.heatIndexF = heatIndexF;
    }

    public String getHeatIndexF() {
        return heatIndexF;
    }

    public void setHeatIndexC(String heatIndexC) {
        this.heatIndexC = heatIndexC;
    }

    public String getHeatIndexC() {
        return heatIndexC;
    }

    public void setObUrl(String obUrl) {
        this.obUrl = obUrl;
    }

    public String getObUrl() {
        return obUrl;
    }

    public void setHeatIndexString(String heatIndexString) {
        this.heatIndexString = heatIndexString;
    }

    public String getHeatIndexString() {
        return heatIndexString;
    }

    public void setVisibilityMi(String visibilityMi) {
        this.visibilityMi = visibilityMi;
    }

    public String getVisibilityMi() {
        return visibilityMi;
    }
}