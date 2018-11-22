package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class Forecastday1 {
    @SerializedName("date")
    private Date date;

    @SerializedName("period")
    private int period;

    @SerializedName("high")
    private High high;

    @SerializedName("low")
    private Low low;

    @SerializedName("conditions")
    private String conditions;

    @SerializedName("icon")
    private String icon;

    @SerializedName("icon_url")
    private String icon_url;

    @SerializedName("skyicon")
    private String skyicon;

    @SerializedName("pop")
    private int pop;

    @SerializedName("qpf_allday")
    private QpfAllday qpf_allday;

    @SerializedName("qpf_day")
    private QpfDay qpf_day;

    @SerializedName("qpf_night")
    private QpfNight qpf_night;

    @SerializedName("snow_allday")
    private SnowAllday snow_allday;

    @SerializedName("snow_day")
    private SnowDay snow_day;

    @SerializedName("snow_night")
    private SnowNight snow_night;

    @SerializedName("maxwind")
    private Maxwind maxwind;

    @SerializedName("avewind")
    private Avewind avewind;

    @SerializedName("avehumidity")
    private int avehumidity;

    @SerializedName("maxhumidity")
    private int maxhumidity;

    @SerializedName("minhumidity")
    private int minhumidity;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public High getHigh() {
        return high;
    }

    public void setHigh(High high) {
        this.high = high;
    }

    public Low getLow() {
        return low;
    }

    public void setLow(Low low) {
        this.low = low;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon_url() {
        return icon_url;
    }

    public void setIcon_url(String icon_url) {
        this.icon_url = icon_url;
    }

    public String getSkyicon() {
        return skyicon;
    }

    public void setSkyicon(String skyicon) {
        this.skyicon = skyicon;
    }

    public int getPop() {
        return pop;
    }

    public void setPop(int pop) {
        this.pop = pop;
    }

    public QpfAllday getQpf_allday() {
        return qpf_allday;
    }

    public void setQpf_allday(QpfAllday qpf_allday) {
        this.qpf_allday = qpf_allday;
    }

    public QpfDay getQpf_day() {
        return qpf_day;
    }

    public void setQpf_day(QpfDay qpf_day) {
        this.qpf_day = qpf_day;
    }

    public QpfNight getQpf_night() {
        return qpf_night;
    }

    public void setQpf_night(QpfNight qpf_night) {
        this.qpf_night = qpf_night;
    }

    public SnowAllday getSnow_allday() {
        return snow_allday;
    }

    public void setSnow_allday(SnowAllday snow_allday) {
        this.snow_allday = snow_allday;
    }

    public SnowDay getSnow_day() {
        return snow_day;
    }

    public void setSnow_day(SnowDay snow_day) {
        this.snow_day = snow_day;
    }

    public SnowNight getSnow_night() {
        return snow_night;
    }

    public void setSnow_night(SnowNight snow_night) {
        this.snow_night = snow_night;
    }

    public Maxwind getMaxwind() {
        return maxwind;
    }

    public void setMaxwind(Maxwind maxwind) {
        this.maxwind = maxwind;
    }

    public Avewind getAvewind() {
        return avewind;
    }

    public void setAvewind(Avewind avewind) {
        this.avewind = avewind;
    }

    public int getAvehumidity() {
        return avehumidity;
    }

    public void setAvehumidity(int avehumidity) {
        this.avehumidity = avehumidity;
    }

    public int getMaxhumidity() {
        return maxhumidity;
    }

    public void setMaxhumidity(int maxhumidity) {
        this.maxhumidity = maxhumidity;
    }

    public int getMinhumidity() {
        return minhumidity;
    }

    public void setMinhumidity(int minhumidity) {
        this.minhumidity = minhumidity;
    }
}
