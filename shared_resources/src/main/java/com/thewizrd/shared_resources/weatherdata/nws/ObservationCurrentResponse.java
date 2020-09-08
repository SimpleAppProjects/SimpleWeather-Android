package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class ObservationCurrentResponse {

    @SerializedName("dewpoint")
    private Dewpoint dewpoint;

    @SerializedName("windGust")
    private WindGust windGust;

    //@SerializedName("@type")
    //private String type;

    @SerializedName("icon")
    private String icon;

    @SerializedName("heatIndex")
    private HeatIndex heatIndex;

    //@SerializedName("minTemperatureLast24Hours")
    //private MinTemperatureLast24Hours minTemperatureLast24Hours;

    //@SerializedName("station")
    //private String station;

    @SerializedName("temperature")
    private Temperature temperature;

    @SerializedName("precipitationLastHour")
    private PrecipitationLastHour precipitationLastHour;

    //@SerializedName("@id")
    //private String id;

    @SerializedName("windDirection")
    private WindDirection windDirection;

    @SerializedName("windSpeed")
    private WindSpeed windSpeed;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("windChill")
    private WindChill windChill;

    @SerializedName("visibility")
    private Visibility visibility;

    //@SerializedName("maxTemperatureLast24Hours")
    //private MaxTemperatureLast24Hours maxTemperatureLast24Hours;

    @SerializedName("precipitationLast6Hours")
    private PrecipitationLast6Hours precipitationLast6Hours;

    @SerializedName("precipitationLast3Hours")
    private PrecipitationLast3Hours precipitationLast3Hours;

    @SerializedName("barometricPressure")
    private BarometricPressure barometricPressure;

    @SerializedName("relativeHumidity")
    private RelativeHumidity relativeHumidity;

    @SerializedName("textDescription")
    private String textDescription;

    //@SerializedName("presentWeather")
    //private List<Object> presentWeather;

    public void setDewpoint(Dewpoint dewpoint) {
        this.dewpoint = dewpoint;
    }

    public Dewpoint getDewpoint() {
        return dewpoint;
    }

    public void setWindGust(WindGust windGust) {
        this.windGust = windGust;
    }

    public WindGust getWindGust() {
        return windGust;
    }

    /*
    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
    */

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public void setHeatIndex(HeatIndex heatIndex) {
        this.heatIndex = heatIndex;
    }

    public HeatIndex getHeatIndex() {
        return heatIndex;
    }

    /*
    public void setMinTemperatureLast24Hours(MinTemperatureLast24Hours minTemperatureLast24Hours) {
        this.minTemperatureLast24Hours = minTemperatureLast24Hours;
    }

    public MinTemperatureLast24Hours getMinTemperatureLast24Hours() {
        return minTemperatureLast24Hours;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public String getStation() {
        return station;
    }
    */

    public void setTemperature(Temperature temperature) {
        this.temperature = temperature;
    }

    public Temperature getTemperature() {
        return temperature;
    }

    public void setPrecipitationLastHour(PrecipitationLastHour precipitationLastHour) {
        this.precipitationLastHour = precipitationLastHour;
    }

    public PrecipitationLastHour getPrecipitationLastHour() {
        return precipitationLastHour;
    }

    /*
    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    */

    public void setWindDirection(WindDirection windDirection) {
        this.windDirection = windDirection;
    }

    public WindDirection getWindDirection() {
        return windDirection;
    }

    public void setWindSpeed(WindSpeed windSpeed) {
        this.windSpeed = windSpeed;
    }

    public WindSpeed getWindSpeed() {
        return windSpeed;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setWindChill(WindChill windChill) {
        this.windChill = windChill;
    }

    public WindChill getWindChill() {
        return windChill;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    /*
    public void setMaxTemperatureLast24Hours(MaxTemperatureLast24Hours maxTemperatureLast24Hours) {
        this.maxTemperatureLast24Hours = maxTemperatureLast24Hours;
    }

    public MaxTemperatureLast24Hours getMaxTemperatureLast24Hours() {
        return maxTemperatureLast24Hours;
    }
    */

    public void setPrecipitationLast6Hours(PrecipitationLast6Hours precipitationLast6Hours) {
        this.precipitationLast6Hours = precipitationLast6Hours;
    }

    public PrecipitationLast6Hours getPrecipitationLast6Hours() {
        return precipitationLast6Hours;
    }

    public void setPrecipitationLast3Hours(PrecipitationLast3Hours precipitationLast3Hours) {
        this.precipitationLast3Hours = precipitationLast3Hours;
    }

    public PrecipitationLast3Hours getPrecipitationLast3Hours() {
        return precipitationLast3Hours;
    }

    public void setBarometricPressure(BarometricPressure barometricPressure) {
        this.barometricPressure = barometricPressure;
    }

    public BarometricPressure getBarometricPressure() {
        return barometricPressure;
    }

    public void setRelativeHumidity(RelativeHumidity relativeHumidity) {
        this.relativeHumidity = relativeHumidity;
    }

    public RelativeHumidity getRelativeHumidity() {
        return relativeHumidity;
    }

    public void setTextDescription(String textDescription) {
        this.textDescription = textDescription;
    }

    public String getTextDescription() {
        return textDescription;
    }

    /*
    public void setPresentWeather(List<Object> presentWeather) {
        this.presentWeather = presentWeather;
    }

    public List<Object> getPresentWeather() {
        return presentWeather;
    }
    */
}