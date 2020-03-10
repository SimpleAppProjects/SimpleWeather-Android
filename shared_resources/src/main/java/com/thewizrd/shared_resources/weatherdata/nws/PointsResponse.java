package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class PointsResponse {

    @SerializedName("radarStation")
    private String radarStation;

    @SerializedName("fireWeatherZone")
    private String fireWeatherZone;

    @SerializedName("@type")
    private String type;

    @SerializedName("forecastZone")
    private String forecastZone;

    @SerializedName("county")
    private String county;

    @SerializedName("timeZone")
    private String timeZone;

    @SerializedName("forecast")
    private String forecast;

    @SerializedName("cwa")
    private String cwa;

    @SerializedName("@context")
    private Context context;

    @SerializedName("relativeLocation")
    private RelativeLocation relativeLocation;

    @SerializedName("forecastHourly")
    private String forecastHourly;

    @SerializedName("observationStations")
    private String observationStations;

    @SerializedName("gridX")
    private int gridX;

    @SerializedName("forecastGridData")
    private String forecastGridData;

    @SerializedName("gridY")
    private int gridY;

    @SerializedName("forecastOffice")
    private String forecastOffice;

    @SerializedName("geometry")
    private String geometry;

    @SerializedName("@id")
    private String id;

    public void setRadarStation(String radarStation) {
        this.radarStation = radarStation;
    }

    public String getRadarStation() {
        return radarStation;
    }

    public void setFireWeatherZone(String fireWeatherZone) {
        this.fireWeatherZone = fireWeatherZone;
    }

    public String getFireWeatherZone() {
        return fireWeatherZone;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setForecastZone(String forecastZone) {
        this.forecastZone = forecastZone;
    }

    public String getForecastZone() {
        return forecastZone;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCounty() {
        return county;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setForecast(String forecast) {
        this.forecast = forecast;
    }

    public String getForecast() {
        return forecast;
    }

    public void setCwa(String cwa) {
        this.cwa = cwa;
    }

    public String getCwa() {
        return cwa;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public void setRelativeLocation(RelativeLocation relativeLocation) {
        this.relativeLocation = relativeLocation;
    }

    public RelativeLocation getRelativeLocation() {
        return relativeLocation;
    }

    public void setForecastHourly(String forecastHourly) {
        this.forecastHourly = forecastHourly;
    }

    public String getForecastHourly() {
        return forecastHourly;
    }

    public void setObservationStations(String observationStations) {
        this.observationStations = observationStations;
    }

    public String getObservationStations() {
        return observationStations;
    }

    public void setGridX(int gridX) {
        this.gridX = gridX;
    }

    public int getGridX() {
        return gridX;
    }

    public void setForecastGridData(String forecastGridData) {
        this.forecastGridData = forecastGridData;
    }

    public String getForecastGridData() {
        return forecastGridData;
    }

    public void setGridY(int gridY) {
        this.gridY = gridY;
    }

    public int getGridY() {
        return gridY;
    }

    public void setForecastOffice(String forecastOffice) {
        this.forecastOffice = forecastOffice;
    }

    public String getForecastOffice() {
        return forecastOffice;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}